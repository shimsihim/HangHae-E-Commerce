package io.hhplus.tdd.domain.product.domain.repository;

import io.hhplus.tdd.domain.product.database.ProductTable;
import io.hhplus.tdd.domain.product.domain.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(long productId);
    List<Product> findAll();
}
