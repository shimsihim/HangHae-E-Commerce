package io.hhplus.tdd.domain.coupon.presentation.dto.res;//package io.hhplus.tdd.domain.cart.presentation.dto.res;

import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.DiscountType;
import lombok.Builder;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
public record CouponResDTO(
    Long id,
    String couponName,
    String discountType,
    int discountValue,
    int totalQuantity,
    int issuedQuantity,
    int limitPerUser,
    int duration,
    int minOrderValue,
    LocalDateTime validFrom,
    LocalDateTime validUntil
) {
    public static CouponResDTO of(Long id,
                                  String couponName,
                                  String discountType,
                                  int discountValue,
                                  int totalQuantity,
                                  int issuedQuantity,
                                  int limitPerUser,
                                  int duration,
                                  int minOrderValue,
                                  LocalDateTime validFrom,
                                  LocalDateTime validUntil){
        return CouponResDTO.builder()
                .id(id)
                .couponName(couponName)
                .discountType(discountType)
                .discountValue(discountValue)
                .totalQuantity(totalQuantity)
                .issuedQuantity(issuedQuantity)
                .limitPerUser(limitPerUser)
                .duration(duration)
                .minOrderValue(minOrderValue)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .build();
    }
}
