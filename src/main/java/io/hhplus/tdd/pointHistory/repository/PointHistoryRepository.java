package io.hhplus.tdd.pointHistory.repository;

import io.hhplus.tdd.pointHistory.domain.PointHistory;

import java.util.List;

public interface PointHistoryRepository {
    List<PointHistory> getHistoryByUserId(Long userId);
    PointHistory addHistory(PointHistory pointHistory);
}
