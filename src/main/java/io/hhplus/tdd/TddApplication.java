package io.hhplus.tdd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing  // JPA Auditing (CreatedBaseEntity, UpdatableBaseEntity)
@EnableRetry        // @Retryable 지원 (낙관적 락 재시도)
@EnableAsync        // @Async 지원 (비동기 처리)
public class TddApplication {

    public static void main(String[] args) {
        SpringApplication.run(TddApplication.class, args);
    }
}

