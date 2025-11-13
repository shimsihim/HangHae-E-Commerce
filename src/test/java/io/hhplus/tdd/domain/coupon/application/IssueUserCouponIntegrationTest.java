package io.hhplus.tdd.domain.coupon.application;

import com.mysql.cj.exceptions.AssertionFailedException;
import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.DiscountType;
import io.hhplus.tdd.domain.coupon.domain.model.Status;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.CouponRepository;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.UserCouponRepository;
import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
@Slf4j
class IssueUserCouponIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private EntityManager em;

    @Autowired
    private IssueUserCouponUseCase issueUserCouponUseCase;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private CouponService couponService;

    @Test
    @DisplayName("쿠폰 발급 통합 테스트 - 사용자가 쿠폰을 정상적으로 발급받는다")
    void 쿠폰_발급_성공() {
        // given
        int initIssuedCnt = 0;
        Coupon coupon = Coupon.builder()
                .couponName("신규 회원 할인 쿠폰")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10)
                .totalQuantity(100)
                .issuedQuantity(initIssuedCnt)
                .limitPerUser(1)
                .duration(30)
                .minOrderValue(10000)
                .validFrom(LocalDate.now().minusDays(1))
                .validUntil(LocalDate.now().plusDays(30))
                .build();
        Coupon savedCoupon = couponRepository.save(coupon);
        em.clear();


        long userId = 1L;

        // when
        IssueUserCouponUseCase.Input input = new IssueUserCouponUseCase.Input(savedCoupon.getId(), userId);
        issueUserCouponUseCase.execute(input);

        // then
        List<UserCoupon> userCoupons = userCouponRepository.findByUserIdAndCouponId(userId, savedCoupon.getId());
        assertThat(userCoupons).hasSize(1);

        UserCoupon issuedCoupon = userCoupons.get(0);
        assertThat(issuedCoupon.getUserId()).isEqualTo(userId);
        assertThat(issuedCoupon.getCouponId()).isEqualTo(savedCoupon.getId());
        assertThat(issuedCoupon.getStatus()).isEqualTo(Status.ISSUED);
        assertThat(issuedCoupon.getExpiredAt()).isEqualTo(LocalDate.now().plusDays(30));

        Coupon updatedCoupon = couponRepository.findById(savedCoupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getIssuedQuantity()).isEqualTo(initIssuedCnt + 1);
    }

    @Test
    void 쿠폰_발급_실패_전체_발급_한도_초과() {
        // given
        Coupon coupon = Coupon.builder()
                .couponName("신규 회원 할인 쿠폰")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10)
                .totalQuantity(100)
                .issuedQuantity(100)
                .limitPerUser(1)
                .duration(30)
                .minOrderValue(10000)
                .validFrom(LocalDate.now().minusDays(1))
                .validUntil(LocalDate.now().plusDays(30))
                .build();
        Coupon savedCoupon = couponRepository.save(coupon);
        em.clear();

        long userId = 1L;

        // when
        IssueUserCouponUseCase.Input input = new IssueUserCouponUseCase.Input(savedCoupon.getId(), userId);
        ;
        Throwable ex = Assertions.catchThrowable(()->issueUserCouponUseCase.execute(input));
        assertThat(ex).isInstanceOf(CouponException.class);
        CouponException couponEx = (CouponException)ex;
        assertThat(couponEx.getErrCode()).isEqualTo(ErrorCode.COUPON_ISSUE_LIMIT);
        // then
        List<UserCoupon> userCoupons = userCouponRepository.findByUserIdAndCouponId(userId, savedCoupon.getId());
        assertThat(userCoupons).hasSize(0);
    }


    @Test
    void 쿠폰_발급_실패_개인_발급_한도_초과() {
        // given

        int issuedCnt = 1;
        Coupon coupon = Coupon.builder()
                .couponName("신규 회원 할인 쿠폰")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10)
                .totalQuantity(100)
                .issuedQuantity(issuedCnt)
                .limitPerUser(1)
                .duration(30)
                .minOrderValue(10000)
                .validFrom(LocalDate.now().minusDays(1))
                .validUntil(LocalDate.now().plusDays(30))
                .build();
        long userId = 1L;
        Coupon savedCoupon = couponRepository.save(coupon);
        UserCoupon userCoupon = couponService.issueCoupon(coupon, userId, new ArrayList<>());
        userCouponRepository.save(userCoupon);

        em.clear();


        // when
        IssueUserCouponUseCase.Input input = new IssueUserCouponUseCase.Input(savedCoupon.getId(), userId);

        // then
        Throwable ex = Assertions.catchThrowable(()->issueUserCouponUseCase.execute(input));
        assertThat(ex).isNotNull();
        assertThat(ex).isInstanceOf(CouponException.class);
        CouponException couponEx = (CouponException)ex;
        assertThat(couponEx.getErrCode()).isEqualTo(ErrorCode.COUPON_ISSUE_LIMIT_PER_USER);

        em.clear();
        Coupon coupon2 =  couponRepository.findById(coupon.getId()).orElseThrow(()->new AssertionFailedException(""));
        assertThat(coupon.getIssuedQuantity()).isEqualTo(issuedCnt+1);
    }


    /*@Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void 쿠폰_발급_동시성_테스트_성공() throws InterruptedException {
        // given

        int initIssuedCnt = 0;
        int threadCount = 20;

        Coupon coupon = Coupon.builder()
                .couponName("신규 회원 할인 쿠폰")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10)
                .totalQuantity(initIssuedCnt + threadCount + 20)
                .issuedQuantity(initIssuedCnt)
                .limitPerUser(1)
                .duration(30)
                .minOrderValue(10000)
                .validFrom(LocalDate.now().minusDays(1))
                .validUntil(LocalDate.now().plusDays(30))
                .build();
        // 🌟 TransactionTemplate을 사용하여 셋업 데이터를 즉시 커밋
        Coupon savedCoupon = transactionTemplate.execute(status -> {
            // 이 블록은 새 트랜잭션에서 실행되고 즉시 커밋됩니다.
            Coupon saved = couponRepository.save(coupon);
            return saved;
        });
        log.warn("쿠폰아이디 : {}" , savedCoupon.getId());


        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            long userId = i + 1;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    // when
                    IssueUserCouponUseCase.Input input = new IssueUserCouponUseCase.Input(savedCoupon.getId(), userId);
                    issueUserCouponUseCase.execute(input);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("Exception in thread userId=" + userId + ": " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();
        em.clear();


        // then
        Coupon updatedCoupon = couponRepository.findById(savedCoupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getIssuedQuantity())
                .isEqualTo(threadCount + initIssuedCnt);
    }*/
}