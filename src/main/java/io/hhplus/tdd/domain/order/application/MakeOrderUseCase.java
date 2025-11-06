package io.hhplus.tdd.domain.order.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.lock.LockAnn;
import io.hhplus.tdd.common.lock.LockKey;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.Status;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.domain.repository.CouponRepository;
import io.hhplus.tdd.domain.coupon.domain.repository.UserCouponRepository;
import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.domain.model.OrderItem;
import io.hhplus.tdd.domain.order.domain.repository.OrderItemRepository;
import io.hhplus.tdd.domain.order.domain.repository.OrderRepository;
import io.hhplus.tdd.domain.order.domain.service.OrderService;
import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.domain.repository.PointHistoryRepository;
import io.hhplus.tdd.domain.point.domain.repository.UserPointRepository;
import io.hhplus.tdd.domain.point.domain.service.PointService;
import io.hhplus.tdd.domain.point.exception.PointRangeException;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.domain.repository.ProductOptionRepository;
import io.hhplus.tdd.domain.product.exception.ProductException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MakeOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductOptionRepository productOptionRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    private final CouponService couponService;
    private final PointService pointService;
    private final OrderService orderService;

    public record Input(
            Long userId,
            List<ItemInfo> items,
            Long userCouponId,
            Long usePointAmount
    ) {
        public static Input of(Long userId, List<ItemInfo> items,
                               Long userCouponId,
                               Long usePointAmount){
            return new Input(userId, items, userCouponId, usePointAmount);
        }

        public record ItemInfo(
                Long productId,
                Long productOptionId,
                Long quantity
        ){
            public static ItemInfo of(long productId, long productOptionId, long quantity){
                return new ItemInfo(productId, productOptionId, quantity);
            }
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

    @LockAnn(lockKey = LockKey.Order)
    public Output execute(Input input) {

        Map<Long, ProductOption> productOptionMap = new HashMap<>();
        Map<Long, Long> originalStockMap = new HashMap<>(); // 롤백용 원본 재고

        // 상품 옵션 조회 및 원본 재고 보관
        for (Input.ItemInfo item : input.items()) {
            ProductOption productOption = productOptionRepository.findById(item.productOptionId())
                    .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND, item.productId(), item.productOptionId()));

            productOptionMap.put(item.productOptionId(), productOption);
            originalStockMap.put(item.productOptionId(), productOption.getQuantity());
        }

        //  총 주문 금액 계산
        List<OrderService.OrderItemInfo> orderItems = input.items().stream()
                .map(item -> OrderService.OrderItemInfo.of(item.productId(), item.productOptionId() ,productOptionMap.get(item.productOptionId()).getPrice() , item.quantity()))
                .toList();

        long totalAmount = orderService.calculateTotalAmount(orderItems);

        // 쿠폰 검증 및 할인 금액 계산
        long discountAmount = 0;
        Coupon coupon = null;
        UserCoupon userCoupon = null;

        if (input.userCouponId() != null) {
            userCoupon = userCouponRepository.findByUserIdAndUserCouponIdAndStatus(
                            input.userId(), input.userCouponId(), Status.ISSUED.toString())
                    .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND, input.userCouponId()));

            long couponId = userCoupon.getCouponId();
            coupon = couponRepository.findById(userCoupon.getCouponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND, couponId));

            discountAmount = couponService.applyDiscount(coupon, userCoupon, totalAmount);
        }

        // 포인트 검증
        long usePointAmount = input.usePointAmount() != null ? input.usePointAmount() : 0;

        UserPoint userPoint = userPointRepository.findByUserId(input.userId())
                .orElseThrow(() -> new ProductException(ErrorCode.USER_NOT_FOUND, input.userId(), 0L));
        long originalBalance = userPoint.getBalance();

        if (userPoint.getBalance() < usePointAmount) {
            throw new PointRangeException(ErrorCode.USER_POINT_NOT_ENOUGH, input.userId(), userPoint.getBalance() , usePointAmount);
        }

        // 주문 생성 (PENDING 상태)
        Order order = Order.createOrder(
                input.userId(),
                input.userCouponId(),
                totalAmount,
                discountAmount,
                usePointAmount
        );
        Order savedOrder = orderRepository.save(order);

        try {
            // 재고 차감 (도메인 서비스 사용)
            orderService.validateAndDeductStock(orderItems, productOptionMap);
            for (Input.ItemInfo item : input.items()) {
                ProductOption productOption = productOptionMap.get(item.productOptionId());
                productOptionRepository.save(productOption);
            }

            // 쿠폰 사용 처리 (이미 applyDiscount에서 처리됨)
            if (userCoupon != null) {
                userCouponRepository.save(userCoupon);
            }

            // 포인트 차감
            if (usePointAmount > 0) {
                PointHistory pointHistory = pointService.usePoint(userPoint, usePointAmount, "주문 결제");
                userPointRepository.save(userPoint);
                pointHistoryRepository.save(pointHistory);
            }

            // 주문 완료 처리 (PAID 상태로 변경)
            savedOrder.completeOrder();
            orderRepository.save(savedOrder);

            // 주문 항목 저장
            for (Input.ItemInfo item : input.items()) {
                ProductOption productOption = productOptionMap.get(item.productOptionId());
                OrderItem orderItem = Order.createOrderItem(
                        savedOrder.getId(),
                        item.productOptionId(),
                        item.quantity().intValue(),
                        productOption.getPrice()
                );
                orderItemRepository.save(orderItem);
            }

            return Output.from(savedOrder);

        } catch (Exception e) {
            log.error("주문 실패. 롤백 수행. userId: {}, orderId: {}", input.userId(), savedOrder.getId(), e);

            // 재고 복구
            orderService.restoreStock(orderItems, productOptionMap, originalStockMap);
            for (Input.ItemInfo item : input.items()) {
                ProductOption productOption = productOptionMap.get(item.productOptionId());
                productOptionRepository.save(productOption);
            }

            // 쿠폰 복구
            if (userCoupon != null) {
                couponService.cancelUsage(userCoupon);
                userCouponRepository.save(userCoupon);
            }

            // 포인트 복구
            if (usePointAmount > 0) {
                userPoint.updateBalance(originalBalance);
                userPointRepository.save(userPoint);
            }

            throw e;
        }
    }
}
