package io.hhplus.tdd.domain.product.domain.service;

import io.hhplus.tdd.common.cache.CacheNames;
import io.hhplus.tdd.common.cache.MultiGetCacheService;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.model.ProductCache;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 상품 도메인 서비스
 * - 상품 조회 시 캐시 전략 적용
 * - 여러 UseCase에서 재사용 가능한 공통 로직 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final MultiGetCacheService multiGetCacheService;

    private static final Duration PRODUCT_CACHE_TTL = Duration.ofHours(1);

    /**
     * 여러 상품을 ID로 조회 (캐시 우선, 미스 시 DB 조회)
     * - 인기 상품, 상품 리스트 등 대량 조회 시 사용
     * - 순서 보장 (입력 ID 순서대로 반환)
     *
     * @param ids 조회할 상품 ID 리스트
     * @return 상품 리스트 (존재하지 않는 ID는 제외됨)
     */
    public List<Product> getProductsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 캐시에서 MGET으로 조회
        Map<Long, ProductCache> cachedMap = multiGetCacheService.getMap(
                CacheNames.PRODUCT_PREFIX,
                ids,
                ProductCache.class
        );

        // 2. 캐시 미스된 ID 추출
        List<Long> missingIds = ids.stream()
                .filter(id -> !cachedMap.containsKey(id))
                .toList();

        // 3. DB에서 조회 후 캐시에 저장
        if (!missingIds.isEmpty()) {
            log.debug("Cache miss for product ids: {}", missingIds);

            List<Product> dbProducts = productRepository.findAllById(missingIds);
            Map<Long, ProductCache> dbMap = dbProducts.stream()
                    .collect(Collectors.toMap(
                            Product::getId,
                            ProductCache::from
                    ));

            // 캐시에 저장
            multiGetCacheService.putAll(
                    CacheNames.PRODUCT_PREFIX,
                    dbMap,
                    PRODUCT_CACHE_TTL
            );

            // 결과 맵에 추가
            cachedMap.putAll(dbMap);
        }

        // 4. 입력 순서 유지하며 반환
        return ids.stream()
                .map(cachedMap::get)
                .filter(Objects::nonNull)
                .map(ProductCache::toProduct)
                .toList();
    }

    /**
     * 상품 정보가 업데이트될 때 캐시 무효화
     *
     * @param productIds 무효화할 상품 ID 리스트
     */
    public void invalidateCache(List<Long> productIds) {
        multiGetCacheService.deleteByIds(CacheNames.PRODUCT_PREFIX, productIds);
        log.info("Invalidated product cache for ids: {}", productIds);
    }
}
