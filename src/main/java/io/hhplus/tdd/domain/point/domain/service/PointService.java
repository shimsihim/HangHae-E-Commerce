package io.hhplus.tdd.domain.point.domain.service;

import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import org.springframework.stereotype.Service;

/**
 * 포인트 도메인 서비스
 * - 포인트 충전/사용과 관련된 도메인 로직을 캡슐화합니다.
 * - UserPoint와 PointHistory를 조합한 비즈니스 규칙을 처리합니다.
 */
@Service
public class PointService {

     // 포인트 충전
    public PointHistory chargePoint(UserPoint userPoint, long amount, String description) {
        // 1. 포인트 충전
        userPoint.chargePoint(amount);

        // 2. 포인트 이력 생성
        return PointHistory.createForCharge(
            userPoint.getId(),
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
            userPoint.getId(),
            amount,
            userPoint.getBalance(),
            description
        );
    }
}
