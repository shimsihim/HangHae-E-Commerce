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

        UserPoint userPoint = userPointRepository.findByUserId(input.userId())
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, input.userId()));

        long originalBalance = userPoint.getBalance();

        try {
            PointHistory pointHistory = pointService.chargePoint(userPoint, input.amount(), input.description());

            UserPoint savedUserPoint = userPointRepository.save(userPoint);

            pointHistoryRepository.save(pointHistory);

            return Output.from(savedUserPoint);

        } catch (Exception e) {
            log.error("충전 실패. 유저 id: {}, 충전량: {}", input.userId(), input.amount(), e);

            userPoint.updateBalance(originalBalance);
            userPointRepository.save(userPoint);

            throw e;
        }
    }
}
