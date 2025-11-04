package io.hhplus.tdd.domain.point.infrastructure.repository;

import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import io.hhplus.tdd.domain.point.domain.repository.PointHistoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PointHistoryRepositoryImpl implements PointHistoryRepository {

    @Override
    public List<PointHistory> findByUserId(long userId) {
        throw new RuntimeException();
    }

    @Override
    public PointHistory save(PointHistory pointHistory) {
        return null;
    }
}
