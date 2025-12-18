package io.hhplus.tdd.domain.coupon.application;

import io.hhplus.tdd.domain.IntegrationTest;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.DiscountType;
import io.hhplus.tdd.domain.coupon.domain.model.Status;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.CouponRepository;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.UserCouponRepository;
import io.hhplus.tdd.domain.coupon.presentation.dto.req.CouponIssueReqDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@Slf4j
class IssueUserCouponIntegrationTest extends IntegrationTest {

    @Autowired
    private IssueUserCouponUseCase issueUserCouponUseCase;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private CouponService couponService;

    // Consumer는 백그라운드에서 돌고 있으므로 주입만 받아두거나(필요시),
    // 실제 로직 호출은 하지 않습니다.

    @BeforeEach
    void setup() {
        userCouponRepository.deleteAll();
        couponRepository.deleteAll();
    }

    @Test
    @DisplayName("쿠폰 발급 통합 테스트 - 큐를 통해 정상적으로 발급된다")
    void 쿠폰_발급_성공() throws InterruptedException {
        // given
        Coupon coupon = createCoupon(100, 0); // 수량 100개
        Coupon savedCoupon = couponRepository.save(coupon);
        long userId = 1L;

        // when
        // 1. UseCase 실행 -> Redis 큐에 적재
        issueUserCouponUseCase.execute(new IssueUserCouponUseCase.Input(savedCoupon.getId(), userId));

        // 2. 비동기 처리가 완료될 때까지 대기 (최대 2초)
        waitForCouponIssue(userId, savedCoupon.getId());

        // then
        List<UserCoupon> userCoupons = userCouponRepository.findByUserIdAndCouponId(userId, savedCoupon.getId());
        assertThat(userCoupons).hasSize(1);

        UserCoupon issuedCoupon = userCoupons.get(0);
        assertThat(issuedCoupon.getStatus()).isEqualTo(Status.ISSUED);

        Coupon updatedCoupon = couponRepository.findById(savedCoupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getIssuedQuantity()).isEqualTo(1);
    }

    @Test
    void 쿠폰_발급_실패_전체_발급_한도_초과() throws InterruptedException {
        // given
        // 이미 100개가 다 발급된 상태
        Coupon coupon = createCoupon(100, 100);
        Coupon savedCoupon = couponRepository.save(coupon);
        long userId = 1L;

        // when
        issueUserCouponUseCase.execute(new IssueUserCouponUseCase.Input(savedCoupon.getId(), userId));

        // 처리 대기 (실패라서 DB에 안 쌓이겠지만, 처리가 끝날 시간은 줘야 함)
        Thread.sleep(1000);

        // then
        List<UserCoupon> userCoupons = userCouponRepository.findByUserIdAndCouponId(userId, savedCoupon.getId());
        assertThat(userCoupons).isEmpty(); // 발급되지 않아야 함
    }

    @Test
    void 쿠폰_발급_실패_개인_발급_한도_초과() throws InterruptedException {
        // given
        Coupon coupon = createCoupon(100, 0);
        Coupon savedCoupon = couponRepository.save(coupon);
        long userId = 1L;

        // 1. 먼저 하나 발급 (직접 DB에 넣어서 상황 연출)
        CouponIssueReqDTO couponIssueReqDTO = new CouponIssueReqDTO(userId , savedCoupon.getId());
        couponService.issueCoupon(couponIssueReqDTO);

        System.out.println("사전세팅 통과");

        // when
        // 2. 같은 유저가 또 요청
        issueUserCouponUseCase.execute(new IssueUserCouponUseCase.Input(savedCoupon.getId(), userId));

        // 처리 대기
        Thread.sleep(1000);

        // then
        List<UserCoupon> userCoupons = userCouponRepository.findByUserIdAndCouponId(userId, savedCoupon.getId());
        assertThat(userCoupons).hasSize(1); // 기존 1개만 유지되어야 함 (2개가 되면 안됨)
    }

    @Test
    @DisplayName("동시성 테스트 - 20명이 동시에 요청해도 순차적으로 20개가 정확히 발급된다")
    void 쿠폰_발급_동시성_테스트_순서_보장() throws InterruptedException {
        // given
        int threadCount = 20;
        Coupon coupon = createCoupon(1000, 0); // 넉넉한 수량
        Coupon savedCoupon = couponRepository.save(coupon);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            long userId = i + 1;
            executorService.submit(() -> {
                try {
                    issueUserCouponUseCase.execute(new IssueUserCouponUseCase.Input(savedCoupon.getId(), userId));
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(); // 요청 전송 완료 대기

        // then
        // Consumer가 큐를 하나씩 처리하는 시간을 기다림 (Polling)
        // 최대 10초 대기, 수량이 20개가 되면 즉시 탈출
        int maxWaitTime = 10;
        int currentCount = 0;

        for (int i = 0; i < maxWaitTime * 10; i++) {
            Coupon c = couponRepository.findById(savedCoupon.getId()).orElseThrow();
            if (c.getIssuedQuantity() == threadCount) {
                currentCount = c.getIssuedQuantity();
                break;
            }
            Thread.sleep(100); // 0.1초 대기
        }

        Coupon updatedCoupon = couponRepository.findById(savedCoupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getIssuedQuantity()).isEqualTo(threadCount);
    }

    // --- Helper Methods ---

    private Coupon createCoupon(int total, int issued) {
        return Coupon.builder()
                .couponName("테스트 쿠폰")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10)
                .totalQuantity(total)
                .issuedQuantity(issued)
                .limitPerUser(1)
                .duration(30)
                .minOrderValue(10000)
                .validFrom(LocalDate.now().minusDays(1))
                .validUntil(LocalDate.now().plusDays(30))
                .build();
    }

    // DB에 데이터가 들어왔는지 확인하는 간단한 Polling 메서드
    private void waitForCouponIssue(long userId, long couponId) throws InterruptedException {
        for (int i = 0; i < 20; i++) { // 최대 2초 (100ms * 20)
            List<UserCoupon> list = userCouponRepository.findByUserIdAndCouponId(userId, couponId);
            if (!list.isEmpty()) {
                return; // 데이터 발견 시 즉시 리턴
            }
            Thread.sleep(100);
        }
    }
}