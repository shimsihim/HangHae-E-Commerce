package io.hhplus.tdd.user.repository;

import io.hhplus.tdd.user.domain.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> selectById(long userId);
    User updateUserBalance(User user);
}