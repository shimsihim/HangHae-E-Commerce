package io.hhplus.tdd.domain.product.infrastructure.repository;

import io.hhplus.tdd.domain.product.domain.model.Product;
import lombok.Getter;

public record ProductSalesDto(
        Product product,
        long totalSalesQuantity
) {

    static ProductSalesDto from(Product p , long totalSalesQuantity) {
        return new  ProductSalesDto(p, totalSalesQuantity);
    }
}
