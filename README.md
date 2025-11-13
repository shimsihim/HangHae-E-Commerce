## 서비스의 병목 예상 쿼리

### 인기 상품 조회

``` java
    @Query("SELECT new io.hhplus.tdd.domain.product.infrastructure.repository.ProductSalesDto(p, SUM(oi.quantity)) " +
            "FROM Product p, OrderItem oi " +
            "WHERE oi.productId = p.id " +
            "  AND oi.createdAt >= :threeDaysAgo " + // 파라미터 사용
            "GROUP BY p.id ORDER BY SUM(oi.quantity) DESC")
    List<ProductSalesDto> findPopular(@Param("threeDaysAgo") LocalDateTime threeDaysAgo);
    
```

``` mysql
explain    SELECT
        PRODUCT.ID,
        PRODUCT.BASE_PRICE,
        PRODUCT.DESCRIPTION,
        PRODUCT.NAME,
        SUM(ORDER_ITEM.QUANTITY) 
    FROM
        PRODUCT,
        ORDER_ITEM 
    WHERE
        ORDER_ITEM.PRODUCT_ID=PRODUCT.ID 
        AND ORDER_ITEM.CREATED_AT>='2025-11-11T01:37:53.831' 
    GROUP BY
        PRODUCT.ID 
    ORDER BY
        SUM(ORDER_ITEM.QUANTITY) DESC
```

![explain 결과](/img/explain.png)

### 병목지점
+ ORDER_ITEM 테이블의 경우 Full Table Scan

### 조치사항 : 인덱스 추가
```sql
CREATE INDEX idx_oi_product_created_quantity 
ON ORDER_ITEM (PRODUCT_ID, CREATED_AT, QUANTITY);
```

### 결과
![explain 결과](/img/explain2.png)



