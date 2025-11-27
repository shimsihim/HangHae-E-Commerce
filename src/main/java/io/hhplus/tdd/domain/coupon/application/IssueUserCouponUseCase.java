package io.hhplus.tdd.domain.coupon.application;

import io.hhplus.tdd.common.cache.CacheEvictionService;
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
    private final CacheEvictionService cacheEvictionService;

    public record Input(
            long couponId,
            long userId
    ){}

    /**
     * 쿠폰 발급 후 남은 수량이 0이 되면 쿠폰 리스트 캐시 무효화
     */
    @DistributedLock(group = LockGroupType.COUPON ,key = "#input.couponId")
    @Transactional
    public void execute(Input input){
        Coupon coupon = couponRepository.findForPessimisticById(input.couponId()).orElseThrow(()->{
            throw new CouponException(ErrorCode.COUPON_NOT_FOUND, input.couponId());
        });
        UserCoupon userCoupon = couponService.issueCoupon(coupon, input.userId());
        userCouponRepository.save(userCoupon);

        // 쿠폰 발급 후 남은 수량 확인 (totalQuantity - issuedQuantity)
        int remainingQuantity = coupon.getTotalQuantity() - coupon.getIssuedQuantity();
        cacheEvictionService.evictCouponListIfSoldOut(remainingQuantity);
    }

}
