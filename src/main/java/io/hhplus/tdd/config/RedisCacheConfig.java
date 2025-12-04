package io.hhplus.tdd.config;

import io.hhplus.tdd.common.cache.CacheNames;
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

@Configuration
@EnableCaching
@EnableScheduling
public class RedisCacheConfig {

    /**
     * [핵심] 캐시 설정(TTL)을 별도의 Bean으로 분리
     * 이제 다른 서비스(MultiGetCacheService 등)에서 이 Bean을 주입받아 TTL을 알 수 있습니다.
     */
    @Bean
    public Map<String, CacheConfig> redisCacheConfiguration() {
        Map<String, CacheConfig> config = new HashMap<>();

        // 1. 상품 리스트: 10분
        config.put(CacheNames.PRODUCT_LIST_ONE_PAGE, new CacheConfig(
                Duration.ofMinutes(10).toMillis(),
                Duration.ofMinutes(10).toMillis()
        ));

        // 2. 인기 상품: 10분
        config.put(CacheNames.POPULAR_PRODUCTS_LIST, new CacheConfig(
                Duration.ofMinutes(10).toMillis(),
                Duration.ofMinutes(10).toMillis()
        ));

        // 3. 상품 상세: 10분
        config.put(CacheNames.PRODUCT_DETAIL, new CacheConfig(
                Duration.ofMinutes(10).toMillis(),
                Duration.ofMinutes(10).toMillis()
        ));

        // 4. 쿠폰 목록: 20분
        config.put(CacheNames.COUPON_LIST, new CacheConfig(
                Duration.ofMinutes(20).toMillis(),
                Duration.ofMinutes(20).toMillis()
        ));

        return config;
    }

    @Bean
    public CacheManager cacheManager(RedissonClient redissonClient,
                                     Map<String, CacheConfig> redisCacheConfiguration) {
        // 위에서 만든 설정 Map을 주입받아 사용
        return new RedissonSpringCacheManager(redissonClient, redisCacheConfiguration);
    }
}