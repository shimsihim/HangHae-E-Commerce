package io.hhplus.tdd.domain.order.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.exception.UserNotFoundException;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.CouponRepository;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.UserCouponRepository;
import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.domain.model.OrderItem;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderItemRepository;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderRepository;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.order.domain.service.OrderService;
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

/**
 * 주문 생성 UseCase
 * - 주문 생성 및 재고 선점
 * - finalAmount가 0원이면 즉시 완료 처리
 * - 그 외에는 PENDING 상태로 저장 (PG 결제 대기)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserPointRepository userPointRepository;
    private final ProductRepository productRepository;

    private final CouponService couponService;
    private final OrderService orderService;

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
                Long productOptionId,  // productId 제거 (불필요)
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
        // 1. 사용자 조회
        UserPoint userPoint = userPointRepository.findById(input.userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, input.userId()));

        // 2. 요청된 옵션 맵 생성 (OrderService.RequestedOption 타입으로 변환)
        Map<Long, OrderService.RequestedOption> requestedOptionMap = input.items().stream()
                .collect(Collectors.toMap(
                        Input.ProductInfo::productOptionId,
                        item -> new OrderService.RequestedOption(item.productOptionId(), item.quantity())
                ));

        // 3. 상품 및 옵션 조회
        List<Long> optionIds = new ArrayList<>(requestedOptionMap.keySet());
        List<Product> products = productRepository.findProductsWithOptions(optionIds);

        // 4. 상품 존재 여부 검증 (Domain Service)
        orderService.validateProductsExist(products, requestedOptionMap);

        // 5. 재고 선점 및 총 금액 계산 (Domain Service)
        long totalAmount = orderService.reserveStock(products, requestedOptionMap);

        // 6. 쿠폰 검증 및 할인 금액 계산
        UserCoupon userCoupon = null;
        Coupon coupon = null;
        long discountAmount = 0;

        if (input.userCouponId() != null) {
            userCoupon = userCouponRepository.findByIdWithCoupon(input.userCouponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND, input.userCouponId()));
            coupon = userCoupon.getCoupon();

            // 쿠폰 사용 가능 여부 검증
            discountAmount = couponService.validateCouponUsage(coupon, userCoupon, totalAmount);
        }

        // 7. 포인트 검증 (사전 검증, UserPoint 엔티티 직접 호출)
        userPoint.validUsePoint(input.usePointAmount());

        // 8. 주문 엔티티 생성 (PENDING 상태)
        Order newOrder = Order.createOrder(userPoint, userCoupon, totalAmount, discountAmount, input.usePointAmount());

        // 9. finalAmount가 0원이면 즉시 완료 처리 (Domain Service)
        long finalAmount = newOrder.getFinalAmount();
        if (finalAmount == 0) {
            orderService.completeImmediateOrder(newOrder);
        }
        // 그 외에는 PENDING 상태 유지 (PG 결제 대기)

        // 10. 주문 저장
        Order savedOrder = orderRepository.save(newOrder);

        // 11. 주문 항목 생성 및 저장
        List<OrderItem> orderItems = createOrderItems(savedOrder, products, requestedOptionMap);
        orderItemRepository.saveAll(orderItems);

        return Output.from(savedOrder);
    }

    /**
     * OrderItem 목록을 생성합니다.
     */
    private List<OrderItem> createOrderItems(Order savedOrder, List<Product> products,
                                              Map<Long, OrderService.RequestedOption> requestedOptionMap) {
        return products.stream()
                .flatMap(product -> product.getOptions().stream())
                .map(productOption -> {
                    OrderService.RequestedOption item = requestedOptionMap.get(productOption.getId());
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
}
