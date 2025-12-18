package io.hhplus.tdd.common.consumed;

import io.hhplus.tdd.common.baseEntity.CreatedBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


// 모든 Consumer에서 공통 사용, 멱등성 보장
@Entity
@Table(
    name = "consumed_event_log",
    indexes = {
        @Index(name = "idx_consumed_created", columnList = "created_at"),
        @Index(name = "idx_consumed_event_type", columnList = "event_type, created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_consumed_event",
            columnNames = {"event_id", "event_type"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsumedEventLog extends CreatedBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이벤트 식별자 (aggregateId , 주문 ID 등)
    @Column(nullable = false, length = 100)
    private String eventId;

    // 이벤트 타입(이벤트 토픽 CouponIssued 등)
    @Column(nullable = false, length = 50)
    private String eventType;

    // 이벤트 처리 시간
    @Column(nullable = false)
    private LocalDateTime processedAt;

    // Consumer 이름 (디버깅 , OrderCompletedEventConsumer 등)
    @Column(nullable = false, length = 100)
    private String consumerName;

    // 원본 JSON 페이로드 (디버깅용)
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Builder
    public static ConsumedEventLog create(
            String eventId,
            String eventType,
            String consumerName,
            String payload
    ) {
        ConsumedEventLog log = new ConsumedEventLog();
        log.eventId = eventId;
        log.eventType = eventType;
        log.processedAt = LocalDateTime.now();
        log.consumerName = consumerName;
        log.payload = payload;
        return log;
    }
}
