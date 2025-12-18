package io.hhplus.tdd.common.outbox;

public enum OutboxStatus {
    PENDING,      // 발행 대기 중
    PUBLISHED,    // 발행 완료
    DEAD_LETTER   // 최대 재시도 초과 (수동 처리 필요)
}
