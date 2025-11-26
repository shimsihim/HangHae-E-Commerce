package io.hhplus.tdd.domain.coupon.application;

import io.hhplus.tdd.common.distributedLock.DistributedLock;
import io.hhplus.tdd.common.distributedLock.LockGroupType;
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

    @DistributedLock(group = LockGroupType.COUPON ,key = "#input.couponId")
    @Transactional
    public void execute(Input input){
        // 비관락을 걸어서 동일 사용자가 한번에 여러번 쿠폰을 요청하더라도 
        // 다음 요청은 이전 트랜젝션이 커밋을 완료한 후 쿠폰의 재고 상태 및 사용자의 쿠폰리스트 조회
        // 리스트의 조회에 대해서 정합성
        Coupon coupon = couponRepository.findForPessimisticById(input.couponId()).orElseThrow(()->{
            throw new CouponException(ErrorCode.COUPON_NOT_FOUND, input.couponId());
        });
        UserCoupon userCoupon = couponService.issueCoupon(coupon, input.userId());
        userCouponRepository.save(userCoupon);
    }

}
