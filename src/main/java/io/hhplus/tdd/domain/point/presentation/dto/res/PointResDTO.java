package io.hhplus.tdd.domain.point.presentation.dto.res;

import io.hhplus.tdd.domain.point.domain.model.UserPoint;

public record PointResDTO(
        long balance
){
    public static PointResDTO of(UserPoint userPoint){
        return new PointResDTO(userPoint.getBalance());
    }
}
