package io.hhplus.tdd.domain.point.domain.repository;

import io.hhplus.tdd.domain.point.domain.model.PointHistory;

import java.util.List;

public interface PointHistoryRepository {
    List<PointHistory>findByUserId(long userId);
    PointHistory save(PointHistory pointHistory);
}
