package io.hhplus.tdd.domain.point.infrastructure.repository;

import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPointRepository extends JpaRepository<UserPoint,Long> {
}
