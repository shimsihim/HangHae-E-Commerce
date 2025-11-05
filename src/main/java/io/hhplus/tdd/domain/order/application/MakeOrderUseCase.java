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
        // 주문 항목별 상품 옵션 조회 및 재고 검증
        Map<Long, ProductOption> productOptionMap = new HashMap<>();
        Map<Long, Long> originalStockMap = new HashMap<>(); // 롤백용 원본 재고
        long totalAmount = 0;

        // 1. 재고 검증 및 총 주문 금액 계산
        for (Input.ItemInfo item : input.items()) {
            ProductOption productOption = productOptionRepository.findById(item.productOptionId())
                    .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND, item.productId(), item.productOptionId()));

            // 재고 검증
            if (productOption.getQuantity() < item.quantity()) {
                throw new ProductException(ErrorCode.PRODUCT_NOT_ENOUGH, item.productId(), item.productOptionId());
            }

            productOptionMap.put(item.productOptionId(), productOption);
            originalStockMap.put(item.productOptionId(), productOption.getQuantity());
            totalAmount += productOption.getPrice() * item.quantity();
        }

        // 2. 쿠폰 검증 및 할인 금액 계산
        long discountAmount = 0;
        Coupon coupon = null;
        UserCoupon userCoupon = null;
        Status originalCouponStatus = null;

        if (input.userCouponId() != null) {
            userCoupon = userCouponRepository.findByUserIdAndUserCouponIdAndStatus(
                            input.userId(), input.userCouponId(), Status.ISSUED.toString())
                    .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND, input.userCouponId()));

            long couponId = userCoupon.getCouponId();
            coupon = couponRepository.findById(userCoupon.getCouponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND, couponId));

            originalCouponStatus = userCoupon.getStatus();
            discountAmount = couponService.applyDiscount(coupon, userCoupon, totalAmount);
        }

        // 3. 포인트 검증
        UserPoint userPoint = null;
        long originalBalance = 0;
        long usePointAmount = input.usePointAmount() != null ? input.usePointAmount() : 0;

        if (usePointAmount > 0) {
            userPoint = userPointRepository.findByUserId(input.userId())
                    .orElseThrow(() -> new ProductException(ErrorCode.USER_NOT_FOUND, input.userId(), 0L));
            originalBalance = userPoint.getBalance();

            if (userPoint.getBalance() < usePointAmount) {
                throw new PointRangeException(ErrorCode.USER_POINT_NOT_ENOUGH, input.userId(), userPoint.getBalance() , usePointAmount);
            }
        }

        // 4. 주문 생성 (PENDING 상태)
        Order order = Order.createOrder(
                input.userId(),
                input.userCouponId(),
                totalAmount,
                discountAmount,
                usePointAmount
        );
        Order savedOrder = orderRepository.save(order);

        try {
            // 5. 재고 차감
            for (Input.ItemInfo item : input.items()) {
                ProductOption productOption = productOptionMap.get(item.productOptionId());
                productOption.deduct(item.quantity());
                productOptionRepository.save(productOption);
            }

            // 6. 쿠폰 사용 처리 (이미 applyDiscount에서 처리됨)
            if (userCoupon != null) {
                userCouponRepository.save(userCoupon);
            }

            // 7. 포인트 차감
            if (usePointAmount > 0) {
                PointHistory pointHistory = pointService.usePoint(userPoint, usePointAmount, "주문 결제");
                userPointRepository.save(userPoint);
                pointHistoryRepository.save(pointHistory);
            }

            // 8. 주문 완료 처리 (PAID 상태로 변경)
            savedOrder.completeOrder();
            orderRepository.save(savedOrder);

            // 9. 주문 항목 저장
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
            log.error("주문 생성 실패. 롤백 수행. userId: {}, orderId: {}", input.userId(), savedOrder.getId(), e);

            // 보상 트랜잭션: 재고 복구
            for (Input.ItemInfo item : input.items()) {
                ProductOption productOption = productOptionMap.get(item.productOptionId());
                Long originalStock = originalStockMap.get(item.productOptionId());
                if (originalStock != null) {
                    // 재고를 원래대로 복구
                    ProductOption freshOption = productOptionRepository.findById(item.productOptionId()).orElse(null);
                    if (freshOption != null) {
                        // 차감된 수량만큼 다시 증가
                        long difference = originalStock - freshOption.getQuantity();
                        if (difference > 0) {
                            freshOption = ProductOption.builder()
                                    .id(freshOption.getId())
                                    .productId(freshOption.getProductId())
                                    .optionName(freshOption.getOptionName())
                                    .price(freshOption.getPrice())
                                    .quantity(originalStock)
                                    .build();
                            productOptionRepository.save(freshOption);
                        }
                    }
                }
            }

            // 보상 트랜잭션: 쿠폰 복구
            if (userCoupon != null && originalCouponStatus != null) {
                couponService.cancelUsage(userCoupon);
                userCouponRepository.save(userCoupon);
            }

            // 보상 트랜잭션: 포인트 복구
            if (userPoint != null && usePointAmount > 0) {
                userPoint.updateBalance(originalBalance);
                userPointRepository.save(userPoint);
            }

            throw e;
        }
    }
}
