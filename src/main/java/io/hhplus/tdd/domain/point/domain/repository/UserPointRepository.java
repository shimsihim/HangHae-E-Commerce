package io.hhplus.tdd.domain.point.domain.repository;

import io.hhplus.tdd.domain.point.domain.model.UserPoint;

public interface UserPointRepository {
    UserPoint findByUserId(long userId);
    UserPoint save(UserPoint userPoint);
}
