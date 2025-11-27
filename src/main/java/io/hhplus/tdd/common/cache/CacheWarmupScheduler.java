package io.hhplus.tdd.common.cache;

import io.hhplus.tdd.domain.product.application.GetPopularProductsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 캐시 워밍업 스케줄러
 * 주기적으로 인기 상품 캐시를 사전 로드하여 DB 부하 감소
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmupScheduler {

    private final GetPopularProductsUseCase getPopularProductsUseCase;
    private final CacheManager cacheManager;

    /**
     * 매일 12시 정각에 인기 상품 캐시를 워밍업
     * - 기존 캐시를 먼저 삭제
     * - DB에서 최신 인기 상품 데이터를 조회하여 캐시에 저장
     * - 12:00에 사용자 요청이 DB로 몰리는 것을 방지
     */
    // 매일 12시에 실행
    @Scheduled(cron = "0 0 12 * * *")
    public void warmupPopularProductsCache() {
        try {
            // @CachePut이 붙은 메서드 호출
            // 1. DB 조회 수행 (이 동안에도 사용자는 기존 캐시 데이터를 봄)
            // 2. 메서드 리턴과 동시에 Redis에 새로운 값으로 '덮어쓰기' 됨
            var result = getPopularProductsUseCase.refreshCache();

        } catch (Exception e) {
            // DB 조회가 실패해도 기존 캐시(TTL 여유분)가 살아있어서 서비스 장애 방지 가능
            log.error("인기 상품 캐시 갱신 실패 (기존 캐시 유지됨)", e);
        }
    }
}
