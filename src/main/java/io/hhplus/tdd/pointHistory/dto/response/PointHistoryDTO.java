package io.hhplus.tdd.pointHistory.dto.response;

import io.hhplus.tdd.pointHistory.domain.PointHistory;
import io.hhplus.tdd.pointHistory.domain.TransactionType;

import java.time.LocalDateTime;

public record PointHistoryDTO(

        long amount,
        long balanceAfter,
        String description,
        TransactionType type,
        LocalDateTime createdAt
) {
    public static PointHistoryDTO from(PointHistory pointHistory){
        return new PointHistoryDTO(pointHistory.getAmount(),pointHistory.getBalanceAfter(), pointHistory.getDescription(), pointHistory.getType() , pointHistory.getCreatedAt());
    }
}