package io.hhplus.tdd.common.cache;

public final class CacheNames {
    public static final String COUPON_LIST = "COUPON_LIST";
    public static final String PRODUCT_DETAIL = "PRODUCT_DETAIL";
    public static final String POPULAR_PRODUCTS_LIST = "POPULAR_PRODUCTS_LIST";
    public static final String PRODUCT_LIST_ONE_PAGE = "PRODUCT_LIST_ONE_PAGE";

    // 단순 랭킹용
    public static final String PRODUCT_RANK_DAILY = "rank:daily:";
    // 랭킹 조회용, 자정 또는 인기상품에 노출될 항목이 부족할 경우등을 고려
    public static final String DAILY_VIEW_KEY = "rank:daily:view:";

    // MultiGetCacheService용 prefix (콜론으로 끝나야 함)
    public static final String PRODUCT_PREFIX = "product:";

}