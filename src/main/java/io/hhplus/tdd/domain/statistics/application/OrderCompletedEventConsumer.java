package io.hhplus.tdd.domain.statistics.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.common.consumed.ConsumedEventLog;
import io.hhplus.tdd.common.consumed.ConsumedEventLogRepository;
import io.hhplus.tdd.domain.order.domain.event.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCompletedEventConsumer {
    private final ObjectMapper objectMapper;
    private final ConsumedEventLogRepository consumedEventLogRepository;

    private static final String EVENT_TYPE = "OrderCompleted";

    @KafkaListener(
        topics = "OrderCompleted",
        groupId = "statistics-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderCompleted(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("주문 완료 이벤트 수신 - Key: {}, Partition: {}, Offset: {}",
                key, partition, offset);

        OrderCompletedEvent event;
        try {
            event = objectMapper.readValue(payload, OrderCompletedEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse OrderCompletedEvent payload: {}", payload, e);
            return;
        }

        String eventId = String.valueOf(event.orderId());

        // 멱등성 검증
        if (consumedEventLogRepository.existsByEventIdAndEventType(eventId, EVENT_TYPE)) {
            log.warn("이미 처리된 OrderCompleted 이벤트입니다. EventId: {}", eventId);
            return;
        }

        try {
            // 이벤트 처리 (여기서는 단순히 로그 기록. 실제 비즈니스 로직은 UseCase 호출)
            log.info("OrderCompleted 이벤트 처리 시작 - OrderId: {}, UserId: {}",
                    event.orderId(), event.userId());

            // ConsumedEventLog 기록
            ConsumedEventLog consumedLog = ConsumedEventLog.create(
                    eventId,
                    EVENT_TYPE,
                    this.getClass().getSimpleName(),
                    payload
            );
            consumedEventLogRepository.save(consumedLog);
            log.info("OrderCompleted 이벤트 처리 완료 및 ConsumedEventLog 기록. EventId: {}", eventId);

        } catch (DataIntegrityViolationException e) {
            // Unique Constraint 위반 (동일 이벤트가 동시에 처리되려 한 경우)
            log.warn("OrderCompleted 이벤트 동시 처리 시도 감지, 스킵. EventId: {}", eventId);
        } catch (Exception e) {
            log.error("OrderCompleted 이벤트 처리 중 예외 발생. EventId: {}, Payload: {}", eventId, payload, e);
        }
    }
}

