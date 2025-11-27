package io.hhplus.tdd.config;

import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 기반 캐시 설정
 * - Spring Cache Abstraction (@Cacheable, @CacheEvict 등) 활성화
 * - 스케줄러 활성화 (캐시 워밍업 등)
 * - 각 캐시별 TTL 설정
 */
@Configuration
@EnableCaching
@EnableScheduling
public class RedisCacheConfig {

    /**
     * Redisson 기반 CacheManager 구성
     * 각 캐시 유형별로 서로 다른 TTL 설정
     */
    @Bean
    public CacheManager cacheManager(RedissonClient redissonClient) {
        Map<String, CacheConfig> config = new HashMap<>();

        // 상품 리스트: 1페이지만 캐싱, TTL 5분
        config.put("productList", new CacheConfig(
                Duration.ofMinutes(10).toMillis(),
                Duration.ofMinutes(10).toMillis()
        ));

        // 인기 상품: 전체 캐싱, TTL 24시간 (매일 12시 워밍업) , + 2분으로 캐시 재로드 사이 Cache Stampede방지
        config.put("popularProducts", new CacheConfig(
                Duration.ofDays(1).plusMinutes(2).toMillis(),
                Duration.ofDays(1).plusMinutes(2).toMillis()
        ));

        // 상품 상세: 재고 10개 이상일 때만 캐싱, TTL 5분
        config.put("productDetail", new CacheConfig(
                Duration.ofMinutes(10).toMillis(),
                Duration.ofMinutes(10).toMillis()
        ));

        // 쿠폰 목록: TTL 3분
        config.put("couponList", new CacheConfig(
                Duration.ofMinutes(20).toMillis(),
                Duration.ofMinutes(20).toMillis()
        ));

        return new RedissonSpringCacheManager(redissonClient, config);
    }
}
