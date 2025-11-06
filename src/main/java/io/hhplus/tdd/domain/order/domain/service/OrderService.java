package io.hhplus.tdd.domain.order.domain.service;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.exception.ProductException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    //총 금액 계산
    public long calculateTotalAmount(List<OrderItemInfo> items) {
        long totalAmount = 0;
        for (OrderItemInfo item : items) {
            totalAmount += item.optionPrice() * item.quantity();
        }
        return totalAmount;
    }

    //재고 검증 및 차감
    public void validateAndDeductStock(List<OrderItemInfo> items, Map<Long, ProductOption> productOptionMap) {
        for (OrderItemInfo item : items) {
            ProductOption productOption = productOptionMap.get(item.productOptionId());
            if (productOption == null) {
                throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND, item.productId(), item.productOptionId());
            }

            // 재고 검증
            if (productOption.getQuantity() < item.quantity()) {
                throw new ProductException(ErrorCode.PRODUCT_NOT_ENOUGH, item.productId(), item.productOptionId());
            }

            // 재고 차감 
            productOption.deductWithoutValidation(item.quantity());
        }
    }

    //재고 복구
    public void restoreStock(List<OrderItemInfo> items, Map<Long, ProductOption> productOptionMap, Map<Long, Long> originalStockMap) {
        for (OrderItemInfo item : items) {
            ProductOption productOption = productOptionMap.get(item.productOptionId());
            Long originalStock = originalStockMap.get(item.productOptionId());
            if (productOption != null && originalStock != null) {
                productOption.restore(originalStock);
            }
        }
    }


    public record OrderItemInfo(
            Long productId,
            Long productOptionId,
            Long optionPrice,
            Long quantity
    ) {
        public static OrderItemInfo of(Long productId, Long productOptionId,Long optionPrice, Long quantity) {
            return new OrderItemInfo(productId, productOptionId ,optionPrice, quantity);
        }
    }
}
