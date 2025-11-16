package io.hhplus.tdd.domain.point.domain.service;

import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import org.springframework.stereotype.Service;


@Service
public class PointService {

     // 포인트 충전
    public PointHistory chargePoint(UserPoint userPoint, long amount, String description) {
        // 1. 포인트 충전
        userPoint.chargePoint(amount);

        // 2. 포인트 이력 생성
        return PointHistory.createForCharge(
            userPoint,
            amount,
            userPoint.getBalance(),
            description
        );
    }

    // 포인트 사용 처리
    public PointHistory usePoint(UserPoint userPoint, long amount, String description) {
        // 1. 포인트 사용
        userPoint.usePoint(amount);

        // 2. 포인트 이력 생성
        return PointHistory.createForUse(
            userPoint,
            amount,
            userPoint.getBalance(),
            description
        );
    }

    public void validUsePoint(UserPoint userPoint , long usePoint){
        userPoint.validUsePoint(usePoint);
    }
}
