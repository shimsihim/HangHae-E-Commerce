package io.hhplus.tdd.user.dto.response;

import io.hhplus.tdd.pointHistory.domain.TransactionType;
import io.hhplus.tdd.user.domain.User;

public record UserPointTransactionDTO(

        long amount,
        long balanceAfter,
        TransactionType type
) {
}