package io.hhplus.tdd.domain.product.domain.service;

import io.hhplus.tdd.common.cache.RedisKey;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
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

    // 인기상품 점수 추가
    public void addScore(long productId, double score) {
        String todayKey = getDailyKey(LocalDate.now());

        RScoredSortedSet<String> todaySet = redissonClient.getScoredSortedSet(todayKey);

        todaySet.addScore(String.valueOf(productId), score);

        if (todaySet.remainTimeToLive() == -1) {
            Duration ttl = RedisKey.RANK_DAILY.getTtlWithJitter();
            todaySet.expire(ttl.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    // 1일 인기상품 아이디 조회
    public List<Long> getDailyTopRankIds(int limit) {
        String todayKey = getDailyKey(LocalDate.now());
        String yesterdayKey = getDailyKey(LocalDate.now().minusDays(1));

        // 임시 집합 생성
        String tempKey = RedisKey.RANK_DAILY_VIEW_TEMP.getFullKey(
                String.valueOf(System.currentTimeMillis())
        );
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
            // 임시 집합 삭제
            tempSet.delete();
        }
    }

    // 일일 랭킹 키 생성
    private String getDailyKey(LocalDate date) {
        return RedisKey.RANK_DAILY.getFullKey(
                date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        );
    }
}
