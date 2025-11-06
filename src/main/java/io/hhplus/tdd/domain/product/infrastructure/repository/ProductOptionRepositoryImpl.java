package io.hhplus.tdd.domain.product.infrastructure.repository;

import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.domain.repository.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@RequiredArgsConstructor
public class ProductOptionRepositoryImpl implements ProductOptionRepository {

    private final Map<Long, ProductOption> table = new ConcurrentHashMap<>();
    private AtomicLong cursor = new AtomicLong(0);

    @Override
    public List<ProductOption> findByProductId(long productId) {
        return table.values().stream()
                .filter(option -> option.getProductId().equals(productId))
                .toList();
    }

    @Override
    public Optional<ProductOption> findById(long productOptionId) {
        ProductOption productOption = table.get(productOptionId);
        return Optional.ofNullable(productOption);
    }

    @Override
    public ProductOption save(ProductOption productOption) {
        if (productOption.getId() == null) {
            productOption.setId(cursor.incrementAndGet());
        }
        return table.put(productOption.getId(), productOption);
    }
}
