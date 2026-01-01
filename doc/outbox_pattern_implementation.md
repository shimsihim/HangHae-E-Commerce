# Outbox Pattern Implementation Guide

## Overview

This document describes the **Outbox Pattern** implementation for reliable Kafka event publishing in the e-commerce order service.

### What is the Outbox Pattern?

The Outbox pattern ensures reliable event publishing by:
1. Storing events in a database table (Outbox) **within the same transaction** as the business operation
2. Publishing events to message brokers (Kafka) **after** the transaction commits
3. Using a background scheduler to retry failed publishes

This guarantees **at-least-once delivery** - if the business transaction succeeds, the event will eventually be published.

---

## Architecture Flow

```
[Order Payment Transaction]
    ↓
1. BEFORE_COMMIT: Save to Outbox Table
    ↓
[Transaction Commits]
    ↓
2. AFTER_COMMIT: Publish to Kafka (immediate attempt)
    ↓
3. Kafka Callback: Update Outbox status (SUCCESS/FAILURE)
    ↓
4. Scheduler (Backup): Retry failed events after 7+ seconds
```

### Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **SKIP LOCKED** | Allows multiple poller instances to process different events concurrently without blocking |
| **7-second threshold** | AFTER_COMMIT handles 0-7s (immediate), Poller handles 7+s (failed cases only) to prevent duplicate publishing |
| **Message Key = aggregateId** | Ensures all events for the same order go to the same Kafka partition for ordering guarantee |
| **MessagePublisher interface** | Abstracts message broker (Kafka, RabbitMQ, SQS) for easy replacement |
| **No @Version field** | SKIP LOCKED + time filtering prevents concurrent updates to same record |
| **REQUIRES_NEW propagation** | Kafka callback updates run in isolated transactions, safe for async threads |

---

## Database Schema

### Outbox Events Table

```sql
CREATE TABLE outbox_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    aggregate_type VARCHAR(50) NOT NULL,      -- ORDER, COUPON, POINT
    aggregate_id VARCHAR(100) NOT NULL,       -- Order ID, Coupon ID, etc.
    event_type VARCHAR(100) NOT NULL,         -- OrderCompleted, CouponIssued, etc.
    payload TEXT NOT NULL,                    -- JSON event data
    status VARCHAR(20) NOT NULL,              -- PENDING, PUBLISHED, DEAD_LETTER
    retry_count INT NOT NULL DEFAULT 0,
    max_retry INT NOT NULL DEFAULT 3,
    published_at DATETIME,
    last_retry_at DATETIME,
    updated_at DATETIME,
    error_message TEXT,
    created_at DATETIME NOT NULL,

    INDEX idx_status_created (status, created_at),
    INDEX idx_aggregate (aggregate_type, aggregate_id),
    INDEX idx_status_updated (status, updated_at)
);
```

### Status State Machine

```
PENDING ──(publish success)──> PUBLISHED
   │
   └──(retry exhausted)──> DEAD_LETTER
```

- **PENDING**: Waiting to be published
- **PUBLISHED**: Successfully published to Kafka
- **DEAD_LETTER**: Max retries exceeded, requires manual intervention

---

## Implementation Details

### 1. Event Publishing Flow (OrderEventListener.java)

```java
@Component
public class OrderEventListener {

    // Step 1: BEFORE_COMMIT - Save to Outbox (atomic with order transaction)
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void saveToOutbox(OrderCompletedEvent event) {
        OutboxEventTable outboxEvent = OutboxEventTable.builder()
            .aggregateType("ORDER")
            .aggregateId(event.getOrderId().toString())
            .eventType("OrderCompleted")
            .payload(objectMapper.writeValueAsString(event))
            .build();

        outboxRepository.save(outboxEvent);
    }

    // Step 2: AFTER_COMMIT - Immediate publish attempt
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishToKafka(OrderCompletedEvent event) {
        OutboxEventTable outboxEvent = outboxRepository
            .findTopByAggregateTypeAndAggregateIdAndEventTypeAndStatusOrderByCreatedAtDesc(
                "ORDER", event.getOrderId().toString(), "OrderCompleted", PENDING
            );

        outboxService.publishEvent(outboxEvent);
    }
}
```

### 2. Kafka Publishing with Callback (OutboxService.java)

