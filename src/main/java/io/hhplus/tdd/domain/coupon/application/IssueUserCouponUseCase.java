package io.hhplus.tdd.domain.coupon.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.lock.LockAnn;
import io.hhplus.tdd.common.lock.LockId;
import io.hhplus.tdd.common.lock.LockKey;
import io.hhplus.tdd.domain.coupon.domain.CouponValidator;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.domain.repository.CouponRepository;
import io.hhplus.tdd.domain.coupon.domain.repository.UserCouponRepository;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IssueUserCouponUseCase {

    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;
    private final CouponValidator couponValidator;

    public record Input(
            @LockId long couponId,
            long userId
    ){}

    @LockAnn(lockKey = LockKey.COUPON)
    public void handle(Input input){
        Coupon coupon = couponRepository.findById(input.couponId()).orElseThrow(()-> new CouponException(ErrorCode.COUPON_NOT_FOUND , input.couponId()));

        //검증
        couponValidator.valid(coupon);
        List<UserCoupon> userCouponList = userCouponRepository.findByUserIdAndCouponId(input.userId(), input.couponId());
        if(userCouponList.size() >= coupon.getLimitPerUser()){
            throw new CouponException(ErrorCode.COUPON_ISSUE_LIMIT_PER_USER , input.couponId());
        }

        // 쿠폰 발급 수량 증가 (동시성 제어 필요)
        coupon.increaseIssuedQuantity();
        couponRepository.save(coupon);

        UserCoupon userCoupon = UserCoupon.from(input.userId() , coupon);
        userCoupon = userCouponRepository.save(userCoupon);
    }

}
