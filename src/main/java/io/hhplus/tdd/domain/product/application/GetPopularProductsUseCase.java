package io.hhplus.tdd.domain.product.application;

import io.hhplus.tdd.common.cache.CacheNames;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.service.ProductService;
import io.hhplus.tdd.domain.product.domain.service.RankingService;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductRepository;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductSalesDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetPopularProductsUseCase {

    private final ProductRepository productRepository;
    private final RankingService rankingService;
    private final ProductService productService;

    public record Output(
            Long productId,
            String name,
            String description,
            Long basePrice
    ) {
        public static Output from(Product product) {
            return new Output(
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getBasePrice()
            );
        }
    }

    // 전체 결과는 10분 캐시 (스케줄러가 8분마다 갱신)
    // - sync=true로 Cache Stampede 방지 (동시 요청 시 첫 요청만 실행)
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.POPULAR_PRODUCTS_LIST, key = "'all'", sync = true)
    public List<Output> execute() {
        // 1. Redis에서 인기 상품 ID 목록 조회 (스코어 기반 정렬됨)
        List<Long> popularProductIds = rankingService.getDailyTopRankIds(500);

        // 2. ProductService를 통해 상품 정보 조회 (캐시 우선, MGET 활용)
        List<Product> products = productService.getProductsByIds(popularProductIds);

        return products.stream()
                .map(product -> Output.from(
                        product
                ))
                .toList();
    }

    // 스케줄러에서 호출 - 캐시 워밍업
    @Transactional(readOnly = true)
    @CachePut(value = CacheNames.POPULAR_PRODUCTS_LIST, key = "'all'")
    public List<Output> refreshCache() {
        return execute();
    }
}
