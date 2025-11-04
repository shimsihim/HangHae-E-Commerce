package io.hhplus.tdd.domain.point.presentation.dto.res;

public record PointResDTO(
        long balance
){
    public static PointResDTO of(long balance){
        return new PointResDTO(balance);
    }
}
