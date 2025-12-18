package io.hhplus.tdd.domain.order.application;

import io.hhplus.tdd.common.distributedLock.LockGroupType;
import io.hhplus.tdd.common.distributedLock.MultiDistributedLockExecutor;
import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.domain.model.OrderItem;
import io.hhplus.tdd.domain.order.domain.model.OrderStatus;
import io.hhplus.tdd.domain.order.domain.service.OrderService;
import io.hhplus.tdd.domain.order.exception.OrderException;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderItemRepository;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderRepository;
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
 * 결제 완료 처리 UseCase
 * - PG사 웹훅으로부터 호출
 * - PENDING 상태의 주문을 PAID로 변경
 * - 재고 차감, 포인트 차감, 쿠폰 사용 처리
 */
@Service
@RequiredArgsConstructor
public class PayCompleteOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final ProductOptionRepository productOptionRepository;
    private final MultiDistributedLockExecutor lockExecutor;
    private final TransactionTemplate transactionTemplate;
    private final OrderEventPublisher orderEventPublisher;

    public record Input(
            long orderId
    ){}

    public void execute(Input input){
        // 1. 주문 조회
        Order order = orderRepository.findById(input.orderId())
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND, input.orderId()));

        // 2. PENDING 상태 검증
        if(order.getStatus() != OrderStatus.PENDING){
            throw new OrderException(ErrorCode.ORDER_NOT_VALID, input.orderId());
        }

        // 3. 주문 항목 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(input.orderId());
        if(orderItems.isEmpty()){
            throw new OrderException(ErrorCode.ORDER_NOT_VALID, input.orderId());
        }

        // 4. 상품 옵션 ID 리스트 추출
        List<Long> optionIds = orderItems.stream()
                .map(OrderItem::getProductOptionId)
                .toList();

        // 5. 분산 락 키 생성 (상품 옵션들 + 사용자 포인트)
        List<String> lockKeys = buildLockKeys(order.getUserId(), optionIds);

        // 6. 다중 분산 락을 획득하고 결제 완료 처리 실행
        lockExecutor.executeWithLocks(lockKeys, () -> {
            // 트랜잭션 내에서 결제 완료 처리 및 이벤트 발행
            transactionTemplate.execute(status -> {
                executePaymentLogic(order, orderItems, optionIds);
                return null;
            });
        });
    }

    /**
     * 결제 완료 비즈니스 로직 (트랜잭션 내에서 실행됨)
     */
    private void executePaymentLogic(Order detachedOrder, List<OrderItem> orderItems, List<Long> optionIds) {
        // 1. 트랜잭션 안에서 Order 재조회 (fresh data + managed 상태)
        Order order = orderRepository.findById(detachedOrder.getId())
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND, detachedOrder.getId()));

        // 2. 상태 재검증 (다른 트랜잭션에서 변경되었을 수 있음)
        if(order.getStatus() != OrderStatus.PENDING){
            throw new OrderException(ErrorCode.ORDER_NOT_VALID, order.getId());
        }

        // 3. OrderItem을 OrderItemInfo 리스트로 변환
        List<OrderService.OrderItemInfo> orderItemInfos = orderItems.stream()
                .map(item -> new OrderService.OrderItemInfo(item.getProductOptionId(), item.getQuantity()))
                .collect(Collectors.toList());

        // 4. 상품 및 옵션 조회 (Pessimistic Lock - DB 레벨 락)
        List<ProductOption> productOptions = productOptionRepository.findAllByIdInForUpdate(optionIds);

        // 5. 결제 완료 처리 (재고 차감, 포인트 차감, 쿠폰 사용, 상태 변경)
        orderService.completeOrderWithPayment(order, productOptions, orderItemInfos);

        // 6. 이벤트 발행 (트랜잭션 커밋 전/후 리스너에서 Outbox 저장 및 Kafka 발행)
        orderEventPublisher.publishOrderCompletedEvent(order);
    }

    //분산 락 키 리스트 생성
    private List<String> buildLockKeys(Long userId, List<Long> optionIds) {
        List<String> lockKeys = new ArrayList<>();

        lockKeys.add(LockGroupType.USER_POINT.name() + ":" + userId);

        for (Long optionId : optionIds) {
            lockKeys.add(LockGroupType.PRODUCT_OPTION.name() + ":" + optionId);
        }

        return lockKeys;
    }
}