```java
@Service
public class OutboxService {

    public void publishEvent(OutboxEventTable event) {
        messagePublisher.publish(
            event.getEventType(),
            event.getAggregateId(),  // Message key for ordering
            event.getPayload()
        ).whenComplete((result, ex) -> {
            // Callback runs after Kafka confirms receipt
            updateEventStatus(event.getId(), ex, result);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateEventStatus(Long eventId, Throwable ex, PublishResult result) {
        OutboxEventTable event = outboxRepository.findById(eventId)
            .orElseThrow(...);

        if (ex == null) {
            event.markAsPublished();  // SUCCESS
        } else {
            event.incrementRetryCount();
            event.setErrorMessage(extractErrorMessage(ex));

            if (event.getRetryCount() >= event.getMaxRetry()) {
                event.markAsDeadLetter();  // MAX RETRIES
            }
        }

        outboxRepository.save(event);
    }
}
```

### 3. Scheduler for Failed Events (OutboxEventPoller.java)

```java
@Component
public class OutboxEventPoller {

    @Scheduled(fixedDelay = 7000)  // Every 7 seconds
    @Transactional
    public void pollAndPublish() {
        // Only process events 7+ seconds old (failed immediate publish)
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(7);

        List<OutboxEventTable> pendingEvents = outboxRepository
            .findPendingEventsForRetry(
                OutboxStatus.PENDING.name(),
                threshold,
                100  // Batch size
            );

        for (OutboxEventTable event : pendingEvents) {
            outboxService.publishEvent(event);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")  // Daily at 3 AM
    @Transactional
    public void cleanupOldEvents() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        outboxRepository.deleteByStatusAndPublishedAtBefore(
            OutboxStatus.PUBLISHED, threshold
        );
    }
}
```

### 4. SKIP LOCKED Query (OutboxEventRepository.java)

```java
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
```

**Why SKIP LOCKED?**
- Multiple poller instances can run concurrently
- Each instance processes different rows (no blocking)
- High throughput with minimal DB contention
- Lock timeout = 0 means immediate skip (no waiting)

### 5. Message Key for Ordering (KafkaMessagePublisher.java)

```java
@Component
public class KafkaMessagePublisher implements MessagePublisher {

    @Override
    public CompletableFuture<PublishResult> publish(
        String eventType,
        String key,      // aggregateId (orderId)
        String payload
    ) {
        CompletableFuture<PublishResult> resultFuture = new CompletableFuture<>();

        // Send to Kafka with key
        kafkaTemplate.send(eventType, key, payload)
            .whenComplete((sendResult, ex) -> {
                if (ex != null) {
                    resultFuture.completeExceptionally(
                        new CompletionException("Kafka 전송 실패: " + eventType, ex)
                    );
                } else {
                    PublishResult result = new PublishResult(
                        eventType,
                        key,
                        sendResult.getRecordMetadata().toString(),
                        sendResult.getRecordMetadata().partition(),
                        sendResult.getRecordMetadata().offset()
                    );
                    resultFuture.complete(result);
                }
            });

        return resultFuture;
    }
}
```

**Message Key Guarantees:**
- Same `orderId` → Same Kafka partition
- Messages within a partition are ordered
- Consumer processes events in order for each order

---

## Concurrency Control

### Why No @Version Field?

**Concurrent update scenarios DON'T exist because:**

1. **AFTER_COMMIT listener runs once per transaction**
   - Spring guarantees single execution
   - No duplicate listeners firing

2. **Poller uses time-based filtering (7+ seconds)**
   - AFTER_COMMIT handles 0-7s range
   - Poller handles 7+s range
   - No overlap between them

3. **SKIP LOCKED prevents row locking**
   - Different poller instances process different rows
   - Same row never processed concurrently

**Therefore:** Optimistic locking (`@Version`) is unnecessary overhead.

**Defensive Programming:**
- `updateEventStatus()` wrapped in try-catch
- Logs errors but doesn't throw (safe for async callbacks)
- Fail-safe approach for unexpected edge cases

---

## Kafka Configuration

### Producer Settings (KafkaConfig.java)

```java
@Configuration
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Reliability settings
        config.put(ProducerConfig.ACKS_CONFIG, "all");              // Wait for all replicas
        config.put(ProducerConfig.RETRIES_CONFIG, 3);               // Retry 3 times
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);  // Ordering guarantee

        return new DefaultKafkaProducerFactory<>(config);
    }
}
```

