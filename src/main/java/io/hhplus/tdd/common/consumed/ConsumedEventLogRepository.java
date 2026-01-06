package io.hhplus.tdd.common.consumed;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConsumedEventLogRepository extends JpaRepository<ConsumedEventLog, Long> {

    // 멱등성 검증
    boolean existsByEventIdAndEventType(String eventId, String eventType);

    Optional<ConsumedEventLog> findByEventIdAndEventType(String eventId, String eventType);

    // 오래된 로그 삭제 (보관 기간 초과)
    @Modifying
    @Query("DELETE FROM ConsumedEventLog c WHERE c.createdAt < :threshold")
    int deleteByCreatedAtBefore(@Param("threshold") LocalDateTime threshold);
}
