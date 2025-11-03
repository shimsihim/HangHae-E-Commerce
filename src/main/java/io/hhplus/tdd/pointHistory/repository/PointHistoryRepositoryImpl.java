package io.hhplus.tdd.pointHistory.repository;

import io.hhplus.tdd.pointHistory.database.PointHistoryTable;
import io.hhplus.tdd.pointHistory.domain.PointHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PointHistoryRepositoryImpl implements  PointHistoryRepository {

    private final PointHistoryTable pointHistoryTable;

    @Override
    public List<PointHistory> getHistoryByUserId(Long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    @Override
    public PointHistory addHistory(PointHistory pointHistory) {
        return pointHistoryTable.insert(pointHistory);
    }

}
