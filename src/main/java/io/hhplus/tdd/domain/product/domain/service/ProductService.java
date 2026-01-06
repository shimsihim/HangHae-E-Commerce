package io.hhplus.tdd.domain.product.domain.service;

import io.hhplus.tdd.common.cache.MultiGetCacheService;
import io.hhplus.tdd.common.cache.RedisKey;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.model.ProductCache;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final MultiGetCacheService multiGetCacheService;

    public List<Product> getProductsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        // 캐시에서  조회
        Map<Long, ProductCache> cachedMap = multiGetCacheService.getMap(
                RedisKey.CACHE_PRODUCT_BY_ID,
                ids,
                ProductCache.class
        );

        // 캐시 미스된 ID
        List<Long> missingIds = ids.stream()
                .filter(id -> !cachedMap.containsKey(id))
                .toList();

        // DB 조회 후 캐시에 저장
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
                    RedisKey.CACHE_PRODUCT_BY_ID,
                    dbMap
            );

            cachedMap.putAll(dbMap);
        }

        return ids.stream()
                .map(cachedMap::get)
                .filter(Objects::nonNull)
                .map(ProductCache::toProduct)
                .toList();
    }

    // 상품 정보가 업데이트될 때 캐시 무효화
    public void invalidateCache(List<Long> productIds) {
        multiGetCacheService.deleteByIds(RedisKey.CACHE_PRODUCT_BY_ID, productIds);
        log.info("Invalidated product cache for ids: {}", productIds);
    }
}
