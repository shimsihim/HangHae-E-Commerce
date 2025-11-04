package io.hhplus.tdd.domain.product.infrastructure.repository;

import io.hhplus.tdd.domain.point.database.UserPointTable;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.domain.repository.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductOptionRepositoryImpl implements ProductOptionRepository {

    private final UserPointTable userPointTable;

}
