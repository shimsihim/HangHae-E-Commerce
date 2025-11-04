package io.hhplus.tdd.domain.point.presentation.dto.req;

public record UserPointUseReqDTO(
        long userId,
        long amount,
        String description
) {
}
