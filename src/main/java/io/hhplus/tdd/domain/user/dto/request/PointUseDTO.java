package io.hhplus.tdd.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

@Schema(description = "포인트 사용 요청")
public record PointUseDTO(
        @Positive(message = "포인트 사용은 양수만 가능합니다.")
        long amount
) {
}
