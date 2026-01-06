package io.hhplus.tdd.domain.order.application;

import io.hhplus.tdd.common.distributedLock.LockGroupType;
import io.hhplus.tdd.common.distributedLock.MultiDistributedLockExecutor;
import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.domain.model.OrderItem;
import io.hhplus.tdd.domain.order.domain.model.OrderStatus;
import io.hhplus.tdd.domain.order.domain.service.OrderService;
import io.hhplus.tdd.domain.order.exception.OrderException;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderItemRepository;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderRepository;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.domain.service.PointService;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductOptionRepository;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 주문 취소 UseCase
 * - PENDING 상태의 주문만 취소 가능
 * - 재고 복구 처리
 */
@Service
@RequiredArgsConstructor
public class CancelOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final ProductOptionRepository productOptionRepository;
    private final PointService pointService;
    private final CouponService couponService;
    private final MultiDistributedLockExecutor lockExecutor;
    private final TransactionTemplate transactionTemplate;

    public record Input(
            long orderId
    ){}

    public void execute(Input input) {
        // 주문 조회
        Order order = orderRepository.findById(input.orderId())
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND, input.orderId()));

        // 주문 항목 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

        OrderStatus orderStatus = order.getStatus();
        // PAID 상태인 경우 재고와쿠폰 복구
        if (orderStatus == OrderStatus.PAID) {
            // 상품 옵션 ID 리스트 추출
            List<Long> optionIds = orderItems.stream()
                    .map(OrderItem::getProductOptionId)
                    .toList();

            // 분산 락 키 생성
            List<String> lockKeys = buildLockKeys(order.getUserId(), optionIds);

            // 다중 분산 락을 획득하고 주문 취소 처리 실행
            lockExecutor.executeWithLocks(lockKeys, () -> {
                // 트랜잭션 내에서 주문 취소 처리 실행
                transactionTemplate.executeWithoutResult(status -> {
                    executeCancelLogic(order, orderItems, optionIds);
                });
            });
        } else {
            // PENDING 상태시 주문의 상태만 취소로 변경
            transactionTemplate.executeWithoutResult(status -> {
                executePendingCancelLogic(order.getId());
            });
        }
    }

    /**
     * PAID 상태 주문 취소 비즈니스 로직 (트랜잭션 내에서 실행됨)
     */
    private void executeCancelLogic(Order detachedOrder, List<OrderItem> orderItems, List<Long> optionIds) {
        // 1. 트랜잭션 안에서 Order 재조회 (fresh data + managed 상태)
        Order order = orderRepository.findById(detachedOrder.getId())
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND, detachedOrder.getId()));

        // 2. 주문 취소 처리 (상태를 CANCELLED로 변경, 취소 가능 여부 검증 포함)
        orderService.cancelOrder(order);

        // 3. 리소스 복구 (재고, 포인트, 쿠폰)
        // OrderItem을 OrderItemInfo 리스트로 변환
        List<OrderService.OrderItemInfo> orderItemInfos = orderItems.stream()
                .map(item -> new OrderService.OrderItemInfo(item.getProductOptionId(), item.getQuantity()))
                .collect(Collectors.toList());

        // 상품 옵션 조회 (Pessimistic Lock - DB 레벨 락)
        List<ProductOption> productOptions = productOptionRepository.findAllByIdInForUpdate(optionIds);

        // 재고 복구
        orderService.restoreStock(productOptions, orderItemInfos);

        // 포인트 복구
        UserPoint userPoint = order.getUserPoint();
        pointService.chargePoint(userPoint, order.getUsePointAmount(), "주문 취소로 인한 포인트 환불");

        // 쿠폰 복구
        UserCoupon userCoupon = order.getUserCoupon();
        if (userCoupon != null) {
            userCoupon.restoreCoupon();
        }
    }

    /**
     * PENDING 상태 주문 취소 비즈니스 로직 (트랜잭션 내에서 실행됨)
     */
    private void executePendingCancelLogic(Long orderId) {
        // 트랜잭션 안에서 Order 재조회 (fresh data + managed 상태)
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND, orderId));

        // 주문 취소 처리 (상태만 CANCELLED로 변경)
        orderService.cancelOrder(order);
    }

    // 분산 락 키 리스트 생성
    private List<String> buildLockKeys(Long userId, List<Long> optionIds) {
        List<String> lockKeys = new ArrayList<>();

        lockKeys.add(LockGroupType.USER_POINT.name() + ":" + userId);

        for (Long optionId : optionIds) {
            lockKeys.add(LockGroupType.PRODUCT_OPTION.name() + ":" + optionId);
        }

        return lockKeys;
    }
}