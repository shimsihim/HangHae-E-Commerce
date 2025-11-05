package io.hhplus.tdd.domain.point.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.exception.UserNotFoundException;
import io.hhplus.tdd.common.lock.LockAnn;
import io.hhplus.tdd.common.lock.LockId;
import io.hhplus.tdd.common.lock.LockKey;
import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.domain.repository.PointHistoryRepository;
import io.hhplus.tdd.domain.point.domain.repository.UserPointRepository;
import io.hhplus.tdd.domain.point.domain.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 포인트 충전 애플리케이션 서비스
 * - 포인트 충전 유스케이스의 흐름을 조율합니다.
 * - Repository를 통한 데이터 조회/저장을 담당합니다.
 * - Domain Service에 비즈니스 로직을 위임합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointChargeUseCase {

    private final PointHistoryRepository pointHistoryRepository;
    private final UserPointRepository userPointRepository;
    private final PointService pointService;

    public record Input(
            @LockId long userId,
            long amount,
            String description
    ){}

    public record Output(
            long userId,
            long balance,
            long version
    ){
        public static Output from(UserPoint userPoint){
            return new Output(userPoint.getId(), userPoint.getBalance(), userPoint.getVersion());
        }
    }

    @LockAnn(lockKey = LockKey.USER)
    public Output execute(Input input){
        UserPoint userPoint;
        // 1. 사용자 포인트 조회
        userPoint = userPointRepository.findByUserId(input.userId())
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, input.userId()));
        // 2. 도메인 서비스를 통한 포인트 충전 로직 실행
        PointHistory pointHistory = pointService.chargePoint(userPoint, input.amount(), input.description());

        // 3. 변경된 포인트 정보 저장
        UserPoint savedUserPoint = userPointRepository.save(userPoint);

        // 4. 이력 저장 , 에러 시 충전 롤백 X
        pointHistoryRepository.save(pointHistory);

        return Output.from(savedUserPoint);


    }
}
