package io.hhplus.tdd.domain.product.infrastructure.repository;

import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductOptionRepository extends JpaRepository<ProductOption,Long> {
    List<ProductOption> findByProductId(Long productId);

    @Query("SELECT DISTINCT po FROM ProductOption po WHERE po.id IN :optionIds")
    List<ProductOption> findAllWithProductByIdIn(@Param("optionIds") List<Long> optionIds);
}