### Application Configuration (application.yml)

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      retries: 3
```

---

## Testing

### Start Kafka (Docker)

```bash
cd doc
docker-compose up -d
```

### Verify Outbox Pattern

1. **Create an order and complete payment:**
```bash
curl -X POST http://localhost:8080/orders/payment \
  -H "Content-Type: application/json" \
  -H "My-User-Id: 1" \
  -d '{"orderId": 123}'
```

2. **Check Outbox table:**
```sql
SELECT * FROM outbox_events
WHERE aggregate_type = 'ORDER'
ORDER BY created_at DESC;
```

Expected result:
- `status = 'PUBLISHED'` if Kafka succeeded
- `status = 'PENDING'` if Kafka failed (will be retried by poller)

3. **Check Kafka topic:**
```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic OrderCompleted \
  --from-beginning
```

### Failure Scenario Testing

1. **Stop Kafka:**
```bash
docker-compose stop kafka
```

2. **Create order** → Outbox saved with `status = 'PENDING'`

3. **Check logs:** AFTER_COMMIT publish fails

4. **Start Kafka:**
```bash
docker-compose start kafka
```

5. **Wait 7 seconds** → Poller retries → `status = 'PUBLISHED'`

---

## Monitoring

### Key Metrics to Track

1. **Outbox lag:** Count of PENDING events
```sql
SELECT COUNT(*) FROM outbox_events WHERE status = 'PENDING';
```

2. **Dead letter queue:** Events requiring manual intervention
```sql
SELECT * FROM outbox_events WHERE status = 'DEAD_LETTER';
```

3. **Retry statistics:**
```sql
SELECT
    AVG(retry_count) as avg_retries,
    MAX(retry_count) as max_retries,
    COUNT(*) as total_events
FROM outbox_events
WHERE status = 'PUBLISHED';
```

4. **Event age (latency):**
```sql
SELECT
    event_type,
    AVG(TIMESTAMPDIFF(SECOND, created_at, published_at)) as avg_latency_seconds
