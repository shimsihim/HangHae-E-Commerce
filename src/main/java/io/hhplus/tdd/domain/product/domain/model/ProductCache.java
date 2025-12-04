package io.hhplus.tdd.domain.product.domain.model;

import java.io.Serializable;

/**
 * Redis 캐시에 저장할 상품 기본 정보
 * - JPA 연관관계 없이 순수 데이터만 포함
 * - 직렬화 가능한 불변 객체
 */
public record ProductCache(
        Long id,
        String name,
        String description,
        Long basePrice
) implements Serializable {

    public static ProductCache from(Product product) {
        return new ProductCache(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getBasePrice()
        );
    }

    /**
     * Product 엔티티로 변환
     * 주의: 연관관계(options)는 포함되지 않음
     */
    public Product toProduct() {
        return Product.builder()
                .id(id)
                .name(name)
                .description(description)
                .basePrice(basePrice)
                .build();
    }
}
