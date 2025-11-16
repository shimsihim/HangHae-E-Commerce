package io.hhplus.tdd.domain.order.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.domain.model.OrderStatus;
import io.hhplus.tdd.domain.order.domain.service.OrderService;
import io.hhplus.tdd.domain.order.exception.OrderException;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 완료 처리 UseCase
 * - PG사 웹훅으로부터 호출
 * - PENDING 상태의 주문을 PAID로 변경
 * - 포인트 차감, 쿠폰 사용 처리
 */
@Service
@RequiredArgsConstructor
public class PayCompleteOrderUseCase {

    private final OrderRepository orderRepository;
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

        // 3. 결제 완료 처리 (Domain Service에 위임)
        orderService.completeOrderWithPayment(order);
    }
}
