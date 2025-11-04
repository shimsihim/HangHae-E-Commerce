package io.hhplus.tdd.domain.user.dto.response;

import io.hhplus.tdd.domain.user.domain.User;

public record UserPointDTO (
            long balance
    ) {
    public static UserPointDTO from(User user){
        return new UserPointDTO(user.getBalance());
    }
}