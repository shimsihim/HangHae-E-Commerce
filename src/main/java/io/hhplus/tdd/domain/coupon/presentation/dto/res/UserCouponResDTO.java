package io.hhplus.tdd.domain.coupon.presentation.dto.res;//package io.hhplus.tdd.domain.cart.presentation.dto.res;

import io.hhplus.tdd.domain.coupon.application.GetUserCouponListQuery;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UserCouponResDTO(
    long id,
    long couponId,
    String couponName,
    String discountType,
    int discountValue,
    int duration,
    int minOrderValue,
    String userCouponStatus,
    LocalDateTime usedAt,
    LocalDateTime expiredAt
) {
    public static UserCouponResDTO from(GetUserCouponListQuery.Output out){
        return UserCouponResDTO.builder()
                .id(out.id())
                .couponId(out.couponId())
                .couponName(out.couponName())
                .discountType(out.discountType())
                .discountValue(out.discountValue())
                .duration(out.duration())
                .minOrderValue(out.minOrderValue())
                .userCouponStatus(out.userCouponStatus())
                .usedAt(out.usedAt())
                .expiredAt(out.expiredAt())
                .build();
    }
}
