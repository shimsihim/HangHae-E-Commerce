package io.hhplus.tdd.domain.order.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.common.outbox.OutboxEventRepository;
import io.hhplus.tdd.common.outbox.OutboxEventTable;
import io.hhplus.tdd.common.outbox.OutboxService;
import io.hhplus.tdd.common.outbox.OutboxStatus;
import io.hhplus.tdd.domain.order.domain.event.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final OutboxEventRepository outboxRepository;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void saveToOutbox(OrderCompletedEvent event) {
        try {
            String payloadJson = objectMapper.writeValueAsString(event);

            OutboxEventTable outboxEvent = OutboxEventTable.builder()
                    .aggregateType("ORDER")
                    .aggregateId(event.orderId().toString())
                    .eventType("OrderCompleted")
                    .payload(payloadJson)
                    .build();

            outboxRepository.save(outboxEvent);

            log.info(" [BEFORE_COMMIT] Outbox 저장 완료 - 주문 ID: {}", event.orderId());

        } catch (JsonProcessingException e) {
            log.error("Outbox JSON 변환 실패 - 주문 ID: {}", event.orderId(), e);
            throw new RuntimeException("Outbox 저장 실패", e);
        }
    }

    /**
     * AFTER_COMMIT: 저장된 Outbox 레코드를 Kafka로 발행
     * OutboxService에 위임하여 재사용성과 책임 분리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishToKafka(OrderCompletedEvent event) {
        // orderId로 방금 저장한 PENDING 상태의 Outbox 레코드 조회
        OutboxEventTable outboxEvent = outboxRepository
                .findTopByAggregateTypeAndAggregateIdAndEventTypeAndStatusOrderByCreatedAtDesc(
                        "ORDER",
                        event.orderId().toString(),
                        "OrderCompleted",
                        OutboxStatus.PENDING
                );

        if (outboxEvent == null) {
            log.error(" [AFTER_COMMIT] Outbox 레코드를 찾을 수 없음 - 주문 ID: {}", event.orderId());
            return;
        }

        log.info(" [AFTER_COMMIT] Outbox 발행 위임 - Outbox ID: {}, 주문 ID: {}",
                outboxEvent.getId(), event.orderId());

        // OutboxService에 발행 위임
        outboxService.publishEvent(outboxEvent);
    }
}
