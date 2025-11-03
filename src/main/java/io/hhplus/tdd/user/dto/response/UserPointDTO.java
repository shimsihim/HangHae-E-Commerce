package io.hhplus.tdd.user.dto.response;

import io.hhplus.tdd.pointHistory.domain.TransactionType;
import io.hhplus.tdd.user.domain.User;

public record UserPointDTO (
            long balance
    ) {
    public static UserPointDTO from(User user){
        return new UserPointDTO(user.getBalance());
    }
}