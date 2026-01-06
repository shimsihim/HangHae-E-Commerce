package io.hhplus.tdd.config;

import io.hhplus.tdd.common.cache.RedisKey;
import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(RedissonClient redissonClient) {
        // RedisKey에서 자동 생성된 CacheConfig 사용
        return new RedissonSpringCacheManager(
                redissonClient,
                RedisKey.generateCacheConfigMap()
        );
    }
}