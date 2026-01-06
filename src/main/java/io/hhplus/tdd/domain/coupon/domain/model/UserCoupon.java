package io.hhplus.tdd.domain.coupon.domain.model;

import io.hhplus.tdd.common.baseEntity.CreatedBaseEntity;
import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
public class UserCoupon extends CreatedBaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @JoinColumn(name = "coupon_id", insertable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Coupon coupon;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDate usedAt;

    @Column(nullable = false)
    private LocalDate expiredAt;

    @Version
    private Long version;

    public static UserCoupon from(long userId , Coupon coupon){
        return UserCoupon.builder()
                .userId(userId)
                .couponId(coupon.getId())
                .coupon(coupon)
                .status(Status.ISSUED)
                .expiredAt(LocalDate.now().plusDays(coupon.getDuration()))
                .build();
    }

    public void useCoupon(){
        validUseCoupon();
        this.status = Status.USED;
        this.usedAt = LocalDate.now();
    }

    public void restoreCoupon(){
        this.status = Status.ISSUED;
    }

    public void validUseCoupon(){
        if (LocalDate.now().isAfter(this.getExpiredAt()) || this.status.equals(Status.EXPIRED)) {
            throw new CouponException(ErrorCode.COUPON_USER_EXPIRED, this.getId());
        }
        if (this.status.equals(Status.USED)) {
            throw new CouponException(ErrorCode.COUPON_USER_USED, this.getId());
        }
    }
}
