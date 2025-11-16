package io.hhplus.tdd.domain.order.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.exception.UserNotFoundException;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.CouponRepository;
import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.domain.model.OrderItem;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderItemRepository;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderRepository;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.domain.service.PointService;
import io.hhplus.tdd.domain.point.infrastructure.repository.UserPointRepository;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CouponRepository couponRepository;
    private final UserPointRepository userPointRepository;

    private final CouponService couponService;
    private final PointService pointService;
    private final ProductRepository productRepository;

    public record Input(
            Long userId,
            List<ProductInfo> items,
            Long userCouponId,
            long usePointAmount
    ) {
        public static Input of(Long userId, List<ProductInfo> items,
                               Long userCouponId,
                               Long usePointAmount){
            return new Input(userId, items, userCouponId, usePointAmount);
        }

        public record ProductInfo(
                Long productId,
                Long productOptionId,
                int quantity
        ){
        }
    }

    public record Output(
            Long orderId,
            Long userId,
            Long totalAmount,
            Long discountAmount,
            Long usePointAmount,
            Long finalAmount
    ) {
        public static Output from(Order order) {
            return new Output(
                    order.getId(),
                    order.getUserId(),
                    order.getTotalAmount(),
                    order.getDiscountAmount(),
                    order.getUsePointAmount(),
                    order.getFinalAmount()
            );
        }
    }


    @Transactional
    public Output execute(Input input) {
        UserPoint userPoint = userPointRepository.findById(input.userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, input.userId()));

        // 제품 옵션 id에 대한 주문 옵션 맵 생성
        Map<Long, Input.ProductInfo> requestedOptionMap = input.items().stream()
                .collect(Collectors.toMap(
                        Input.ProductInfo::productOptionId,
                        item -> item
                ));

        List<Long> optionIds = new ArrayList<>(requestedOptionMap.keySet());
        List<Product> products = productRepository.findProductsWithOptions(optionIds);

        validAllProductExist(products, requestedOptionMap);

        // 최종 금액 계산 및 재고 선점
        long totalAmount = calculateAndDeductStock(products, requestedOptionMap);

        // 쿠폰 사용 검증 및 할인액 계산
        UserCoupon userCoupon = null;
        Coupon coupon = null;
        long discountAmount = 0;

        if (input.userCouponId() != null) {
            coupon = couponRepository.findCouponWithUserCoupon(input.userCouponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND, input.userCouponId()));

            userCoupon = coupon.getUserCoupons().stream()
                    .filter(uc -> uc.getId().equals(input.userCouponId()))
                    .findFirst()
                    .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND, input.userCouponId()));

            discountAmount = couponService.validUseUserCoupon(coupon, userCoupon, totalAmount);
        }

        // 사용자 포인트 검증 (차감은 결제 단계에서)
        pointService.validUsePoint(userPoint, input.usePointAmount());

        // pg사 연동 부분은 생략.

        // 최종 금액이 0원일 경우 paid 상태로 order생성
        Order newOrder = Order.createOrder(userPoint, userCoupon, totalAmount, discountAmount, input.usePointAmount());
        long finalAmount = newOrder.getFinalAmount();

        if(finalAmount == 0){ // 포인트 결제 만으로 끝날경우
            pointService.usePoint(userPoint , input.usePointAmount() , "Buy Product");
            if(coupon != null && userCoupon != null){
                couponService.useUserCoupon(coupon , userCoupon , totalAmount);
            }
            newOrder.completeOrder();
        }

        final Order savedOrder = orderRepository.save(newOrder);

        List<OrderItem> orderItems = createOrderItems(savedOrder, products, requestedOptionMap);
        orderItemRepository.saveAll(orderItems);

        return Output.from(savedOrder);
    }

    /**
     * 상품 옵션의 재고를 차감하고 총 금액을 계산합니다. (단일 트랜잭션 내에서 처리)
     * @param products 조회된 Product 엔티티 목록 (구매 제품 옵션 정보들만 포함)
     * @param requestedOptionMap 요청된 옵션 ID와 수량 맵
     * @return 포인트/쿠폰 미적용된 총 주문 금액
     */
    private long calculateAndDeductStock(List<Product> products, Map<Long, Input.ProductInfo> requestedOptionMap) {
        long totalAmount = 0;

        for (Product product : products) {
            for (ProductOption option : product.getOptions()) {
                Input.ProductInfo orderReq = requestedOptionMap.get(option.getId());
                option.deduct(orderReq.quantity());
                totalAmount += option.getPrice() * orderReq.quantity();
            }
        }
        return totalAmount;
    }

    /**
     * OrderItem 목록을 생성합니다. (Stream 사용)
     */
    private List<OrderItem> createOrderItems(Order savedOrder, List<Product> products, Map<Long, Input.ProductInfo> requestedOptionMap) {
        return products.stream()
                .flatMap(product -> product.getOptions().stream())
                .map(productOption -> {
                    Input.ProductInfo item = requestedOptionMap.get(productOption.getId());
                    return OrderItem.create(
                            savedOrder,
                            productOption.getProduct(),
                            productOption,
                            item.quantity(),
                            productOption.getPrice()
                    );
                })
                .collect(Collectors.toList());
    }

     //요청된 모든 상품 및 옵션 ID가 DB 조회 결과에 포함되었는지 검증
    private void validAllProductExist(List<Product> products, Map<Long, Input.ProductInfo> requestedOptionMap) {

        Set<Long> requestedOptionIds = requestedOptionMap.keySet();

        Set<Long> foundOptionIds = products.stream()
                .flatMap(product -> product.getOptions().stream())
                .map(ProductOption::getId)
                .collect(Collectors.toSet());

        //  누락된 옵션 ID 찾기
        Set<Long> missingOptionIds = requestedOptionIds.stream()
                .filter(id -> !foundOptionIds.contains(id))
                .collect(Collectors.toSet());

        if (!missingOptionIds.isEmpty()) {
            throw new IllegalArgumentException("요청된 상품 옵션 ID 중 일부(" + missingOptionIds + ")가 존재하지 않거나 유효하지 않아 주문을 생성할 수 없습니다.");
        }
    }
}
