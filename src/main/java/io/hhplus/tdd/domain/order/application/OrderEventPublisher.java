package io.hhplus.tdd.domain.order.application;

import io.hhplus.tdd.domain.order.domain.event.OrderCompletedEvent;
import io.hhplus.tdd.domain.order.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 주문 이벤트 발행자
 * OrderCompletedEvent를 발행하면:
 * 1. BEFORE_COMMIT: OrderEventListener가 Outbox 테이블에 저장
 * 2. AFTER_COMMIT: OrderEventListener가 Kafka로 발행
 */
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 주문 완료 이벤트 발행 (Spring Event)
     * BEFORE_COMMIT: Outbox 저장
     * AFTER_COMMIT: Kafka 발행
     */
    public void publishOrderCompletedEvent(Order order) {
        OrderCompletedEvent event = OrderCompletedEvent.from(
            order.getId(),
            order.getUserId(),
            order.getTotalAmount(),
            order.getDiscountAmount(),
            order.getUsePointAmount(),
            order.getFinalAmount()
        );

        // Spring Event 발행
        eventPublisher.publishEvent(event);
    }
}
