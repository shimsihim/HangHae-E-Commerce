package io.hhplus.tdd.domain;

import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.CouponRepository;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.UserCouponRepository;
import io.hhplus.tdd.domain.order.domain.service.OrderService;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderItemRepository;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderRepository;
import io.hhplus.tdd.domain.point.domain.service.PointService;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
public abstract class BaseIntegrationTest {

    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    static {
        mysql.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.hikari.max-lifetime", () -> "30000"); // 30초
        registry.add("spring.datasource.hikari.connection-timeout", () -> "60000"); // 60초 (1분)

        // 3. 테스트 종료 시 연결 강제 소멸 (선택적)
        // 풀이 닫힐 때까지 기다리는 최대 시간. 테스트 간의 지연을 줄일 수 있습니다.
        registry.add("spring.datasource.hikari.idle-timeout", () -> "10000"); // 10초
    }

    @SpyBean // MockBean과의 차이점으로는 MockBean의 경우 껍데기만 있는 객체 ,SpyBean의 경우 스프링빈의 구현 기능을 일부만 덮어쓰고  덮어쓰지 않은 기능은 스프링빈 기능을 그대로 사용
    protected CouponService couponService;

    @SpyBean
    protected OrderService orderService;

    @SpyBean
    protected PointService pointService;

    @SpyBean
    protected CouponRepository couponRepository;

    @SpyBean
    protected UserCouponRepository userCouponRepository;

    @SpyBean
    protected OrderItemRepository orderItemRepository;

    @SpyBean
    protected OrderRepository orderRepository;

    @AfterEach
    void tearDown() {
        Mockito.reset(couponService, orderService , pointService , couponRepository , userCouponRepository , orderItemRepository , orderRepository);
    }
}