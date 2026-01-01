package io.hhplus.tdd.domain;

import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.CouponRepository;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.UserCouponRepository;
import io.hhplus.tdd.domain.coupon.infrastructure.queue.CouponIssueConsumer;
import io.hhplus.tdd.domain.order.domain.service.OrderService;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderItemRepository;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderRepository;
import io.hhplus.tdd.domain.point.domain.service.PointService;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

/**
 * 통합 테스트를 위한 중간 클래스
 *
 * 전환 방법:
 * - Testcontainers 사용: extends BaseIntegrationTest
 * - 로컬 인프라 사용: extends LocalIntegrationTest
 */
public abstract class IntegrationTest extends ContainerIntegrationTest {

    @SpyBean
    protected CouponIssueConsumer couponIssueConsumer;

    @SpyBean
    // MockBean과의 차이점으로는 MockBean의 경우 껍데기만 있는 객체 ,SpyBean의 경우 스프링빈의 구현 기능을 일부만 덮어쓰고  덮어쓰지 않은 기능은 스프링빈 기능을 그대로 사용
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
