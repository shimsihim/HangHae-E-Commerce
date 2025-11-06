package io.hhplus.tdd.domain.coupon.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.lock.LockAnn;
import io.hhplus.tdd.common.lock.LockId;
import io.hhplus.tdd.common.lock.LockKey;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.domain.repository.CouponRepository;
import io.hhplus.tdd.domain.coupon.domain.repository.UserCouponRepository;
import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class IssueUserCouponUseCase {

    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;
    private final CouponService couponService;

    public record Input(
            @LockId long couponId,
            long userId
    ){}

    @LockAnn(lockKey = LockKey.COUPON)
    public void execute(Input input){
        Coupon coupon = couponRepository.findById(input.couponId())
                .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND, input.couponId()));

        // 롤백용
        int originalIssuedQuantity = coupon.getIssuedQuantity();

        List<UserCoupon> userIssuedCoupons = userCouponRepository.findByUserIdAndCouponId(input.userId(), input.couponId());

        try {
            UserCoupon userCoupon = couponService.issueCoupon(coupon, input.userId(), userIssuedCoupons);
            couponRepository.save(coupon);
            userCouponRepository.save(userCoupon);
        } catch (Exception e) {
            while (coupon.getIssuedQuantity() > originalIssuedQuantity) {
                coupon.decreaseIssuedQuantity();
            }
            couponRepository.save(coupon);
            throw e;
        }
    }

}
