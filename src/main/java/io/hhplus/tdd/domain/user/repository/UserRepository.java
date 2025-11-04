package io.hhplus.tdd.domain.user.repository;

import io.hhplus.tdd.domain.user.domain.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> selectById(long userId);
    User updateUserBalance(User user);
}