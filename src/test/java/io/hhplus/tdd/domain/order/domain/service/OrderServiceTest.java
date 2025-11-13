package io.hhplus.tdd.domain.order.domain.service;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.exception.ProductException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderServiceTest {

    private final OrderService orderService = new OrderService();

    @Nested
    class 총_금액_계산 {

        @Test
        void 단일_상품_금액_계산() {
            // given
            long productId = 1L;
            long optionId = 1L;
            long price = 10000L;
            long quantity = 2L;

            ProductOption option = ProductOption.builder()
                    .id(optionId)
                    .productId(productId)
                    .price(price)
                    .quantity(10L)
                    .build();

            Map<Long, ProductOption> optionMap = Map.of(optionId, option);
            List<OrderService.OrderItemInfo> items = List.of(
                    OrderService.OrderItemInfo.of(productId, optionId , price, quantity)
            );

            // when
            long totalAmount = orderService.calculateTotalAmount(items);

            // then
            assertThat(totalAmount).isEqualTo(price * quantity);
        }

        @Test
        void 여러_상품_금액_계산() {
            // given
            ProductOption option1 = ProductOption.builder()
                    .id(1L).productId(1L).price(10000L).quantity(10L).build();
            ProductOption option2 = ProductOption.builder()
                    .id(2L).productId(2L).price(20000L).quantity(10L).build();
            ProductOption option3 = ProductOption.builder()
                    .id(3L).productId(3L).price(5000L).quantity(10L).build();

            Map<Long, ProductOption> optionMap = Map.of(
                    1L, option1,
                    2L, option2,
                    3L, option3
            );

            List<OrderService.OrderItemInfo> items = List.of(
                    OrderService.OrderItemInfo.of(1L, 1L,10000L,  2L),  // 10000 * 2 = 20000
                    OrderService.OrderItemInfo.of(2L, 2L,20000L,  1L),  // 20000 * 1 = 20000
                    OrderService.OrderItemInfo.of(3L, 3L, 5000L, 3L)   // 5000 * 3 = 15000
            );

            // when
            long totalAmount = orderService.calculateTotalAmount(items);

            // then
            assertThat(totalAmount).isEqualTo(55000L);
        }
    }

    @Nested
    class ValidateAndDeductStock {

        @Test
        void 재고_차감_성공() {
            // given
            long initialQuantity = 10L;
            long orderQuantity = 3L;

            ProductOption option = ProductOption.builder()
                    .id(1L)
                    .productId(1L)
                    .price(10000L)
                    .quantity(initialQuantity)
                    .build();

            Map<Long, ProductOption> optionMap = Map.of(1L, option);
            List<OrderService.OrderItemInfo> items = List.of(
                    OrderService.OrderItemInfo.of(option.getProductId() , option.getId(), option.getPrice(), orderQuantity)
            );

            // when
            orderService.validateAndDeductStock(items, optionMap);

            // then
            assertThat(option.getQuantity()).isEqualTo(initialQuantity - orderQuantity);
        }

        @ParameterizedTest(name = "재고 {0}, 주문 수량 {1} → 재고 부족")
        @CsvSource({
                "0, 1",
                "5, 6",
                "10, 11"
        })
        void 재고_부족_예외(long stock, long orderQuantity) {
            // given
            ProductOption option = ProductOption.builder()
                    .id(1L)
                    .productId(1L)
                    .price(10000L)
                    .quantity(stock)
                    .build();

            Map<Long, ProductOption> optionMap = Map.of(1L, option);
            List<OrderService.OrderItemInfo> items = List.of(
                    OrderService.OrderItemInfo.of(option.getProductId() , option.getId(), option.getPrice(), orderQuantity)
            );

            // when , then
            assertThatThrownBy(() -> orderService.validateAndDeductStock(items, optionMap))
                    .isInstanceOf(ProductException.class);
        }

        @Test
        void 여러_상품_재고_차감() {
            // given
            ProductOption option1 = ProductOption.builder()
                    .id(1L).productId(1L).price(10000L).quantity(10L).build();
            ProductOption option2 = ProductOption.builder()
                    .id(2L).productId(2L).price(20000L).quantity(5L).build();

            Map<Long, ProductOption> optionMap = Map.of(1L, option1, 2L, option2);
            List<OrderService.OrderItemInfo> items = List.of(
                    OrderService.OrderItemInfo.of(1L, 1L,10000L, 3L),
                    OrderService.OrderItemInfo.of(2L, 2L, 20000L,2L)
            );

            // when
            orderService.validateAndDeductStock(items, optionMap);

            // then
            assertThat(option1.getQuantity()).isEqualTo(7L);
            assertThat(option2.getQuantity()).isEqualTo(3L);
        }
    }
}
