package io.hhplus.tdd.domain.order.infrastructure.repository;

import io.hhplus.tdd.domain.order.database.ProductTable;
import io.hhplus.tdd.domain.order.domain.model.OrderItem;
import io.hhplus.tdd.domain.order.domain.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@RequiredArgsConstructor
public class OrderItemRepositoryImpl implements OrderItemRepository {

    private final Map<Long, OrderItem> table = new ConcurrentHashMap<>();

    @Override
    public List<OrderItem> findByOrderId(long orderId) {
        return List.of();
    }
}
