package io.hhplus.tdd.domain.product.infrastructure.repository;

import io.hhplus.tdd.domain.point.database.PointHistoryTable;
import io.hhplus.tdd.domain.product.database.ProductTable;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductTable productTable;

    @Override
    public Optional<Product> findById(long productId) {
        return Optional.of(productTable.selectById(productId));
    }

    @Override
    public List<Product> findAll() {
        return productTable.selectAll();
    }

}
