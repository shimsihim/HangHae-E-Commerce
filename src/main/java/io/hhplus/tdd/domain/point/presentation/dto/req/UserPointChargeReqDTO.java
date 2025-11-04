package io.hhplus.tdd.domain.point.presentation.dto.req;

public record UserPointChargeReqDTO(
        long userId,
        long amount,
        String description
) {
}
