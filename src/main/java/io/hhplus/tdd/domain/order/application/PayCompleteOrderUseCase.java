package io.hhplus.tdd.domain.order.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.domain.model.OrderItem;
import io.hhplus.tdd.domain.order.domain.model.OrderStatus;
import io.hhplus.tdd.domain.order.domain.service.OrderService;
import io.hhplus.tdd.domain.order.exception.OrderException;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderItemRepository;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderRepository;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 결제 완료 처리 UseCase
 * - PG사 웹훅으로부터 호출
 * - PENDING 상태의 주문을 PAID로 변경
 * - 재고 차감, 포인트 차감, 쿠폰 사용 처리
 */
@Service
@RequiredArgsConstructor
public class PayCompleteOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;

    public record Input(
            long orderId
    ){}

    @Transactional
    public void execute(Input input){
        // 1. 주문 조회
        Order order = orderRepository.findById(input.orderId())
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND, input.orderId()));

        // 2. PENDING 상태 검증
        if(order.getStatus() != OrderStatus.PENDING){
            throw new OrderException(ErrorCode.ORDER_NOT_VALID, input.orderId());
        }

        // 3. 주문 항목 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(input.orderId());
        if(orderItems.isEmpty()){
            throw new OrderException(ErrorCode.ORDER_NOT_VALID, input.orderId());
        }

        // 4. OrderItem을 OrderItemInfo 리스트로 변환
        List<OrderService.OrderItemInfo> orderItemInfos = orderItems.stream()
                .map(item -> new OrderService.OrderItemInfo(item.getProductOptionId(), item.getQuantity()))
                .collect(Collectors.toList());

        // 5. 상품 및 옵션 조회 (Pessimistic Lock)
        List<Long> optionIds = orderItemInfos.stream()
                .map(OrderService.OrderItemInfo::productOptionId)
                .toList();
        List<Product> products = productRepository.findProductsWithOptionsForUpdate(optionIds);

        // 6. 결제 완료 처리 (재고 차감, 포인트 차감, 쿠폰 사용, 상태 변경)
        orderService.completeOrderWithPayment(order, products, orderItemInfos);
    }
}