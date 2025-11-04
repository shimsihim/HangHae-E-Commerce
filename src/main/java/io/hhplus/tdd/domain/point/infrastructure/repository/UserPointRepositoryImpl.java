package io.hhplus.tdd.domain.point.infrastructure.repository;

import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.domain.repository.UserPointRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserPointRepositoryImpl implements UserPointRepository {

    @Override
    public UserPoint findByUserId(long userId) {
        return null;
    }

    @Override
    public UserPoint save(UserPoint userPoint) {
        return null;
    }
}
