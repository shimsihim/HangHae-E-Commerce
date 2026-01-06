package io.hhplus.tdd.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;


    //개발용으로 , 토픽 생성 위해서..
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic orderCompletedTopic() {
        return new NewTopic("OrderCompleted", 1, (short) 1);
    }


    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Kafka 전송 신뢰성 설정
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");  // 모든 replica 확인
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);   // 재시도 3번
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);  // 순서 보장

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ========== Consumer (신규 추가) ==========

    /**
     * Consumer Factory 설정
     *
     * 신뢰성 설정:
     * - enable.auto.commit: true (자동 커밋, 간편성)
     * - auto.offset.reset: earliest (누락 방지)
     * - max.poll.records: 10 (배치 크기 제한)
     * - session.timeout.ms: 30000 (30초, 안정성)
     *
     * 멱등성:
     * - ConsumedEventLog로 중복 처리 방지
     * - Auto Commit + 멱등성 = at-least-once 보장
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // 기본 설정
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // 신뢰성 설정
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);  // 수동 커밋 (Spring Kafka 컨테이너가 담당)
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 5000);  // 5초
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");  // 처음부터

        // 성능 설정
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);  // 배치 크기
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);  // 30초
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);  // 10초

        // 역직렬화 에러 처리 (Gemini 권장)
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, StringDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Kafka Listener Container Factory
     *
     * 동시성:
     * - concurrency: 1 (단일 컨슈머, 순서 보장)
     * - 향후 확장: 파티션 수만큼 증가 가능
     *
     * Ack Mode:
     * - BATCH: 배치 단위로 Ack (성능 최적화)
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
            kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(1);  // 단일 컨슈머 (순서 보장)
        factory.getContainerProperties()
               .setAckMode(ContainerProperties.AckMode.BATCH);  // 배치 Ack

        // 에러 핸들러 (선택사항)
        // - 재시도는 멱등성으로 커버하므로 재시도 없음
        DefaultErrorHandler errorHandler = new DefaultErrorHandler((record, exception) -> {
            log.error("Kafka 메시지 처리 실패 - offset: {}, key: {}, message: {}",
                    record.offset(), record.key(), exception.getMessage());
        });
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
