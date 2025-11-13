package io.hhplus.tdd.domain.coupon.presentation.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CouponIssueReqDTO(
        @Positive @NotNull
        Long userId,
        @Positive @NotNull
        Long couponId
) {
}
