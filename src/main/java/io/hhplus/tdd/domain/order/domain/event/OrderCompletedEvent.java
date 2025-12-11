package io.hhplus.tdd.domain.order.domain.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 주문 완료 이벤트
 * - 주문 결제 완료 시 발행되는 도메인 이벤트
 * - 데이터 플랫폼 전송 등 외부 연동에 사용
 */
@Getter
public class OrderCompletedEvent {

    private final Long orderId;
    private final Long userId;
    private final Long totalAmount;
    private final Long discountAmount;
    private final Long usePointAmount;
    private final Long finalAmount;
    private final LocalDateTime completedAt;

    public OrderCompletedEvent(Long orderId, Long userId, Long totalAmount,
                              Long discountAmount, Long usePointAmount,
                              Long finalAmount, LocalDateTime completedAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.usePointAmount = usePointAmount;
        this.finalAmount = finalAmount;
        this.completedAt = completedAt;
    }

    public static OrderCompletedEvent from(Long orderId, Long userId, Long totalAmount,
                                          Long discountAmount, Long usePointAmount,
                                          Long finalAmount) {
        return new OrderCompletedEvent(
            orderId,
            userId,
            totalAmount,
            discountAmount,
            usePointAmount,
            finalAmount,
            LocalDateTime.now()
        );
    }
}
