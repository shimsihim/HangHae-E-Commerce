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

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiGetCacheService {

    private final RedissonClient redissonClient;

    // 여러 ID의 캐시 데이터를 MGET으로 일괄 조회
    public <T> Map<Long, T> getMap(RedisKey keyType, List<Long> ids, Class<T> type) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        String[] keys = ids.stream()
                .map(id -> keyType.getFullKey(String.valueOf(id)))
                .toArray(String[]::new);

        // RBuckets로 MGET 한방 쿼리
        RBuckets buckets = redissonClient.getBuckets();
        Map<String, T> rawResult;

        try {
            rawResult = buckets.get(keys);
            log.debug("Cache MGET: keyType={}, requested={}, hits={}",
                    keyType.name(), ids.size(), rawResult.size());
        } catch (Exception e) {
            log.error("Redis MGET failed: keyType={}, keys={}", keyType.name(), keys.length, e);
            return Collections.emptyMap();
        }

        Map<Long, T> result = new HashMap<>();
        for (Long id : ids) {
            String key = keyType.getFullKey(String.valueOf(id));
            T value = rawResult.get(key);
            if (value != null) {
                result.put(id, value);
            }
        }

        return result;
    }

    // 여러 데이터를 MSET으로 일괄 저장 (각 항목마다 개별 TTL 적용)
    public <T> void putAll(RedisKey keyType, Map<Long, T> data) {
        if (data == null || data.isEmpty()) {
            return;
        }
        RBatch batch = redissonClient.createBatch();
        data.forEach((id, value) -> {
            String key = keyType.getFullKey(String.valueOf(id));
            Duration ttl = keyType.getTtlWithJitter();  // 각 항목마다 개별 계산
            batch.getBucket(key).setAsync(value, ttl.toMillis(), TimeUnit.MILLISECONDS);
        });

        try {
            batch.execute();
            log.debug("Cache MSET with individual TTLs: keyType={}, count={}",
                    keyType.name(), data.size());
        } catch (Exception e) {
            log.error("Redis Batch Put failed: keyType={}, count={}", keyType.name(), data.size(), e);
        }
    }

    // 여러 캐시 키를 한번에 삭제
    public void deleteByIds(RedisKey keyType, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        try {
            String[] keys = ids.stream()
                    .map(id -> keyType.getFullKey(String.valueOf(id)))
                    .toArray(String[]::new);

            redissonClient.getKeys().delete(keys);
            log.debug("Cache DELETE: keyType={}, count={}", keyType.name(), ids.size());
        } catch (Exception e) {
            log.error("Redis DELETE failed: keyType={}, count={}", keyType.name(), ids.size(), e);
        }
    }
}
