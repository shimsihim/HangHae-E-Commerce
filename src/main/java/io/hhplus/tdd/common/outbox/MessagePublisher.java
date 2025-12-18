package io.hhplus.tdd.common.outbox;

import java.util.concurrent.CompletableFuture;

/**
 * 메시지 발행자 인터페이스
 * 메시지 브로커(Kafka, RabbitMQ, SQS 등)에 독립적인 추상화
 *
 * 책임:
 * - 이벤트를 외부 메시지 브로커로 발행
 * - 발행 결과를 비동기로 반환
 *
 * 구현체:
 * - KafkaMessagePublisher: Kafka 구현
 * - RabbitMQMessagePublisher: RabbitMQ 구현 (향후)
 * - MockMessagePublisher: 테스트용 Mock 구현
 */
public interface MessagePublisher {

    /**
     * 이벤트를 메시지 브로커로 발행
     *
     * @param eventType 이벤트 타입 (토픽/큐 이름으로 사용)
     * @param key 메시지 키 (파티셔닝 기준, 순서 보장)
     *            - Kafka: 동일 키는 동일 파티션 → 순서 보장
     *            - RabbitMQ: 라우팅 키로 사용
     * @param payload 이벤트 페이로드 (JSON)
     * @return 발행 결과를 담은 CompletableFuture
     *         - 성공: 정상 완료
     *         - 실패: CompletionException으로 예외 전달
     */
    CompletableFuture<PublishResult> publish(String eventType, String key, String payload);

    /**
     * 발행 결과
     */
    record PublishResult(
            String eventType,
            String key,
            String messageId,
            int partition,
            long offset
    ) {}
}
