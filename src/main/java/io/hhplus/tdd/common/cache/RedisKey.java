package io.hhplus.tdd.common.cache;

import lombok.Getter;
import org.redisson.spring.cache.CacheConfig;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public enum RedisKey {

    // ===== @Cacheable 캐시 그룹 (CACHE:) =====
    // 키 이름 , ttl , jitter , @Cacheable 어노테이션 없이 직접 redisson 사용 여부
    COUPON_LIST(
            "CACHE:COUPON_LIST",
            Duration.ofMinutes(20),
            0L,
            false
    ),

    /**
     * 상품 상세 캐시
     * - TTL: 10분
     * - 키: CACHE:PRODUCT_DETAIL::id:123
     * - 조건부 캐싱: 재고 10개 이상
     * - 사용: @Cacheable 어노테이션
     */
    PRODUCT_DETAIL(
            "CACHE:PRODUCT_DETAIL",
            Duration.ofMinutes(10),
            0L,
            false
    ),

    /**
     * 인기 상품 캐시
     * - TTL: 10분
     * - 키: CACHE:POPULAR_PRODUCTS::all
     * - 스케줄러 12시 갱신
     * - 사용: @Cacheable 어노테이션
     */
    POPULAR_PRODUCTS(
            "CACHE:POPULAR_PRODUCTS",
            Duration.ofMinutes(10),
            0L,
            false
    ),

    /**
     * 상품 리스트 페이지 캐시
     * - TTL: 10분
     * - 키: CACHE:PRODUCT_LIST_PAGE::page:0:size:20
     * - 첫 4페이지만 캐싱
     * - 사용: @Cacheable 어노테이션
     */
    PRODUCT_LIST_PAGE(
            "CACHE:PRODUCT_LIST_PAGE",
            Duration.ofMinutes(10),
            0L,
            false
    ),

    // ===== 직접 Redisson 사용 - 캐시 그룹 (CACHE:) =====

    /**
     * 상품 개별 캐시 (MGET용)
     * - TTL: 1시간 (±120초 Jitter)
     * - 키: CACHE:product:123
     * - MultiGetCacheService에서 벌크 조회
     * - 사용: 직접 Redisson API
     */
    CACHE_PRODUCT_BY_ID(
            "CACHE:product:",
            Duration.ofHours(1),
            120L,
            true
    ),

    // ===== 랭킹 그룹 (RANK:) =====

    /**
     * 일일 상품 랭킹 (Sorted Set)
     * - TTL: 14일 (±1시간 Jitter)
     * - 키: RANK:daily:20231204
     * - 사용: RankingService (Sorted Set 직접 사용)
     */
    RANK_DAILY(
            "RANK:daily:",
            Duration.ofDays(14),
            3600L,
            true
    ),

    /**
     * 랭킹 조회용 임시 집합
     * - TTL: 즉시 삭제 (0초)
     * - 키: RANK:daily:view:1701234567890
     * - 사용: RankingService (가중치 합산 계산 후 즉시 삭제)
     */
    RANK_DAILY_VIEW_TEMP(
            "RANK:daily:view:",
            Duration.ZERO,
            0L,
            false
    ),

    // ===== 락 그룹 (LOCK:) =====

    /**
     * 분산 락 (단일)
     * - TTL: 10초 (Jitter 없음)
     * - 키: LOCK:couponId
     * - 사용: DistributedLockAop
     */
    LOCK_DISTRIBUTED(
            "LOCK:",
            Duration.ofSeconds(10),
            0L,
            false
    ),

    /**
     * 분산 락 (다중)
     * - TTL: 10초 (Jitter 없음)
     * - 키: LOCK:PRODUCT_OPTION:1
     * - 사용: MultiDistributedLockExecutor
     */
    LOCK_MULTI(
            "LOCK:",
            Duration.ofSeconds(10),
            0L,
            false
    );

    // ===== @Cacheable용 컴파일 타임 상수 =====

    /**
     * @Cacheable 어노테이션용 캐시 이름 상수
     * <p>
     * Spring의 @Cacheable은 value 파라미터로 컴파일 타임 상수만 허용하므로,
     * static final String 형태로 제공합니다.
     * <p>
     * 사용 예시:
     * <pre>
     * &#64;Cacheable(value = RedisKey.COUPON_LIST_NAME, key = RedisKey.COUPON_LIST_KEY)
     * </pre>
     */
    public static final String COUPON_LIST_NAME = "CACHE:COUPON_LIST";
    public static final String PRODUCT_DETAIL_NAME = "CACHE:PRODUCT_DETAIL";
    public static final String POPULAR_PRODUCTS_NAME = "CACHE:POPULAR_PRODUCTS";
    public static final String PRODUCT_LIST_PAGE_NAME = "CACHE:PRODUCT_LIST_PAGE";

    /**
     * @Cacheable 어노테이션용 키 템플릿 상수
     * <p>
     * 각 캐시별 키 생성 규칙을 중앙에서 관리합니다.
     * SpEL 표현식을 사용하여 동적으로 키를 생성합니다.
     * <p>
     * 사용 예시:
     * <pre>
     * &#64;Cacheable(value = RedisKey.PRODUCT_DETAIL_NAME, key = RedisKey.PRODUCT_DETAIL_KEY)
     * </pre>
     */
    public static final String COUPON_LIST_KEY = "'all'";
    public static final String PRODUCT_DETAIL_KEY = "'id:' + #input.id()";
    public static final String POPULAR_PRODUCTS_KEY = "'all'";
    public static final String PRODUCT_LIST_PAGE_KEY = "'page:' + #input.page() + ':size:' + #input.size()";

    // ===== 필드 정의 =====

    /**
     * Redis 키 프리픽스 또는 전체 키 이름
     * - @Cacheable용: "COUPON_LIST" (전체 이름)
     * - 직접 사용용: "CACHE:product:" (프리픽스, 콜론으로 끝남)
     */
    private final String keyName;
    private final Duration baseTtl;
    private final long jitterRangeSeconds;

    /**
     * 직접 Redisson 사용 여부
     * - false: @Cacheable 전용 (Spring Cache Manager)
     * - true: 직접 Redisson API 사용 (MultiGet, Ranking, Lock)
     */
    private final boolean directRedisson;


    RedisKey(String keyName, Duration baseTtl,
             long jitterRangeSeconds, boolean directRedisson) {
        this.keyName = keyName;
        this.baseTtl = baseTtl;
        this.jitterRangeSeconds = jitterRangeSeconds;
        this.directRedisson = directRedisson;
    }

    // ===== 핵심 메서드 =====

    /**
     * Jitter가 적용된 TTL 반환
     * <p>
     * 캐시가 동시에 만료되어 DB로 몰리는 Cache Stampede 현상을 방지하기 위해
     * 기본 TTL에 ±jitterRange 범위의 랜덤 시간을 추가합니다.
     * <p>
     * ThreadLocalRandom을 사용하여 thread-safe하게 랜덤값을 생성합니다.
     * <p>
     * 사용처:
     * <ul>
     *   <li>MultiGetCacheService: 각 캐시 항목마다 개별 TTL 적용</li>
     *   <li>RankingService: RANK_DAILY에만 Jitter 적용</li>
     * </ul>
     *
     * @return Jitter가 적용된 TTL
     */
    public Duration getTtlWithJitter() {
        if (jitterRangeSeconds == 0) {
            return baseTtl;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        // ±jitterRange 범위의 랜덤값 생성
        long jitterSeconds = random.nextLong(
                -jitterRangeSeconds,
                jitterRangeSeconds + 1
        );
        return baseTtl.plusSeconds(jitterSeconds);
    }

    public String getFullKey(String suffix) {
        return keyName + suffix;
    }

    /**
     * RedisCacheConfig용 CacheConfig Map 자동 생성
     * <p>
     * @Cacheable 전용 키들만 포함하여 Spring Cache Manager 초기화에 사용합니다.
     * 직접 Redisson 사용 키는 제외됩니다.
     * <p>
     * 생성 결과:
     * <ul>
     *   <li>COUPON_LIST → CacheConfig(20분)</li>
     *   <li>PRODUCT_DETAIL → CacheConfig(10분)</li>
     *   <li>POPULAR_PRODUCTS_LIST → CacheConfig(10분)</li>
     *   <li>PRODUCT_LIST_ONE_PAGE → CacheConfig(10분)</li>
     * </ul>
     *
     * @return CacheConfig Map (캐시 이름 → CacheConfig)
     */
    public static Map<String, CacheConfig> generateCacheConfigMap() {
        Map<String, CacheConfig> configs = new HashMap<>();

        for (RedisKey redisKey : values()) {
            // @Cacheable 전용 키만 추가 (directRedisson = false)
            if (!redisKey.directRedisson) {
                long ttlMillis = redisKey.baseTtl.toMillis();
                configs.put(redisKey.keyName, new CacheConfig(ttlMillis, ttlMillis));
            }
        }

        return configs;
    }
}
