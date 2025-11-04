package io.hhplus.tdd.domain.coupon.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.coupon.domain.model.Status;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.domain.repository.UserCouponRepository;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RollBackUseUserCouponUseCase {
    private final UserCouponRepository userCouponRepository;

    public record Input(
        long userId,
        long userCouponId
    ){}

    public void handle(Input input){
        UserCoupon userCoupon = userCouponRepository.findByUserIdAndUserCouponIdAndStatus(input.userId(), input.userCouponId() , Status.USED.toString())
                .orElseThrow(()-> new CouponException(ErrorCode.COUPON_NOT_FOUND , input.userCouponId));

        if(LocalDateTime.now().isAfter(userCoupon.getExpiredAt())){
            throw new CouponException(ErrorCode.COUPON_USER_EXPIRED , input.userCouponId);
        }

        userCoupon.rollbackUseCoupon();
        userCouponRepository.save(userCoupon);
    }
}
