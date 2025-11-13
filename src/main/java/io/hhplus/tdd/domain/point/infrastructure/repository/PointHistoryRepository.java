package io.hhplus.tdd.domain.point.infrastructure.repository;

import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory,Long> {
    List<PointHistory> findByUserId(long userId);
}
