package io.hhplus.tdd.domain;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
public abstract class ContainerIntegrationTest {

    // 1. 컨테이너 정의 (static final로 선언하여 클래스 로딩 시점에 생성)
    private static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true) // 로컬 개발 시 컨테이너 재사용 (Testcontainers 설정 필요)
            .waitingFor(Wait.forLogMessage(".*ready for connections.*\\n", 1));

    private static final GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    // 2. 컨테이너 시작 (static 블록을 통해 최초 1회만 실행됨 -> 싱글톤 효과)
    static {
        MYSQL_CONTAINER.start();
        REDIS_CONTAINER.start();
        // 종료는 Testcontainers(Ryuk)가 JVM 종료 시 알아서 처리하므로 stop() 호출 불필요
    }

    // 3. 동적 프로퍼티 설정
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // MySQL
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);

        // Redis
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", REDIS_CONTAINER::getFirstMappedPort);

        // HikariCP 안정성 설정
        registry.add("spring.datasource.hikari.max-lifetime", () -> "1800000");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "0");
    }
}