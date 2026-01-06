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

    // PENDING 상태이고 생성 시간이 임계값 이전인 이벤트 조회 (SKIP LOCKED)
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

    // AFTER_COMMIT 리스너에서 방금 저장한 Outbox 레코드를 찾기 위해 사용
    OutboxEventTable findTopByAggregateTypeAndAggregateIdAndEventTypeAndStatusOrderByCreatedAtDesc(
            String aggregateType,
            String aggregateId,
            String eventType,
            OutboxStatus status
    );

    @Modifying
    @Query("DELETE FROM OutboxEventTable e WHERE e.status = :status AND e.publishedAt < :threshold")
    int deleteByStatusAndPublishedAtBefore(
            @Param("status") OutboxStatus status,
            @Param("threshold") LocalDateTime threshold
    );

}
