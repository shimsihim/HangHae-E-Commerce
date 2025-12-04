package io.hhplus.tdd.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBatch;
import org.redisson.api.RBuckets;
import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.CacheConfig;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

// Redis MGET/MSET을 활용한 다중 키 캐시 서비스 (RBuckets 사용)
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiGetCacheService {

    private final RedissonClient redissonClient;
    // [핵심] 아까 등록한 설정 Bean을 주입받음
    private final Map<String, CacheConfig> redisCacheConfiguration;

    // 기본 TTL (설정에 없을 경우 대비 안전장치: 5분)
    private static final long DEFAULT_TTL_MILLIS = Duration.ofMinutes(5).toMillis();

    /**
     * @param prefix 캐시 키 prefix
     * @param ids 조회할 ID 리스트
     * @param type 반환 타입 클래스
     * @return ID를 키로 하는 Map
     */
    public <T> Map<Long, T> getMap(String prefix, List<Long> ids, Class<T> type) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        String[] keys = ids.stream()
                .map(id -> prefix + id)
                .toArray(String[]::new);

        // RBuckets로 MGET 한방 쿼리
        RBuckets buckets = redissonClient.getBuckets();
        Map<String, T> rawResult;

        try {
            rawResult = buckets.get(keys);
            log.debug("Cache MGET: prefix={}, requested={}, hits={}",
                    prefix, ids.size(), rawResult.size());
        } catch (Exception e) {
            log.error("Redis MGET failed: prefix={}, keys={}", prefix, keys.length, e);
            return Collections.emptyMap();
        }

        Map<Long, T> result = new HashMap<>();
        for (Long id : ids) {
            String key = prefix + id;
            T value = rawResult.get(key);
            if (value != null) {
                result.put(id, value);
            }
        }

        return result;
    }

    /**
     * MSET 수행 - 여러 데이터를 한번에 캐시에 저장 (TTL 설정)
     * - TTL 설정이 필요하므로 RBatch 사용
     *
     * @param prefix 캐시 키 prefix
     * @param data ID를 키로 하는 데이터 맵
     */
    public <T> void putAll(String prefix, Map<Long, T> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        // 1. 설정 Map에서 TTL 조회
        long ttlMillis = DEFAULT_TTL_MILLIS;
        CacheConfig config = redisCacheConfiguration.get(cacheName);

        if (config != null) {
            ttlMillis = config.getTTL(); // 설정된 TTL 가져오기 (밀리초 단위)
        } else {
            log.warn("No TTL config found for cacheName: {}. Using default.", cacheName);
        }

        // TTL 설정이 필요하므로 RBatch 사용
        RBatch batch = redissonClient.createBatch();
        data.forEach((id, value) -> {
            String key = prefix + id;
            batch.getBucket(key).setAsync(value, ttl.toMillis(), TimeUnit.MILLISECONDS);
        });

        try {
            batch.execute();
            log.debug("Cache MSET: prefix={}, count={}, ttl={}", prefix, data.size(), ttl);
        } catch (Exception e) {
            log.error("Redis Batch Put failed: prefix={}, count={}", prefix, data.size(), e);
        }
    }

    /**
     * 여러 캐시 키를 한번에 삭제
     *
     * @param prefix 캐시 키 prefix
     * @param ids 삭제할 ID 리스트
     */
    public void deleteByIds(String prefix, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        try {
            String[] keys = ids.stream()
                    .map(id -> prefix + id)
                    .toArray(String[]::new);

            redissonClient.getKeys().delete(keys);
            log.debug("Cache DELETE: prefix={}, count={}", prefix, ids.size());
        } catch (Exception e) {
            log.error("Redis DELETE failed: prefix={}, count={}", prefix, ids.size(), e);
        }
    }
}
