package io.hhplus.tdd.domain.point.application;

import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import io.hhplus.tdd.domain.point.domain.model.TransactionType;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.infrastructure.repository.PointHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GetPointHistoryListQueryTest {

    @InjectMocks
    GetPointHistoryListQuery getPointHistoryListQuery;

    @Mock
    PointHistoryRepository pointHistoryRepository;

    @Test
    void 포인트_히스토리_조회_성공() {
        // given
        long userId = 1L;
        UserPoint userPoint = UserPoint.builder()
                .id(userId)
                .balance(100000L)
                .build();

        PointHistory history1 = PointHistory.builder()
                .id(1L)
                .userId(userId)
                .userPoint(userPoint)
                .type(TransactionType.CHARGE)
                .amount(50000L)
                .balanceAfter(50000L)
                .description("포인트 충전")
                .build();

        PointHistory history2 = PointHistory.builder()
                .id(2L)
                .userId(userId)
                .userPoint(userPoint)
                .type(TransactionType.CHARGE)
                .amount(50000L)
                .balanceAfter(100000L)
                .description("포인트 충전")
                .build();

        PointHistory history3 = PointHistory.builder()
                .id(3L)
                .userId(userId)
                .userPoint(userPoint)
                .type(TransactionType.USE)
                .amount(10000L)
                .balanceAfter(90000L)
                .description("주문 결제")
                .build();

        List<PointHistory> histories = Arrays.asList(history1, history2, history3);
        given(pointHistoryRepository.findByUserId(userId)).willReturn(histories);

        // when
        GetPointHistoryListQuery.Input input = new GetPointHistoryListQuery.Input(userId);
        List<GetPointHistoryListQuery.Output> result = getPointHistoryListQuery.execute(input);

        // then
        assertThat(result).hasSize(3);

        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).type()).isEqualTo("CHARGE");
        assertThat(result.get(0).amount()).isEqualTo(50000L);
        assertThat(result.get(0).balanceAfter()).isEqualTo(50000L);

        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(1).type()).isEqualTo("CHARGE");
        assertThat(result.get(1).amount()).isEqualTo(50000L);
        assertThat(result.get(1).balanceAfter()).isEqualTo(100000L);

        assertThat(result.get(2).id()).isEqualTo(3L);
        assertThat(result.get(2).type()).isEqualTo("USE");
        assertThat(result.get(2).amount()).isEqualTo(10000L);
        assertThat(result.get(2).balanceAfter()).isEqualTo(90000L);

        verify(pointHistoryRepository).findByUserId(userId);
    }

    @Test
    void 포인트_히스토리가_없는_경우_빈_리스트_반환() {
        // given
        long userId = 1L;
        given(pointHistoryRepository.findByUserId(userId)).willReturn(Collections.emptyList());

        // when
        GetPointHistoryListQuery.Input input = new GetPointHistoryListQuery.Input(userId);
        List<GetPointHistoryListQuery.Output> result = getPointHistoryListQuery.execute(input);

        // then
        assertThat(result).isEmpty();
        verify(pointHistoryRepository).findByUserId(userId);
    }
}
