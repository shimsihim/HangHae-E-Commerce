package io.hhplus.tdd.domain.product.presentation.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ProductDetailReqDTO(
        @Positive(message = "사용자 ID는 양수여야 합니다.")
        long userId,
        @Positive(message = "사용 금액은 양수여야 합니다.")
        long amount,
        @NotBlank(message = "설명은 필수입니다.")
        String description
) {
}
