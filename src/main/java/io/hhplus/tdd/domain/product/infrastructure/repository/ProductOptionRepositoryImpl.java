package io.hhplus.tdd.domain.product.infrastructure.repository;

import io.hhplus.tdd.domain.product.database.ProductOptionTable;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.domain.repository.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductOptionRepositoryImpl implements ProductOptionRepository {

    private final ProductOptionTable productOptionTable;

    @Override
    public List<ProductOption> findByProductId(long productId) {
        return productOptionTable.selectAll().stream()
                .filter(option -> option.getProductId().equals(productId))
                .toList();
    }

    @Override
    public Optional<ProductOption> findById(long productOptionId) {
        ProductOption productOption = productOptionTable.selectById(productOptionId);
        return Optional.ofNullable(productOption);
    }

    @Override
    public ProductOption save(ProductOption productOption) {
        return productOptionTable.save(productOption);
    }
}
