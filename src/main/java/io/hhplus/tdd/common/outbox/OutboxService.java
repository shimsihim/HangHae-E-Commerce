package io.hhplus.tdd.common.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Outbox 이벤트 발행 서비스
 * 도메인에 독립적인 공통 Outbox 발행 로직
 *
 * 책임:
 * - MessagePublisher를 사용하여 메시지 발행
 * - 발행 결과에 따라 Outbox 상태 관리
 * - 메시지 브로커(Kafka 등)와는 독립적
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxEventRepository outboxRepository;
    private final MessagePublisher messagePublisher;  // ← 인터페이스 의존 (Kafka 분리)

    /**
     * Outbox 이벤트를 메시지 브로커로 발행
     * MessagePublisher 인터페이스를 통해 브로커 독립적으로 동작
     *
     * 메시지 키 사용:
     * - aggregateId를 메시지 키로 사용
     * - 동일한 aggregate(주문 등)의 이벤트들이 동일한 파티션으로 전송됨
     * - 파티션 내에서 순서가 보장됨
     *
     * @param event 발행할 Outbox 이벤트
     */
    public void publishEvent(OutboxEventTable event) {
        log.info("🚀 이벤트 발행 시작 - Outbox ID: {}, Type: {}, AggregateId: {}",
                event.getId(), event.getEventType(), event.getAggregateId());

        // MessagePublisher로 발행 (aggregateId를 키로 사용)
        messagePublisher.publish(
                        event.getEventType(),
                        event.getAggregateId(),  // ← 메시지 키 (순서 보장)
                        event.getPayload()
                )
                .whenComplete((result, ex) -> {
                    // 발행 결과에 따라 상태 업데이트 (별도 트랜잭션)
                    updateEventStatus(event.getId(), ex, result);
                });
    }

    /**
     * 콜백에서 이벤트 상태 업데이트
     * 별도 트랜잭션으로 실행하여 콜백 스레드에서 안전하게 DB 업데이트
     *
     * 동시성 고려사항:
     * - REQUIRES_NEW 트랜잭션으로 실행 (독립적)
     * - 같은 Outbox 레코드를 여러 번 발행하지 않으므로 동시 업데이트 가능성 낮음
     * - AFTER_COMMIT 리스너는 한 번만 실행
     * - 폴러는 SKIP LOCKED + 7초 필터링으로 중복 방지
     * - 따라서 낙관적 락(@Version) 불필요
     *
     * @param eventId Outbox 이벤트 ID
     * @param ex 예외 (성공 시 null)
     * @param result 발행 결과 (성공 시)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateEventStatus(Long eventId, Throwable ex, MessagePublisher.PublishResult result) {
        try {
            OutboxEventTable event = outboxRepository.findById(eventId)
                    .orElseThrow(() -> new IllegalStateException("Outbox 이벤트를 찾을 수 없음: " + eventId));

            if (ex == null) {
                // 성공 → PUBLISHED
                event.markAsPublished();
                outboxRepository.save(event);

                log.info("✅ 이벤트 발행 성공 - Outbox ID: {}, Type: {}, Key: {}, Partition: {}, Offset: {}",
                        eventId, event.getEventType(), result.key(), result.partition(), result.offset());

            } else {
                // 실패 → PENDING 유지 또는 DEAD_LETTER
                event.incrementRetryCount();
                event.setErrorMessage(extractErrorMessage(ex));
                event.setLastRetryAt(LocalDateTime.now());

                if (event.getRetryCount() >= event.getMaxRetry()) {
                    event.markAsDeadLetter();
                    log.error("❌ 이벤트 발행 최종 실패 (DEAD_LETTER) - Outbox ID: {}, Retry: {}/{}",
                            eventId, event.getRetryCount(), event.getMaxRetry());
                } else {
                    // PENDING 상태 유지 (폴러가 재처리)
                    log.warn("⚠️ 이벤트 발행 실패 (재시도 예정) - Outbox ID: {}, Retry: {}/{}",
                            eventId, event.getRetryCount(), event.getMaxRetry());
                }

                outboxRepository.save(event);
            }

        } catch (Exception updateEx) {
            // 상태 업데이트 실패 시 로그만 남기고 예외를 던지지 않음
            // (콜백 스레드에서 예외를 던지면 처리할 곳이 없음)
            log.error("상태 업데이트 실패 - Outbox ID: {}", eventId, updateEx);
        }
    }

    /**
     * 예외 메시지 추출 (CompletionException 언래핑)
     */
    private String extractErrorMessage(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }
}
