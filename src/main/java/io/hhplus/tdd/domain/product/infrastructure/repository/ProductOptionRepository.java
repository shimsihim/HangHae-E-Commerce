package io.hhplus.tdd.domain.product.infrastructure.repository;

import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductOptionRepository extends JpaRepository<ProductOption,Long> {
    List<ProductOption> findByProductId(Long productId);

    @Query("SELECT DISTINCT po FROM ProductOption po JOIN FETCH po.product WHERE po.id IN :optionIds")
    List<ProductOption> findAllWithProductByIdIn(@Param("optionIds") List<Long> optionIds);

    @Query("SELECT po FROM ProductOption po JOIN FETCH po.product WHERE po.id IN :optionIds")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ProductOption> findAllByIdInForUpdate(@Param("optionIds") List<Long> optionIds);

}
