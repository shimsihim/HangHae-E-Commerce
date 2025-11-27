package io.hhplus.tdd.domain.point.application;

import io.hhplus.tdd.common.distributedLock.DistributedLock;
import io.hhplus.tdd.common.distributedLock.LockGroupType;
import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.exception.UserNotFoundException;
import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.infrastructure.repository.PointHistoryRepository;
import io.hhplus.tdd.domain.point.infrastructure.repository.UserPointRepository;
import io.hhplus.tdd.domain.point.domain.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class PointUseUseCase {

    private final PointHistoryRepository pointHistoryRepository;
    private final UserPointRepository userPointRepository;
    private final PointService pointService;

    public record Input(
            long userId,
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

    @DistributedLock(group = LockGroupType.USER_POINT, key = "#input.userId")
    @Transactional
    public Output execute(Input input){
        UserPoint userPoint = userPointRepository.findById(input.userId())
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, input.userId()));

        PointHistory pointHistory = pointService.usePoint(userPoint, input.amount(), input.description());

        pointHistoryRepository.save(pointHistory);

        return Output.from(userPoint);
    }
}
