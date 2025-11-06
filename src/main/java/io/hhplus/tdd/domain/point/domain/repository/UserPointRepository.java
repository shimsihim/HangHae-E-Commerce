package io.hhplus.tdd.domain.point.domain.repository;

import io.hhplus.tdd.domain.point.domain.model.UserPoint;

import java.util.Optional;

public interface UserPointRepository {
    Optional<UserPoint> findByUserId(long userId);
    UserPoint save(UserPoint userPoint);
}
