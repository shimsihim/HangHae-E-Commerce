package io.hhplus.tdd.domain.product.infrastructure.repository;

import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final Map<Long, Product> table = new ConcurrentHashMap<>();
    private AtomicLong cursor = new AtomicLong(0);

    @Override
    public Optional<Product> findById(long productId) {
        return Optional.of(table.get(productId));
    }

    @Override
    public List<Product> findAll() {
        return table.values().stream().toList();
    }

}
