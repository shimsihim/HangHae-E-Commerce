package io.hhplus.tdd.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * 캐시 무효화를 담당하는 서비스
 * 재고 변동 시 적절한 캐시를 무효화하여 데이터 일관성 유지
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheEvictionService {

    private final CacheManager cacheManager;

    /**
     * 재고 부족 임계값
     * 재고가 이 값 미만으로 떨어지면 캐시 무효화
     */
    private static final long LOW_STOCK_THRESHOLD = 10L;

    // 재고가 임계값 미만이면 해당 상품 캐시를 무효화
    public void evictIfLowStock(Long productId, Long currentStock) {
        if (currentStock < LOW_STOCK_THRESHOLD) {
            Cache cache = cacheManager.getCache(CacheNames.PRODUCT_DETAIL);
            if (cache != null) {
                String cacheKey = "id:" + productId;
                cache.evict(cacheKey);
                log.info("상품 상세 캐시 무효화: productId={}", productId);
            }
        }
    }


    //쿠폰 재고가 0이 되면 쿠폰 목록 캐시를 무효화
    public void evictCouponListIfSoldOut(int remainingQuantity) {
        if (remainingQuantity <= 0) {
            Cache cache = cacheManager.getCache(CacheNames.COUPON_LIST);
            if (cache != null) {
                cache.evict("all");
                log.info("쿠폰 목록 캐시 무효화");
            }
            log.info("쿠폰 품절로 인한 캐시 무효화: 남은 수량={}", remainingQuantity);
        }
    }
}
