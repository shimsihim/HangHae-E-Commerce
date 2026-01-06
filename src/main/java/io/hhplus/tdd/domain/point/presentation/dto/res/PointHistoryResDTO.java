package io.hhplus.tdd.domain.point.presentation.dto.res;

public record PointHistoryResDTO (
        long id,
        String type,
        long amount,
        long balanceAfter,
        String description
    )
    {
        public static PointHistoryResDTO of(long id, String type, long amount, long balanceAfter, String description){
            return new PointHistoryResDTO(id, type, amount, balanceAfter, description);
        }
}
