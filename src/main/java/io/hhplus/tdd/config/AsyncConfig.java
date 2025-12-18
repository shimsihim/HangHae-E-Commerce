package io.hhplus.tdd.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 및 스케줄링 설정
 *
 * @EnableAsync: TddApplication에 이미 존재 (중복 제거)
 * @EnableScheduling: 필수 (CacheWarmupScheduler, OutboxEventPoller 사용)
 * AsyncConfigurer: 커스텀 스레드풀 설정 (운영 환경 권장)
 */
@Configuration
@EnableScheduling
public class AsyncConfig implements AsyncConfigurer {

    /**
     * @Async 메서드에서 사용할 커스텀 스레드풀 설정
     * 기본 SimpleAsyncTaskExecutor는 매번 새 스레드를 생성하므로 비효율적
     *
     * 사용처:
     * - OrderEventListener.publishToKafka() (@Async)
     * - 기타 @Async 메서드들
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);           // 기본 스레드 수
        executor.setMaxPoolSize(10);           // 최대 스레드 수
        executor.setQueueCapacity(100);        // 큐 크기
        executor.setThreadNamePrefix("async-"); // 스레드 이름 (디버깅용)
        executor.initialize();
        return executor;
    }
}
