package io.hhplus.tdd.common.cache;

import io.hhplus.tdd.domain.product.application.GetProductDetailUseCase;
import org.springframework.stereotype.Component;

/**
 * 상품 상세 정보 캐싱 조건 판단 클래스
 * 모든 상품 옵션의 재고가 임계값 이상일 때만 캐싱
 */
@Component
public class ProductCacheCondition {

    /**
     * 재고 부족 임계값
     * 이 값보다 적은 재고를 가진 옵션이 있으면 캐싱하지 않음
     */
    private static final long LOW_STOCK_THRESHOLD = 10L;

    /**
     * 상품 상세 정보를 캐싱할지 여부를 판단
     */
    public boolean shouldCache(GetProductDetailUseCase.Output output) {
        if (output == null || output.options() == null) {
            return false;
        }

        // 모든 옵션의 재고가 LOW_STOCK_THRESHOLD 이상인지 확인
        return output.options().stream()
                .allMatch(option -> option.quantity() != null && option.quantity() >= LOW_STOCK_THRESHOLD);
    }
}
