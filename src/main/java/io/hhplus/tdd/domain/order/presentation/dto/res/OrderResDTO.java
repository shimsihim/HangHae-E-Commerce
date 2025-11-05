package io.hhplus.tdd.domain.order.presentation.dto.res;

import io.hhplus.tdd.domain.order.application.GetOrderListUseCase;
import io.hhplus.tdd.domain.order.domain.model.OrderStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record OrderResDTO(
        Long id,
        Long userId,
        Long userCouponId,
        OrderStatus status,
        Long totalAmount,
        Long discountAmount,
        Long usePointAmount,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        List<OrderItemResDTO> orderItems
    )
    {
        public static OrderResDTO from(GetOrderListUseCase.OrderInfo orderInfo){

            List<OrderItemResDTO> orderItems = orderInfo.orderItems().stream().map(OrderItemResDTO::from).toList();

            return OrderResDTO.builder()
                    .id(orderInfo.id())
                    .userId(orderInfo.userId())
                    .userCouponId(orderInfo.userCouponId())
                    .status(orderInfo.status())
                    .totalAmount(orderInfo.totalAmount())
                    .discountAmount(orderInfo.discountAmount())
                    .usePointAmount(orderInfo.usePointAmount())
                    .createdAt(orderInfo.createdAt())
                    .paidAt(orderInfo.paidAt())
                    .orderItems(orderItems)
                    .build();
        }


        @Builder
        public record OrderItemResDTO(
                Long id,
                Long orderItemId,
                Long orderId,
                Long productOptionId,
                int quantity,
                Long unitPrice,
                Long subTotal
        ){
            public static OrderItemResDTO from(GetOrderListUseCase.OrderItemInfo orderItem){
                return OrderItemResDTO.builder()
                        .id(orderItem.id())
                        .orderItemId(orderItem.orderItemId())
                        .orderId(orderItem.orderId())
                        .productOptionId(orderItem.productOptionId())
                        .quantity(orderItem.quantity())
                        .unitPrice(orderItem.unitPrice())
                        .subTotal(orderItem.subTotal())
                        .build();
            }

        }
}
