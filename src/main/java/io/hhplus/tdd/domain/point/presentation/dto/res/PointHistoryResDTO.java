package io.hhplus.tdd.domain.point.presentation.dto.res;

import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import io.hhplus.tdd.domain.point.domain.model.TransactionType;

public record PointHistoryResDTO (
        long id,
        TransactionType type,
        long amount,
        long balanceAfter,
        String description
    )
    {
        public static PointHistoryResDTO of(PointHistory pointHistory){
            return new PointHistoryResDTO(pointHistory.getId(),pointHistory.getType(),pointHistory.getAmount(),pointHistory.getBalanceAfter(),pointHistory.getDescription());
        }
}
