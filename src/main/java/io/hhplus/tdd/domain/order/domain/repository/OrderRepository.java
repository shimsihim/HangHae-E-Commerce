package io.hhplus.tdd.domain.order.domain.repository;

import io.hhplus.tdd.domain.order.domain.model.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Optional<Order> findById(long orderId);
    List<Order> findByUserId(long userId);
    List<Order> findAll();
}
