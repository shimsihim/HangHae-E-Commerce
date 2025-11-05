package io.hhplus.tdd.domain.order.domain.repository;

import io.hhplus.tdd.domain.order.domain.model.OrderItem;

import java.util.List;

public interface OrderItemRepository {
    List<OrderItem> findByOrderId(long orderId);
    OrderItem save(OrderItem  orderItem);
}
