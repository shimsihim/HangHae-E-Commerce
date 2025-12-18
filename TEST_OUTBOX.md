# Outbox 패턴 테스트 가이드

## 구현 완료 내용

### 1. Outbox 인프라
- `OutboxEventTable`: Outbox 이벤트 JPA 엔티티
- `OutboxEvent`: Outbox 도메인 모델
- `OutboxStatus`: 이벤트 상태 enum (PENDING, SENDING, PUBLISHED, FAILED, DEAD_LETTER)
- `OutboxEventRepository`: JPA 리포지토리

### 2. 이벤트 발행 흐름
```
주문 결제 완료 (트랜잭션 내)
  ↓
OrderEventPublisher.publishOrderCompletedEvent()
  ↓
OutboxPublisher.publish()
  → Outbox 테이블에 PENDING 상태로 저장
  → OutboxCreatedEvent 발행 (Spring Event)
  ↓
[트랜잭션 커밋]
  ↓
OutboxEventListener.handleOutboxCreatedEvent() (@TransactionalEventListener AFTER_COMMIT)
  → Kafka로 즉시 발행 시도 (비동기 + 콜백)
  → 성공: PUBLISHED
  → 실패: PENDING 유지 (스케줄러가 재처리)
  ↓
OutboxEventPoller (스케줄러)
  → PENDING 이벤트 재발행 (1초마다)
  → SENDING 타임아웃 처리 (5초마다)
  → 오래된 PUBLISHED 삭제 (매일 새벽 3시)
```

### 3. 상태 전이도
```
PENDING (초기)
   ↓
SENDING (발행 중)
   ↓
   ├─ 성공 콜백 → PUBLISHED ✅
   ├─ 실패 콜백 → PENDING (재시도) 또는 DEAD_LETTER (최대 재시도 초과)
   └─ 타임아웃 → PENDING (재시도) 또는 DEAD_LETTER (최대 재시도 초과)
```

## 테스트 방법

### 1. Kafka 시작

```bash
cd doc
docker-compose up -d

# Kafka 정상 작동 확인
docker exec -it ecommerce-api-kafka /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:29092

# Kafka UI 접속: http://localhost:8090
```

### 2. 토픽 생성 (자동 생성 안되는 경우)

```bash
docker exec -it ecommerce-api-kafka /opt/kafka/bin/kafka-topics.sh \
  --create \
  --topic OrderCompleted \
  --bootstrap-server localhost:29092 \
  --partitions 3 \
  --replication-factor 1
```

### 3. Consumer 실행 (메시지 확인용)

```bash
docker exec -it ecommerce-api-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --topic OrderCompleted \
  --bootstrap-server localhost:29092 \
  --from-beginning \
  --property print.key=true \
  --property print.timestamp=true
```

### 4. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 5. 주문 결제 API 호출

```bash
# 1. 사용자 포인트 충전
curl -X POST http://localhost:8080/api/points/charge \
  -H "Content-Type: application/json" \
  -H "My-User-Id: 1" \
  -d '{"amount": 100000}'

# 2. 주문 생성
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "My-User-Id: 1" \
  -d '{
    "orderItems": [
      {"productOptionId": 1, "quantity": 2}
    ]
  }'

# 3. 주문 결제 (응답에서 orderId 확인 후 사용)
curl -X POST http://localhost:8080/api/orders/{orderId}/payment \
  -H "My-User-Id: 1"
```

### 6. Outbox 테이블 확인

```sql
-- Outbox 이벤트 조회
SELECT id, aggregate_type, event_type, status, retry_count, created_at, published_at, error_message
FROM outbox_events
ORDER BY created_at DESC;

-- 상태별 카운트
SELECT status, COUNT(*)
FROM outbox_events
GROUP BY status;
```

### 7. 로그 확인

애플리케이션 로그에서 다음 내용 확인:

```
📝 Outbox 이벤트 저장 완료 - ID: 1, Type: OrderCompleted, Aggregate: ORDER:1
✅ 이벤트 발행 성공 - ID: 1, Type: OrderCompleted, Partition: 0, Offset: 0
```

## 테스트 시나리오

### 시나리오 1: 정상 발행
1. 주문 결제 완료
2. Outbox에 PENDING 저장
3. 트랜잭션 커밋 후 즉시 Kafka 발행
4. PUBLISHED 상태로 변경
5. Consumer에서 메시지 수신 확인

### 시나리오 2: 즉시 발행 실패 + 폴러 재발행
1. Kafka 중단: `docker stop ecommerce-api-kafka`
2. 주문 결제 완료
3. Outbox에 PENDING 저장
4. 즉시 발행 실패 → PENDING 유지
5. Kafka 재시작: `docker start ecommerce-api-kafka`
6. 폴러가 PENDING 이벤트 자동 재발행
7. PUBLISHED 상태로 변경

### 시나리오 3: 최대 재시도 초과
1. Kafka 중단
2. 주문 결제 완료 (여러 번)
3. 폴러가 재시도 (maxRetry = 3)
4. 3번 실패 후 DEAD_LETTER 상태로 전환
5. 로그에서 DEAD_LETTER 확인
6. 수동 처리 필요

### 시나리오 4: 타임아웃 처리
1. Kafka 응답 지연 시뮬레이션
2. SENDING 상태로 30초 이상 유지
3. 타임아웃 핸들러가 PENDING으로 전환
4. 재시도 로직 실행

## 주의사항

1. **Kafka 연결**: Kafka가 실행 중이어야 테스트 가능
2. **DB 초기화**: `spring.jpa.hibernate.hbm2ddl.auto=create`로 설정되어 있어 재시작 시 데이터 초기화됨
3. **로그 레벨**: `DEBUG`로 설정되어 있어 상세한 로그 확인 가능
4. **폴러 간격**: 1초마다 실행되어 빠른 재처리 (운영에서는 조정 필요)
5. **트랜잭션 격리**: 콜백에서 별도 트랜잭션으로 상태 업데이트

## 모니터링 포인트

1. **Outbox 테이블 크기**: PUBLISHED 레코드가 쌓이지 않는지 확인
2. **DEAD_LETTER 레코드**: 수동 처리가 필요한 실패 이벤트
3. **재시도 횟수**: retry_count가 높은 레코드 모니터링
4. **Kafka Lag**: Consumer가 제대로 소비하고 있는지 확인
5. **폴러 성능**: PENDING 레코드 처리 시간

## 트러블슈팅

### Kafka 연결 실패
```
Error: org.apache.kafka.common.errors.TimeoutException
```
→ Kafka가 실행 중인지 확인: `docker ps | grep kafka`

### Outbox 테이블 없음
```
Error: Table 'commerce.outbox_events' doesn't exist
```
→ 애플리케이션 재시작 (hbm2ddl.auto=create로 테이블 자동 생성)

### 이벤트가 발행되지 않음
1. Outbox 테이블에 PENDING 레코드가 있는지 확인
2. 폴러 로그 확인 (1초마다 실행되어야 함)
3. Kafka 정상 작동 확인

### DEAD_LETTER 레코드 처리
```sql
-- DEAD_LETTER 레코드 확인
SELECT * FROM outbox_events WHERE status = 'DEAD_LETTER';

-- 수동으로 PENDING으로 전환 (재시도)
UPDATE outbox_events
SET status = 'PENDING', retry_count = 0, error_message = NULL
WHERE id = {id};
```
