package io.hhplus.tdd.domain.coupon.domain.model;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCoupon {
    @Setter
    private Long id;
    private Long userId;
    private Long couponId;
    private Status status;
    private LocalDateTime usedAt;
    private LocalDateTime expiredAt;

    public static UserCoupon from(long userId , Coupon coupon){
        return UserCoupon.builder()
                .userId(userId)
                .couponId(coupon.getId())
                .status(Status.ISSUED)
                .expiredAt(LocalDateTime.now().plusDays(coupon.getDuration()))
                .build();
    }

    public void useCoupon(){
        if (LocalDateTime.now().isAfter(this.getExpiredAt()) || this.status.equals(Status.EXPIRED)) {
            throw new CouponException(ErrorCode.COUPON_USER_EXPIRED, this.getId());
        }
        if (this.status.equals(Status.USED)) {
            throw new CouponException(ErrorCode.COUPON_USER_USED, this.getId());
        }
        this.status = Status.USED;
    }

    public void rollbackUseCoupon(){
        this.status = Status.ISSUED;
    }
}
