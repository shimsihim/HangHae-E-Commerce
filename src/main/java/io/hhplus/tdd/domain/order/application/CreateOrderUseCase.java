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
import io.hhplus.tdd.domain.order.domain.service.OrderService;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderItemRepository;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderRepository;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.domain.service.PointService;
import io.hhplus.tdd.domain.point.infrastructure.repository.UserPointRepository;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductOptionRepository;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 주문 생성 UseCase
 * - 주문 생성 및 검증 (재고 검증만, 차감하지 않음)
 * - finalAmount가 0원이면 즉시 완료 처리 (재고/포인트/쿠폰 모두 처리)
 * - 그 외에는 PENDING 상태로 저장 (PG 결제 대기)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CouponRepository couponRepository;
    private final UserPointRepository userPointRepository;
    private final ProductRepository productRepository;

    private final CouponService couponService;
    private final OrderService orderService;
    private final PointService pointService;
    private final UserCouponRepository userCouponRepository;
    private final ProductOptionRepository productOptionRepository;

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
                Long productOptionId,
                int quantity
        ) {}
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
        // 1. 사용자 포인트 검증
        UserPoint userPoint = userPointRepository.findById(input.userId())
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, input.userId()));
        userPoint.validUsePoint(input.usePointAmount());


        long discountAmount = 0;
        long totalAmount = 0;


        // 2. 재고 검증
        List<OrderService.OrderItemInfo> orderItems = input.items().stream()
                .map(item -> new OrderService.OrderItemInfo(item.productOptionId(), item.quantity()))
                .toList();
        List<Long> optionIds = orderItems.stream()
                .map(OrderService.OrderItemInfo::productOptionId)
                .toList();
        List<ProductOption> productOptions = productOptionRepository.findAllWithProductByIdIn(optionIds);
        totalAmount = orderService.validOrder(productOptions , orderItems);


        // 3. 쿠폰 검증
        UserCoupon userCoupon = null;
        if(input.userCouponId() != null){
            userCoupon = userCouponRepository.findByIdWithCoupon(input.userCouponId()).orElseThrow(()->
                    new CouponException(ErrorCode.COUPON_NOT_FOUND, input.userCouponId())
            );
            // 3. 사용자 쿠폰 검증
            Coupon coupon = userCoupon.getCoupon();
            discountAmount = couponService.validateCouponUsage(coupon, userCoupon, totalAmount);
        }
        // 4. 주문생성
        Order newOrder = Order.createOrder(userPoint, userCoupon, totalAmount, discountAmount, input.usePointAmount());

        // 5. finalAmount가 0원이면 즉시 완료 처리
        long finalAmount = newOrder.getFinalAmount();
        if (finalAmount == 0) {
            // 재고 차감이 필요하므로 락을 걸고 다시 조회
            List<Product> productsWithLock = productRepository.findProductsWithOptionsForUpdate(optionIds);
            // 재고 차감, 포인트 차감, 쿠폰 사용 모두 처리
            orderService.completeImmediateOrder(newOrder, productsWithLock, orderItems);
        }

        // 6. 주문 저장
        Order savedOrder = orderRepository.save(newOrder);

        // 7. 주문 항목 생성 및 저장
        List<OrderItem> orderItemEntities = createOrderItems(savedOrder, products, orderItems);
        orderItemRepository.saveAll(orderItemEntities);

        return Output.from(savedOrder);
    }

    /**
     * OrderItem 목록을 생성합니다.
     */
    private List<OrderItem> createOrderItems(Order savedOrder, List<Product> products,
                                             List<OrderService.OrderItemInfo> orderItems) {
        // ProductOption Map 생성 (빠른 조회)
        Map<Long, ProductOption> optionMap = products.stream()
                .flatMap(product -> product.getOptions().stream())
                .collect(Collectors.toMap(ProductOption::getId, Function.identity()));

        return orderItems.stream()
                .map(item -> {
                    ProductOption productOption = optionMap.get(item.productOptionId());
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