package io.hhplus.tdd.domain.order.domain.service;

import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.domain.model.OrderItem;
import org.springframework.stereotype.Service;

/**
 * 포인트 도메인 서비스
 * - 포인트 충전/사용과 관련된 도메인 로직을 캡슐화합니다.
 * - UserPoint와 PointHistory를 조합한 비즈니스 규칙을 처리합니다.
 */
@Service
public class OrderService {

     // 포인트 충전
    public Order chargePoint(OrderItem userPoint, long amount, String description) {
        // 1. 포인트 충전
        userPoint.chargePoint(amount);

        // 2. 포인트 이력 생성
        return Order.createForCharge(
            userPoint.getId(),
            amount,
            userPoint.getBalance(),
            description
        );
    }

    // 포인트 사용 처리
    public Order usePoint(OrderItem userPoint, long amount, String description) {
        // 1. 포인트 사용
        userPoint.usePoint(amount);

        // 2. 포인트 이력 생성
        return Order.createForUse(
            userPoint.getId(),
            amount,
            userPoint.getBalance(),
            description
        );
    }
}
