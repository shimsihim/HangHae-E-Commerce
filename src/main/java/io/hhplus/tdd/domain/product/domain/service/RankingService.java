package io.hhplus.tdd.domain.product.domain.service;

import io.hhplus.tdd.common.cache.CacheNames;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final RedissonClient redissonClient;

    public void addScore(long productId ,double score ){
        String todayKey = getDailyKey(LocalDate.now());

        RScoredSortedSet<String> todaySet = redissonClient.getScoredSortedSet(todayKey);

        todaySet.addScore(String.valueOf(productId), score);
        todaySet.expire(14, TimeUnit.DAYS);
    }

    // 일일 인기 상품 ID 목록 조회 (캐시 없이 실시간 계산)
    // 오늘 + 어제 데이터를 가중치 합산
    public List<Long> getDailyTopRankIds(int limit) {
        String todayKey = getDailyKey(LocalDate.now());
        String yesterdayKey = getDailyKey(LocalDate.now().minusDays(1));

        //  임시 집합 생성
        String tempKey = CacheNames.DAILY_VIEW_KEY + System.currentTimeMillis();
        RScoredSortedSet<String> tempSet = redissonClient.getScoredSortedSet(tempKey);

        try {
            // 가중치 합산
            Map<String, Double> weights = new HashMap<>();
            weights.put(todayKey, 1.0);
            weights.put(yesterdayKey, 0.5); // 어제 데이터 가중치 0.5

            tempSet.union(weights);

            // 상위 N개 조회
            Collection<String> rawIds = tempSet.valueRangeReversed(0, limit - 1);

            List<Long> resultIds = rawIds.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            // TODO: 부족한 개수 채우기 (최신 상품 또는 MD 상품)
            if (resultIds.size() < limit) {
                int missingCount = limit - resultIds.size();
                // 최신 상품 또는 MD상품 가져오기...
            }

            return resultIds;
        } finally {
            // 5. 임시 집합 삭제
            tempSet.delete();
        }
    }


    private String getDailyKey(LocalDate date) {
        return CacheNames.PRODUCT_RANK_DAILY + date.format(DateTimeFormatter.ofPattern("yyyymmdd"));
    }
}
