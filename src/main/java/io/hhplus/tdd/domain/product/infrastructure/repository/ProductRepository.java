package io.hhplus.tdd.domain.product.infrastructure.repository;

import io.hhplus.tdd.domain.product.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    Optional<Product> findById(long productId);
    List<Product> findAll();



    @Query("SELECT new io.hhplus.tdd.domain.product.infrastructure.repository.ProductSalesDto(p, SUM(oi.quantity)) " +
            "FROM Product p, OrderItem oi " +
            "WHERE oi.productId = p.id " +
            "  AND oi.createdAt >= :threeDaysAgo " + // 파라미터 사용
            "GROUP BY p.id ORDER BY SUM(oi.quantity) DESC")
    List<ProductSalesDto> findPopular(@Param("threeDaysAgo") LocalDateTime threeDaysAgo);
}
