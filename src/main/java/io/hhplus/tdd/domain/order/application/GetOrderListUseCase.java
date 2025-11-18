package io.hhplus.tdd.domain.order.application;

import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.domain.model.OrderItem;
import io.hhplus.tdd.domain.order.domain.model.OrderStatus;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderItemRepository;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetOrderListUseCase {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public record Input(
        long userId
    ){}

    @Transactional(readOnly = true)
    public List<OrderInfo> execute(Input input){
        List<Order> orders = orderRepository.findByUserId(input.userId);
        List<OrderInfo> orderInfos = orders.stream().map(order ->{
            List<OrderItemInfo> orderItems = orderItemRepository.findByOrderId(order.getId()).stream().map(OrderItemInfo::from).toList();
            if(orderItems.isEmpty()){
                log.warn("No orderItem found for orderId={}",order.getId());
            }
            return OrderInfo.from(order , orderItems);
        }).toList();

        return orderInfos;
    }


    @Builder
    public static record OrderInfo(
            Long id,
            Long userId,
            Long userCouponId,
            OrderStatus status,
            Long totalAmount,
            Long discountAmount,
            Long usePointAmount,
            Long finalAmount,
            LocalDateTime createdAt,
            LocalDateTime paidAt,
            List<OrderItemInfo> orderItems
    )
    {
        public static OrderInfo from(Order order , List<OrderItemInfo> orderItems){
            return new OrderInfoBuilder()
                    .id(order.getId())
                    .userId(order.getUserId())
                    .userCouponId(order.getUserCouponId())
                    .status(order.getStatus())
                    .totalAmount(order.getTotalAmount())
                    .discountAmount(order.getDiscountAmount())
                    .usePointAmount(order.getUsePointAmount())
                    .finalAmount(order.getFinalAmount())
                    .createdAt(order.getCreatedAt())
                    .paidAt(order.getPaidAt())
                    .orderItems(orderItems)
                    .build();
        }

    }
    @Builder
    public static record OrderItemInfo(
            Long id,
            Long orderItemId,
            Long orderId,
            Long productOptionId,
            int quantity,
            Long unitPrice,
            Long subTotal
    ){
        public static OrderItemInfo from(OrderItem orderItem){
            return new OrderItemInfoBuilder()
                    .id(orderItem.getId())
                    .productOptionId(orderItem.getProductOptionId())
                    .quantity(orderItem.getQuantity())
                    .unitPrice(orderItem.getUnitPrice())
                    .subTotal(orderItem.getSubtotal())
                    .build();
        }
    }
}
