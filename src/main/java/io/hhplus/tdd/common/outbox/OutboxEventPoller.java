package io.hhplus.tdd.common.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 이벤트 폴러
 * 1. PENDING 상태 이벤트를 주기적으로 Kafka로 재발행
 * 2. SENDING 상태가 오래된 이벤트(타임아웃) 처리
 * 3. 오래된 PUBLISHED 이벤트 정리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPoller {

    private final OutboxEventRepository outboxRepository;
    private final OutboxService outboxService;

    /**
     * PENDING 이벤트 재발행 (실패 케이스 백업)
     *
     * 실행 간격: 7초 (부하 감소)
     * 처리 대상: 7초 이상 지난 PENDING 이벤트만
     *
     * 동작 원리:
     * 1. AFTER_COMMIT 리스너가 0~7초 내에 즉시 발행 시도
     * 2. 폴러는 7초 이상 지난 실패 케이스만 처리 (백업)
     * 3. SKIP LOCKED로 여러 폴러 인스턴스 동시 실행 가능
     *
     * 중복 발행 방지:
     * - 최근 7초 이내 레코드는 AFTER_COMMIT 리스너 처리 중으로 간주
     * - 폴러는 7초 이상 지난 레코드만 조회하여 중복 방지
     */
    @Scheduled(fixedDelay = 7000)
    @Transactional
    public void pollAndPublish() {
        // 7초 이상 지난 PENDING 이벤트만 조회 (즉시 발행 실패 케이스)
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(7);
        List<OutboxEventTable> pendingEvents = outboxRepository
                .findPendingEventsForRetry(
                        OutboxStatus.PENDING.name(),
                        threshold,
                        100
                );

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("🔄 [폴러] 재발행 대기 중인 이벤트 {}건 처리 시작 (7초+ 경과)", pendingEvents.size());

        for (OutboxEventTable event : pendingEvents) {
            publishEvent(event);
        }
    }

    /**
     * 오래된 PUBLISHED 이벤트 정리
     * 7일 이상 지난 PUBLISHED 레코드 삭제
     * 매일 새벽 3시 실행
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldEvents() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        int deleted = outboxRepository.deleteByStatusAndPublishedAtBefore(
                OutboxStatus.PUBLISHED,
                threshold
        );

        if (deleted > 0) {
            log.info("🗑️ {}건의 오래된 PUBLISHED 이벤트 삭제 완료", deleted);
        }
    }

    /**
     * Kafka로 이벤트 발행 (OutboxService 위임)
     */
    private void publishEvent(OutboxEventTable event) {
        outboxService.publishEvent(event);
    }
}