FROM outbox_events
WHERE status = 'PUBLISHED'
GROUP BY event_type;
```

### Log Messages

- `📝 [BEFORE_COMMIT] Outbox 저장 완료` - Event saved to Outbox
- `📤 [AFTER_COMMIT] Outbox 발행 위임` - Immediate publish attempt
- `✅ 이벤트 발행 성공` - Kafka publish succeeded
- `⚠️ 이벤트 발행 실패 (재시도 예정)` - Kafka publish failed, will retry
- `❌ 이벤트 발행 최종 실패 (DEAD_LETTER)` - Max retries exceeded
- `🔄 [폴러] 재발행 대기 중인 이벤트 N건 처리 시작` - Poller retry batch

---

## Adding New Event Types

### Example: CouponIssuedEvent

1. **Create event class:**
```java
public record CouponIssuedEvent(
    Long couponId,
    Long userId,
    String couponName
) {
    public static CouponIssuedEvent from(UserCoupon userCoupon) {
        return new CouponIssuedEvent(
            userCoupon.getCouponId(),
            userCoupon.getUserId(),
            userCoupon.getCouponName()
        );
    }
}
```

2. **Create event listener:**
```java
@Component
@RequiredArgsConstructor
public class CouponEventListener {

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void saveToOutbox(CouponIssuedEvent event) {
        String payload = objectMapper.writeValueAsString(event);

        OutboxEventTable outboxEvent = OutboxEventTable.builder()
            .aggregateType("COUPON")
            .aggregateId(event.couponId().toString())
            .eventType("CouponIssued")
            .payload(payload)
            .build();

        outboxRepository.save(outboxEvent);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishToKafka(CouponIssuedEvent event) {
        OutboxEventTable outboxEvent = outboxRepository
            .findTopByAggregateTypeAndAggregateIdAndEventTypeAndStatusOrderByCreatedAtDesc(
                "COUPON", event.couponId().toString(), "CouponIssued", PENDING
            );

        outboxService.publishEvent(outboxEvent);
    }
}
```

3. **Publish event in use case:**
```java
@Service
@RequiredArgsConstructor
public class IssueUserCouponUseCase {
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void execute(Long userId, Long couponId) {
        // Business logic...
        UserCoupon userCoupon = couponService.issueCoupon(...);

        // Publish event
        CouponIssuedEvent event = CouponIssuedEvent.from(userCoupon);
        eventPublisher.publishEvent(event);
    }
}
```

**That's it!** The Outbox infrastructure handles the rest:
- BEFORE_COMMIT saves to Outbox
- AFTER_COMMIT publishes to Kafka
- Poller retries failures
- Kafka callback updates status

---

## Troubleshooting

### Issue: Events stuck in PENDING

**Possible causes:**
1. Kafka is down
2. Network issues
3. Topic doesn't exist
4. Serialization errors

**Solution:**
```bash
# Check Kafka health
docker-compose ps

# Check Kafka logs
docker-compose logs kafka

# Verify topic exists
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Manual retry (SQL)
UPDATE outbox_events
SET retry_count = 0, updated_at = NOW()
WHERE status = 'PENDING' AND retry_count >= max_retry;
```

### Issue: Events in DEAD_LETTER

**Resolution steps:**
1. **Investigate error:**
```sql
SELECT id, event_type, aggregate_id, error_message, retry_count
FROM outbox_events
WHERE status = 'DEAD_LETTER'
ORDER BY created_at DESC;
```

2. **Fix underlying issue** (e.g., fix Kafka, update payload format)

3. **Reset for retry:**
```sql
UPDATE outbox_events
SET status = 'PENDING', retry_count = 0, error_message = NULL
WHERE id = ?;
```

4. **Poller will retry automatically**

### Issue: Duplicate events in Kafka

**Check:**
1. Is time threshold set correctly? (Should be 7 seconds)
2. Are multiple pollers running? (Should use SKIP LOCKED)
3. Check `created_at` filter in repository query

**Prevention:**
- SKIP LOCKED is enabled
- Time filtering is working
- AFTER_COMMIT runs exactly once (Spring guarantee)

---

## Performance Tuning

### Poller Interval

Current: 7 seconds (balance between latency and DB load)

Adjust based on requirements:
- **Lower latency needed:** Decrease to 3-5 seconds (higher DB load)
- **Lower DB load needed:** Increase to 10-15 seconds (higher latency for failures)

```java
@Scheduled(fixedDelay = 7000)  // Milliseconds
```

### Batch Size

Current: 100 events per poll

```java
.findPendingEventsForRetry(..., 100);
```

Adjust based on:
- Average event size
- DB query performance
- Kafka producer throughput

### Cleanup Schedule

Current: Daily at 3 AM, delete events 7+ days old

```java
@Scheduled(cron = "0 0 3 * * *")
```

Considerations:
- Audit requirements (may need longer retention)
- Disk space constraints
- Query performance (fewer rows = faster queries)

---

## Future Enhancements

### 1. Partitioned Outbox Tables

For very high throughput, consider table partitioning:
```sql
CREATE TABLE outbox_events (...)
PARTITION BY RANGE (YEAR(created_at) * 100 + MONTH(created_at)) (
    PARTITION p202501 VALUES LESS THAN (202502),
    PARTITION p202502 VALUES LESS THAN (202503),
    ...
);
```

### 2. CDC (Change Data Capture)

Alternative to polling: use Debezium to stream Outbox changes directly to Kafka
- Zero application overhead
- Near real-time publishing
- Requires Kafka Connect infrastructure

### 3. Metrics and Alerting

Integrate with monitoring tools:
```java
@Component
public class OutboxMetrics {
    private final MeterRegistry registry;

    public void recordPublishSuccess(String eventType) {
        registry.counter("outbox.publish.success", "type", eventType).increment();
    }

    public void recordPublishFailure(String eventType) {
        registry.counter("outbox.publish.failure", "type", eventType).increment();
    }
}
```

### 4. Event Deduplication

Add idempotency key for exactly-once consumer processing:
```java
OutboxEventTable.builder()
    .idempotencyKey(UUID.randomUUID().toString())
    ...
```

---

## References

- [Transactional Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html)
- [Spring @TransactionalEventListener](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/event/TransactionalEventListener.html)
- [Kafka Message Ordering](https://kafka.apache.org/documentation/#semantics)
- [MySQL SKIP LOCKED](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking-reads.html#innodb-locking-reads-nowait-skip-locked)
