package io.hhplus.tdd.domain.coupon.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.CouponRepository;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.UserCouponRepository;
import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class IssueUserCouponUseCase {

    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;
    private final CouponService couponService;

    public record Input(
            long couponId,
            long userId
    ){}

    @Transactional
    @Retryable(
            value = {ObjectOptimisticLockingFailureException.class , DeadlockLoserDataAccessException.class}, // 재시도할 예외 유형
            maxAttempts = 5,                                      // 최대 재시도 횟수
            backoff = @Backoff(delay = 100, random = true)                       // 100ms 지연 후 재시도
    )
    public void execute(Input input){


        Coupon coupon = couponRepository.findById(input.couponId()).orElseThrow(()->{
            log.warn("쿠폰 미존재");
            throw new CouponException(ErrorCode.COUPON_NOT_FOUND, input.couponId());
        });
        coupon.validIssue();

        List<UserCoupon> userIssuedCoupons = userCouponRepository.findByUserIdAndCouponId(input.userId(), input.couponId());
        UserCoupon userCoupon = couponService.issueCoupon(coupon, input.userId(), userIssuedCoupons);
        couponRepository.save(coupon);
        userCouponRepository.save(userCoupon);

    }

}
