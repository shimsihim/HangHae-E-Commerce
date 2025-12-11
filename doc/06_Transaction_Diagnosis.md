# 도메인별 DB 분리 시 분산 트랜잭션 처리 설계 문서



## 1. 현재 시스템 분석

### 1.1  현재 트랜잭션 처리 방식

- **단일 트랜잭션으로 모든 도메인 작업이 하나의 트랜잭션으로 범위 내에서 실행** 

#### 결제 완료 프로세스 (PayCompleteOrderUseCase)
```java
public void execute(Input input) {
    // 1. 주문 조회
    Order order = orderRepository.findById(input.orderId());

    // 2. 분산 락 키 생성
    List<String> lockKeys = buildLockKeys(order.getUserId(), optionIds);

    // 3. 다중 분산 락 획득 후 트랜잭션 실행
    lockExecutor.executeWithLocks(lockKeys, () -> {
        transactionTemplate.executeWithoutResult(status -> {
            executePaymentLogic(order, orderItems, optionIds);
        });
    });
}
```


### 1.2 현재 시스템의 장점
1.  @Transactional 하나로 관리할 수 있어 단순하다.
2. 중간에 에러 등이 발생 시 자동으로 롤백을 해주어 일관성을 유지해준다.
3. 모든 작업이 1개의 소스 내에 있어 디버깅이 용이하다.

### 1.3 현재 시스템의 단점
1.  트래픽이 몰릴 때, 결제 처리가 지연되면 주문, 쿠폰, 포인트 테이블까지 락을 길게 잡고 있어 전체 시스템의 성능 저하
2. 외부 결제사 응답이 늦거나 에러가 나면, 주문 자체가 실패



---

## 2. 도메인별로 서버와 DB를 분리하였을 때


### 2.1 장점
1. 트래픽이 많은 도메인만 스케일 아웃 가능
2. 특정 도메인의 장애가 다른 도메인으로 전파되는것 최소화
3. 도메인별로 팀 간 독립적인 개발 및 배포 가능


### 2.2 단점 (문제점)
1. 기존 1개의 트랜잭션 내에 동작되었던 것들이 물리적으로 분리되어, 중간에 작업이 실패하더라도 롤백이 불가함 => 데이터 불일치
2. 네트워크 장애, 서비스 다운 등으로 부분 실패 발생 가능
2. 2PC를 사용한다면 성능 저하 및 블로킹 이슈 등이 있음


<BR><BR>

## 3. 대응 방안


###  Saga 패턴 

분산된 여러 서비스의 트랜잭션을 순차적으로 처리하되, 실패 시 보상 트랜잭션을 실행하여 데이터의 정합성을 맞추는 패턴

#### 3.1 Orchestration Saga 

**중앙 Orchestrator가 전체 흐름을 제어**하는 방식

**장점**:
1. 중앙 집중 관리로 전체 워크플로우를 한눈에 파악 가능
2. 각 단계의 상태를 추적 및 로깅 가능
3. 재시도 정책 적용 용이

**단점**:
1. 단일 장애점: Orchestrator 장애 시 모든 Saga 중단
2. 결합도 증가: Orchestrator가 모든 서비스를 알아야 함



#### 3.2 Choreography Saga

이벤트 기반으로 각 서비스가 독립적으로 반응하는 방식입니다.

**장점**:
1. 낮은 결합도로 서비스간 직접적인 호출 없이 이벤트로 통신
2. 새로운 서비스 추가 시 기존 이벤트 구독등을 통한 쉬운 확장

**단점**:
1. 처리의 흐름을 파악하기 어려움(디버깅의 어려움)
2. 잘못 설계 시 이벤트 루프 발생 가능


#### 3.3   구분
> **코레오그래피**  
> 주문 서비스에서 주문 상태 변경 후 주문완료 이벤트 발행후 종료  
> 쿠폰 서비스 , 결제 서비스 등에서는 주문 완료 이벤트를 받아 각 서비스 별 작업을 수행.  
> **Order Service: 메시지 1번 발송 (OrderCreated) , 나머지는 연쇄 반응**


 

>**오케스트레이션**
> 오케스트레이션에서 우선 주문 상태 변경 후 쿠폰 차감해 명령 발행  
> 쿠폰 서비스에서는 해당 명령을 듣고 쿠폰 차감 후 완료 응답 반환
> 오케스트레이션에서는 다음 서비스에 명령 발행 ...   
> **이벤트 방식의 경우 현재의 상태를 저장해야 함**





### 4 Outbox Pattern

이벤트 발행의 신뢰성을 보장하기 위한 패턴  (이벤트 발행한 후 롤백이 된다든가 등의 상황 예방)  
트랜잭션 내에서 Outbox 테이블에 이벤트를 저장하고, 별도 프로세스가 이를 메시지 브로커로 발행   


```java
// Outbox 테이블
@Entity
@Table(name = "outbox")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateType;  // "Order", "Point" 등
    private String aggregateId;    // 주문 ID, 사용자 ID 등
    private String eventType;      // "OrderCreated", "PointDeducted" 등
    private String payload;        //  이벤트 데이터(JSON)

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;   // PENDING, PUBLISHED, FAILED

    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private int retryCount;
}

// Outbox Poller: 주기적으로 PENDING 이벤트 발행
@Component
@RequiredArgsConstructor
public class OutboxEventPoller {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)  // 1초마다 실행
    public void pollAndPublish() {
        List<OutboxEvent> pendingEvents = outboxRepository
            .findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, PageRequest.of(0, 100));

        for (OutboxEvent event : pendingEvents) {
            try {
                // Kafka로 이벤트 발행
                kafkaTemplate.send(event.getEventType(), event.getPayload())
                    .get(5, TimeUnit.SECONDS);  // 동기 대기

                // 발행 성공 시 상태 업데이트
                event.markAsPublished();
                outboxRepository.save(event);

            } catch (Exception e) {
                log.error("이벤트 발행 실패: {}", event.getId(), e);
                event.incrementRetryCount();

                if (event.getRetryCount() >= 3) {
                    event.markAsFailed();
                    // 알림 발송 또는 Dead Letter Queue 저장
                }
                outboxRepository.save(event);
            }
        }
    }
}
```

**장점**:
1. 이벤트가 최소 1번은 발행됨
2.  비즈니스 로직과 이벤트 저장이 원자적
3. 발행 실패 시 자동 재시도

**단점**:
1. 중복 발행이 가능하므로 consumer측에서 멱등성을 보장해줘야 함.
2. 아웃박스 테이블의 관리가 필요
3. 주기적인 DB 조회로 인한 부하.




### 5. 최종 일관성 (Eventual Consistency)

요청 시점에서 모든 서비스의 역할을 수행 후 응답을 주는것이 아닌  
우선 이벤트 발행 후 202응답 반환.  
위의 사가 패턴 , 아웃박스 패턴등을 결과의 최종 일관성을 보장할 수 있음 

**장점**:
1.  일부 서비스 장애 시에도 나머지 서비스 동작 가능
2. 빠른 응답
3. 높은 처리량

**단점**:
1. 주문 완료까지 대기 필요
2. 중간 상태 추적 필요

=> 숏 폴링 등을 이용하여 UX 개선 등의 작업 필요 

