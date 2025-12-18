package io.hhplus.tdd.common.outbox;

import io.hhplus.tdd.common.baseEntity.CreatedBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "outbox_events",
    indexes = {
        @Index(name = "idx_status_created", columnList = "status, created_at"),
        @Index(name = "idx_aggregate", columnList = "aggregate_type, aggregate_id"),
        @Index(name = "idx_status_updated", columnList = "status, updated_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEventTable extends CreatedBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String aggregateType;  // ORDER, COUPON, POINT

    @Column(nullable = false, length = 100)
    private String aggregateId;

    @Column(nullable = false, length = 100)
    private String eventType;  // OrderCompleted, CouponIssued 등

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;  // JSON

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private Integer retryCount = 0;

    @Column(nullable = false)
    private Integer maxRetry = 3;

    private LocalDateTime publishedAt;

    private LocalDateTime lastRetryAt;

    private LocalDateTime updatedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Builder
    public OutboxEventTable(
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload,
            Integer maxRetry
    ) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
        this.maxRetry = maxRetry != null ? maxRetry : 3;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsDeadLetter() {
        this.status = OutboxStatus.DEAD_LETTER;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementRetryCount() {
        this.retryCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }

    public void setLastRetryAt(LocalDateTime lastRetryAt) {
        this.lastRetryAt = lastRetryAt;
        this.updatedAt = LocalDateTime.now();
    }
}
