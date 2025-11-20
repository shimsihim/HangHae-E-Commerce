package io.hhplus.tdd.domain.order.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.domain.model.OrderItem;
import io.hhplus.tdd.domain.order.domain.service.OrderService;
import io.hhplus.tdd.domain.order.exception.OrderException;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderItemRepository;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderRepository;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductOptionRepository;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 주문 취소 UseCase
 * - PENDING 상태의 주문만 취소 가능
 * - 재고 복구 처리
 */
@Service
@RequiredArgsConstructor
public class CancelOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final ProductOptionRepository productOptionRepository;

    public record Input(
            long orderId
    ){}

    @Transactional
    public void execute(Input input) {
        // 1. 주문 조회
        Order order = orderRepository.findById(input.orderId())
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND, input.orderId()));

        // 2. 주문 취소 처리 (PENDING 검증 포함)
        orderService.cancelOrder(order);

        // 3. 주문 항목 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

        // 4. OrderItem을 OrderItemInfo 리스트로 변환
        List<OrderService.OrderItemInfo> orderItemInfos = orderItems.stream()
                .map(item -> new OrderService.OrderItemInfo(item.getProductOptionId(), item.getQuantity()))
                .collect(Collectors.toList());

        // 5. 상품 조회 , 비관락 필요하지 않을까?
        List<Long> optionIds = orderItemInfos.stream()
                .map(OrderService.OrderItemInfo::productOptionId)
                .toList();
        List<ProductOption> productOptions = productOptionRepository.findAllById(optionIds);

        // 6. 재고 복구 (Domain Service)
        orderService.restoreStock(productOptions, orderItemInfos);
    }
}