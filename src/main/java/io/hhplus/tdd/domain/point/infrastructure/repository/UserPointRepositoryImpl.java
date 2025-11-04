package io.hhplus.tdd.domain.point.infrastructure.repository;

import io.hhplus.tdd.domain.point.database.UserPointTable;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.domain.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserPointRepositoryImpl implements UserPointRepository {

    private final UserPointTable userPointTable;

    @Override
    public Optional<UserPoint> findByUserId(long userId) {
        UserPoint userPoint = userPointTable.selectById(userId);
        return Optional.of(userPoint);
    }

    @Override
    public UserPoint save(UserPoint userPoint) {
        if (userPoint.getId() == null) {
            throw new IllegalArgumentException("UserPoint id cannot be null");
        }

        UserPoint existingPoint = userPointTable.selectById(userPoint.getId());
        if (existingPoint == null) {
            return userPointTable.insertOrUpdate(userPoint.getId(), userPoint.getBalance());
        }

        return userPointTable.update(userPoint);
    }
}
