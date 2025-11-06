package io.hhplus.tdd.domain.point.infrastructure.repository;

import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.domain.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@RequiredArgsConstructor
public class UserPointRepositoryImpl implements UserPointRepository {

    private final Map<Long, UserPoint> table = new ConcurrentHashMap<>();
    private AtomicLong cursor = new AtomicLong(0);


    @Override
    public Optional<UserPoint> findByUserId(long userId) {
        UserPoint userPoint = table.get(userId);
        return Optional.of(userPoint);
    }

    @Override
    public UserPoint save(UserPoint userPoint) {
        Long id = userPoint.getId();
        if (userPoint.getId() == null) {
            id = cursor.incrementAndGet();
            userPoint.setId(id);
        }
        table.put(id, userPoint);
        return userPoint;
    }
}
