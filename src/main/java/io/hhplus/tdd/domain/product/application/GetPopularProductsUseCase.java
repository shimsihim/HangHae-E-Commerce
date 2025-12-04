package io.hhplus.tdd.domain.product.application;

import io.hhplus.tdd.common.cache.RedisKey;
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

    // 전체 결과는 10분 캐시 (스케줄러가 주기적으로 갱신)
    // - sync=true로 Cache Stampede 방지 (동시 요청 시 첫 요청만 실행)
    @Transactional(readOnly = true)
    @Cacheable(value = RedisKey.POPULAR_PRODUCTS_NAME, key = RedisKey.POPULAR_PRODUCTS_KEY, sync = true)
    public List<Output> execute() {
        return getPopularProducts();
    }

    // 스케줄러에서 호출 - 캐시 워밍업
    // @CachePut은 항상 메서드를 실행하고 결과를 캐시에 저장
    @Transactional(readOnly = true)
    @CachePut(value = RedisKey.POPULAR_PRODUCTS_NAME, key = RedisKey.POPULAR_PRODUCTS_KEY)
    public List<Output> refreshCache() {
        return getPopularProducts();
    }

    // 실제 비즈니스 로직 - execute()와 refreshCache()에서 공통으로 사용
    private List<Output> getPopularProducts() {
        // 1. Redis에서 인기 상품 ID 목록 조회 (스코어 기반 정렬됨)
        List<Long> popularProductIds = rankingService.getDailyTopRankIds(500);

        // 2. ProductService를 통해 상품 정보 조회 (캐시 우선, MGET 활용)
        List<Product> products = productService.getProductsByIds(popularProductIds);

        return products.stream()
                .map(Output::from)
                .toList();
    }
}
