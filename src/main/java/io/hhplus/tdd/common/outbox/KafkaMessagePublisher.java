package io.hhplus.tdd.common.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaMessagePublisher implements MessagePublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public CompletableFuture<PublishResult> publish(String eventType, String key, String payload) {
        log.debug("Kafka 전송 시작 - Topic: {}, Key: {}", eventType, key);

        CompletableFuture<PublishResult> resultFuture = new CompletableFuture<>();

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

