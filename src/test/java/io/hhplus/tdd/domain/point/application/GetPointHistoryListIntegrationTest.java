package io.hhplus.tdd.domain.point.application;

import io.hhplus.tdd.domain.BaseIntegrationTest;
import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import io.hhplus.tdd.domain.point.domain.model.TransactionType;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.infrastructure.repository.PointHistoryRepository;
import io.hhplus.tdd.domain.point.infrastructure.repository.UserPointRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GetPointHistoryListIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private GetPointHistoryListQuery getPointHistoryListQuery;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Test
    @DisplayName("포인트 히스토리 조회 통합 테스트 - 사용자의 포인트 히스토리를 정상적으로 조회한다")
    void 포인트_히스토리_조회_성공() {
        // given
        UserPoint userPoint = UserPoint.builder()
                .balance(100000L)
                .build();
        UserPoint savedUserPoint = userPointRepository.save(userPoint);

        PointHistory history1 = PointHistory.createForCharge(savedUserPoint, 50000L, 50000L, "포인트 충전");
        PointHistory history2 = PointHistory.createForCharge(savedUserPoint, 50000L, 100000L, "포인트 충전");
        PointHistory history3 = PointHistory.createForUse(savedUserPoint, 10000L, 90000L, "주문 결제");

        pointHistoryRepository.save(history1);
        pointHistoryRepository.save(history2);
        pointHistoryRepository.save(history3);

        // when
        GetPointHistoryListQuery.Input input = new GetPointHistoryListQuery.Input(savedUserPoint.getId());
        List<GetPointHistoryListQuery.Output> result = getPointHistoryListQuery.execute(input);

        // then
        assertThat(result).hasSizeGreaterThanOrEqualTo(3);
        assertThat(result).anyMatch(output ->
                output.type().equals("CHARGE") &&
                        output.amount() == 50000L
        );
        assertThat(result).anyMatch(output ->
                output.type().equals("USE") &&
                        output.amount() == 10000L
        );
    }

    @Test
    @DisplayName("포인트 히스토리가 없는 경우 빈 리스트 반환")
    void 포인트_히스토리_조회_빈_리스트() {
        // given
        long userId = 9999L; // 존재하지 않는 사용자

        // when
        GetPointHistoryListQuery.Input input = new GetPointHistoryListQuery.Input(userId);
        List<GetPointHistoryListQuery.Output> result = getPointHistoryListQuery.execute(input);

        // then
        assertThat(result).isEmpty();
    }
}
