package io.hhplus.tdd.domain.user.dto.response;

import io.hhplus.tdd.domain.point.domain.model.TransactionType;

public record UserPointTransactionDTO(

        long amount,
        long balanceAfter,
        TransactionType type
) {
}