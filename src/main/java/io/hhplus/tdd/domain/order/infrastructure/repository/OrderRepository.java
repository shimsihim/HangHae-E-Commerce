package io.hhplus.tdd.domain.order.infrastructure.repository;

import io.hhplus.tdd.domain.order.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order,Long> {
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.userId = :userId")
    List<Order> findAllWithItemsByUserId(@Param("userId") Long userId);
}
