package io.hhplus.tdd.domain.order.infrastructure.repository;

import io.hhplus.tdd.domain.order.domain.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(long orderId);
}
