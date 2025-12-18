package io.hhplus.tdd.domain.order.domain.event;

import java.time.LocalDateTime;

/**
 * 주문 완료 이벤트
 * - 주문 결제 완료 시 발행되는 도메인 이벤트
 * - 데이터 플랫폼 전송 등 외부 연동에 사용
 *
 * Java record 사용:
 * - 불변성 보장
 * - Jackson 역직렬화 자동 지원 (어노테이션 불필요)
 * - Lombok과의 충돌 없음
 */
public record OrderCompletedEvent(
        Long orderId,
        Long userId,
        Long totalAmount,
        Long discountAmount,
        Long usePointAmount,
        Long finalAmount,
        LocalDateTime completedAt
) {
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
