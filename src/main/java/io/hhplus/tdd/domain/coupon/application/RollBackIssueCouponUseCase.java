package io.hhplus.tdd.domain.coupon.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.lock.LockAnn;
import io.hhplus.tdd.common.lock.LockId;
import io.hhplus.tdd.common.lock.LockKey;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.Status;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.domain.repository.CouponRepository;
import io.hhplus.tdd.domain.coupon.domain.repository.UserCouponRepository;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 쿠폰 발급 롤백 UseCase
 * 결제 실패 시 발급된 쿠폰을 회수하고 발급 수량을 감소시킵니다.
 */
@Service
@RequiredArgsConstructor
public class RollBackIssueCouponUseCase {

    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;

    public record Input(
            @LockId long couponId,
            long userId,
            long userCouponId
    ){}

    @LockAnn(lockKey = LockKey.COUPON)
    public void handle(Input input){
        // UserCoupon 삭제 또는 상태 변경
        UserCoupon userCoupon = userCouponRepository.findByUserIdAndUserCouponIdAndStatus(input.userId(), input.userCouponId(), Status.ISSUED.toString())
                .orElseThrow(()-> new CouponException(ErrorCode.COUPON_NOT_FOUND , input.userCouponId()));

        // 쿠폰 발급 수량 감소
        Coupon coupon = couponRepository.findById(input.couponId())
                .orElseThrow(()-> new CouponException(ErrorCode.COUPON_NOT_FOUND , input.couponId()));

        coupon.decreaseIssuedQuantity();
        couponRepository.save(coupon);

        // UserCoupon 삭제 (인메모리에서는 상태 변경보다 삭제가 적절)
        // 또는 CANCELLED 같은 상태 추가 가능
        userCoupon.rollbackUseCoupon(); // 임시로 상태만 변경
    }
}
