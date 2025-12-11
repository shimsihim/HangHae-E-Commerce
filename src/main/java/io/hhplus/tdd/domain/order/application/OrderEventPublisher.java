package io.hhplus.tdd.domain.order.application;

import io.hhplus.tdd.domain.order.domain.event.OrderCompletedEvent;
import io.hhplus.tdd.domain.order.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishOrderCompletedEvent(Order order) {
        OrderCompletedEvent event = OrderCompletedEvent.from(
            order.getId(),
            order.getUserId(),
            order.getTotalAmount(),
            order.getDiscountAmount(),
            order.getUsePointAmount(),
            order.getFinalAmount()
        );
        eventPublisher.publishEvent(event);
    }
}
