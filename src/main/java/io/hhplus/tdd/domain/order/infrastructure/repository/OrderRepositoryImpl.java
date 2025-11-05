package io.hhplus.tdd.domain.order.infrastructure.repository;

import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final Map<Long, Order> table = new ConcurrentHashMap<>();

    @Override
    public Optional<Order> findById(long orderId) {
        return Optional.of(table.get(orderId));
    }

    @Override
    public List<Order> findByUserId(long userId) {
        return table.values().stream().filter(order -> order.getUserId().equals(userId)).toList();
    }

    @Override
    public List<Order> findAll() {
        return table.values().stream().toList();
    }
}
