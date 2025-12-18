package io.hhplus.tdd.common.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Kafka 기반 메시지 발행자
 * MessagePublisher 인터페이스의 Kafka 구현체
 *
 * 책임:
 * - Kafka로 메시지 전송 (키 포함)
 * - 전송 결과를 CompletableFuture로 반환
 * - 예외를 명확하게 전파
 *
 * 메시지 키의 중요성:
 * - 동일한 키를 가진 메시지는 동일한 파티션으로 전송됨
 * - 파티션 내에서 메시지 순서가 보장됨
 * - 예: 주문 ID를 키로 사용하면 해당 주문의 모든 이벤트 순서 보장
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaMessagePublisher implements MessagePublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public CompletableFuture<PublishResult> publish(String eventType, String key, String payload) {
        log.debug("Kafka 전송 시작 - Topic: {}, Key: {}", eventType, key);

        // CompletableFuture 생성하여 명확한 예외 처리
        CompletableFuture<PublishResult> resultFuture = new CompletableFuture<>();

        // Kafka로 비동기 전송 (키 포함)
        kafkaTemplate.send(eventType, key, payload)
                .whenComplete((sendResult, ex) -> {
                    if (ex != null) {
                        // 실패: CompletionException으로 감싸서 전파
                        log.error("Kafka 전송 실패 - Topic: {}, Key: {}", eventType, key, ex);
                        resultFuture.completeExceptionally(
                                new CompletionException("Kafka 전송 실패: " + eventType, ex)
                        );
                    } else {
                        // 성공: PublishResult 변환
                        SendResult<String, String> result = sendResult;
                        PublishResult publishResult = new PublishResult(
                                eventType,
                                key,
                                result.getRecordMetadata().toString(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()
                        );
                        resultFuture.complete(publishResult);

                        log.debug("Kafka 전송 성공 - Topic: {}, Key: {}, Partition: {}, Offset: {}",
                                eventType, key, publishResult.partition(), publishResult.offset());
                    }
                });

        return resultFuture;
    }
}

