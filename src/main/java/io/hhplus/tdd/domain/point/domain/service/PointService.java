package io.hhplus.tdd.domain.point.domain.service;

import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import org.springframework.stereotype.Service;


/**
 * 포인트 도메인 서비스
 * - 포인트 충전/사용 시 이력 생성 담당
 * - 검증 로직은 UserPoint 엔티티에 위임
 */
@Service
public class PointService {

     /**
      * 포인트 충전
      * - 충전 검증 및 처리는 UserPoint 엔티티에서 수행
      * @return 포인트 이력
      */
    public PointHistory chargePoint(UserPoint userPoint, long amount, String description) {
        // 1. 포인트 충전 (엔티티에서 검증 포함)
        userPoint.chargePoint(amount);

        // 2. 포인트 이력 생성
        return PointHistory.createForCharge(
            userPoint,
            amount,
            userPoint.getBalance(),
            description
        );
    }

    /**
     * 포인트 사용
     * - 사용 검증 및 처리는 UserPoint 엔티티에서 수행
     * @return 포인트 이력
     */
    public PointHistory usePoint(UserPoint userPoint, long amount, String description) {
        // 1. 포인트 사용 (엔티티에서 검증 포함)
        userPoint.usePoint(amount);

        // 2. 포인트 이력 생성
        return PointHistory.createForUse(
            userPoint,
            amount,
            userPoint.getBalance(),
            description
        );
    }
}
