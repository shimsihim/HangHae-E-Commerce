package io.hhplus.tdd.common.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEventTable, Long> {

    /**
     * PENDING 상태이고 생성 시간이 임계값 이전인 이벤트 조회 (SKIP LOCKED)
     *
     * SKIP LOCKED 사용 이유:
     * 1. 여러 폴러 인스턴스가 동시 실행 가능 (락 대기 없음)
     * 2. 각 폴러가 다른 레코드 처리 (높은 처리량)
     * 3. DB 부하 최소화
     *
     * 생성 시간 필터링 이유:
     * 1. AFTER_COMMIT 리스너가 처리 중인 최근 레코드 제외
     * 2. 중복 발행 방지
     * 3. 7초 이상 지난 = 실패로 간주하고 재시도
     *
     * @param status 상태 (PENDING)
     * @param createdBefore 생성 시간 임계값 (현재 시간 - 7초)
     * @param pageable 페이징 (limit)
     * @return 처리할 Outbox 이벤트 목록
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "javax.persistence.lock.timeout", value = "0"))
    @Query(value = """
        SELECT * FROM outbox_events
        WHERE status = :status
        AND created_at < :createdBefore
        ORDER BY created_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEventTable> findPendingEventsForRetry(
            @Param("status") String status,
            @Param("createdBefore") LocalDateTime createdBefore,
            @Param("limit") int limit
    );

    /**
     * 특정 aggregate의 특정 이벤트 타입의 최근 PENDING 레코드 조회
     * AFTER_COMMIT 리스너에서 방금 저장한 Outbox 레코드를 찾기 위해 사용
     */
    OutboxEventTable findTopByAggregateTypeAndAggregateIdAndEventTypeAndStatusOrderByCreatedAtDesc(
            String aggregateType,
            String aggregateId,
            String eventType,
            OutboxStatus status
    );

    /**
     * 오래된 PUBLISHED 레코드 삭제 (정리용)
     */
    @Modifying
    @Query("DELETE FROM OutboxEventTable e WHERE e.status = :status AND e.publishedAt < :threshold")
    int deleteByStatusAndPublishedAtBefore(
            @Param("status") OutboxStatus status,
            @Param("threshold") LocalDateTime threshold
    );

    /**
     * 특정 aggregate의 최근 이벤트 조회 (디버깅용)
     */
    List<OutboxEventTable> findByAggregateTypeAndAggregateIdOrderByCreatedAtDesc(
            String aggregateType,
            String aggregateId,
            Pageable pageable
    );
}
