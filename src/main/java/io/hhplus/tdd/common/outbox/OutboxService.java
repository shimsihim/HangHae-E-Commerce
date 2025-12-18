package io.hhplus.tdd.common.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 도메인에 독립적인 공통 Outbox 발행 로직
 * MessagePublisher를 사용하여 메시지 발행
 * 발행 결과에 따라 Outbox 상태 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxEventRepository outboxRepository;
    private final MessagePublisher messagePublisher; 


    public void publishEvent(OutboxEventTable event) {
        log.info(" 이벤트 발행 - Outbox ID: {}, Type: {}, AggregateId: {}",
                event.getId(), event.getEventType(), event.getAggregateId());

        messagePublisher.publish(
                        event.getEventType(),
                        event.getAggregateId(), 
                        event.getPayload()
                )
                .whenComplete((result, ex) -> {
                    // 발행 결과에 따라 상태 업데이트 (별도 트랜잭션)
                    updateEventStatus(event.getId(), ex, result);
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateEventStatus(Long eventId, Throwable ex, MessagePublisher.PublishResult result) {
        try {
            OutboxEventTable event = outboxRepository.findById(eventId)
                    .orElseThrow(() -> new IllegalStateException("Outbox 이벤트를 찾을 수 없음: " + eventId));

            if (ex == null) {
                // 성공 → PUBLISHED
                event.markAsPublished();
                outboxRepository.save(event);

                log.info("이벤트 발행 성공 - Outbox ID: {}, Type: {}, Key: {}, Partition: {}, Offset: {}",
                        eventId, event.getEventType(), result.key(), result.partition(), result.offset());

            } else {
                // 실패 → PENDING 유지 또는 DEAD_LETTER
                event.incrementRetryCount();
                event.setErrorMessage(extractErrorMessage(ex));
                event.setLastRetryAt(LocalDateTime.now());

                if (event.getRetryCount() >= event.getMaxRetry()) {
                    event.markAsDeadLetter();
                    log.error("이벤트 발행 최종 실패 (DEAD_LETTER) - Outbox ID: {}, Retry: {}/{}",
                            eventId, event.getRetryCount(), event.getMaxRetry());
                } else {
                    // PENDING 상태 유지 (폴러가 재처리)
                    log.warn("이벤트 발행 실패 (재시도 예정) - Outbox ID: {}, Retry: {}/{}",
                            eventId, event.getRetryCount(), event.getMaxRetry());
                }

                outboxRepository.save(event);
            }

        } catch (Exception updateEx) {
            log.error("상태 업데이트 실패 - Outbox ID: {}", eventId, updateEx);
        }
    }

    private String extractErrorMessage(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }
}
