package io.hhplus.tdd.domain.point.infrastructure.repository;

import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import io.hhplus.tdd.domain.point.domain.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@RequiredArgsConstructor
public class PointHistoryRepositoryImpl implements PointHistoryRepository {

    private final List<PointHistory> table = new ArrayList<>();
    private AtomicLong cursor = new AtomicLong(0);

    @Override
    public List<PointHistory> findByUserId(long userId) {
        return table.stream().filter(pointHistory -> pointHistory.getUserId() == userId).toList();
    }

    @Override
    public PointHistory save(PointHistory pointHistory) {

        if (pointHistory.getId() == null) {
            long nextCursor = cursor.addAndGet(1);
            pointHistory.setPointHistoryId(nextCursor);
        }
        table.add(pointHistory);
        return pointHistory;
    }
}
