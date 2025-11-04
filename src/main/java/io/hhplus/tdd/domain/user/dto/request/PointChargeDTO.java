package io.hhplus.tdd.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

@Schema(description = "포인트 충전 요청")
public record PointChargeDTO(
        @Schema(
                description = "충전할 포인트 금액",
                example = "10000",
                minimum = "1000",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @Positive(message = "포인트 충전은 양수만 가능합니다.")
        long amount
) {
}
