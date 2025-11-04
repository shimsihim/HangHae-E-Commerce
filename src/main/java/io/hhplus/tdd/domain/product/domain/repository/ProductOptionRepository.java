package io.hhplus.tdd.domain.product.domain.repository;

import io.hhplus.tdd.domain.product.domain.model.ProductOption;

import java.util.List;
import java.util.Optional;

public interface ProductOptionRepository {
    List<ProductOption> findByProductId(long ProductId);
}
