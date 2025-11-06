package io.hhplus.tdd.domain.point.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.exception.UserNotFoundException;
import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.domain.repository.PointHistoryRepository;
import io.hhplus.tdd.domain.point.domain.repository.UserPointRepository;
import io.hhplus.tdd.domain.point.domain.service.PointService;
import io.hhplus.tdd.domain.point.exception.PointRangeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("포인트 사용 UseCase 테스트")
class PointUseUseCaseTest {

    @InjectMocks
    PointUseUseCase pointUseUseCase;

    @Mock
    UserPointRepository userPointRepository;

    @Mock
    PointHistoryRepository pointHistoryRepository;

    @Mock
    PointService pointService;

    @Nested
    @DisplayName("포인트 사용 성공")
    class UseSuccess {

        @ParameterizedTest(name = "초기 잔액 {0}, 사용 금액 {1} → 최종 잔액 {2}")
        @CsvSource({
                "10000, 1000, 9000",
                "50000, 10000, 40000",
                "100000, 100000, 0"
        })
        void 정상_사용_테스트(long initialBalance, long useAmount, long expectedBalance) {
            // given
            long userId = 1L;
            UserPoint userPoint = UserPoint.builder()
                    .id(userId)
                    .balance(initialBalance)
                    .version(0L)
                    .build();

            PointHistory pointHistory = PointHistory.createForUse(userId, useAmount, expectedBalance, "테스트 사용");

            given(userPointRepository.findByUserId(userId)).willReturn(Optional.of(userPoint));
            given(pointService.usePoint(any(UserPoint.class), anyLong(), any(String.class)))
                    .willReturn(pointHistory);
            given(userPointRepository.save(any(UserPoint.class))).willAnswer(invocation -> {
                UserPoint saved = invocation.getArgument(0);
                return UserPoint.builder()
                        .id(saved.getId())
                        .balance(expectedBalance)
                        .version(saved.getVersion())
                        .build();
            });
            given(pointHistoryRepository.save(any(PointHistory.class))).willReturn(pointHistory);

            // when
            PointUseUseCase.Input input = new PointUseUseCase.Input(userId, useAmount, "테스트 사용");
            PointUseUseCase.Output output = pointUseUseCase.execute(input);

            // then
            assertThat(output.userId()).isEqualTo(userId);
            assertThat(output.balance()).isEqualTo(expectedBalance);
            verify(userPointRepository, times(1)).findByUserId(userId);
            verify(pointService, times(1)).usePoint(any(UserPoint.class), eq(useAmount), eq("테스트 사용"));
            verify(userPointRepository, times(1)).save(any(UserPoint.class));
            verify(pointHistoryRepository, times(1)).save(any(PointHistory.class));
        }
    }

    @Nested
    @DisplayName("포인트 사용 실패")
    class UseFailure {

        @Test
        @DisplayName("사용자 미존재 시 예외 발생")
        void 사용자_미존재_예외() {
            // given
            long userId = 999L;
            long useAmount = 10000L;

            given(userPointRepository.findByUserId(userId)).willReturn(Optional.empty());

            // when & then
            PointUseUseCase.Input input = new PointUseUseCase.Input(userId, useAmount, "테스트");
            assertThatThrownBy(() -> pointUseUseCase.execute(input))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userPointRepository, times(1)).findByUserId(userId);
            verify(pointService, never()).usePoint(any(), anyLong(), any());
        }

        @Test
        @DisplayName("히스토리 저장 실패 시 롤백")
        void 히스토리_저장_실패_롤백() {
            // given
            long userId = 1L;
            long initialBalance = 10000L;
            long useAmount = 5000L;

            UserPoint userPoint = UserPoint.builder()
                    .id(userId)
                    .balance(initialBalance)
                    .version(0L)
                    .build();

            PointHistory pointHistory = PointHistory.createForUse(userId, useAmount, initialBalance - useAmount, "테스트");

            given(userPointRepository.findByUserId(userId)).willReturn(Optional.of(userPoint));
            given(pointService.usePoint(any(UserPoint.class), anyLong(), any(String.class)))
                    .willReturn(pointHistory);
            given(userPointRepository.save(any(UserPoint.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(pointHistoryRepository.save(any(PointHistory.class)))
                    .willThrow(new RuntimeException("히스토리 저장 실패"));

            // when & then
            PointUseUseCase.Input input = new PointUseUseCase.Input(userId, useAmount, "테스트");
            assertThatThrownBy(() -> pointUseUseCase.execute(input))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("히스토리 저장 실패");

            // 롤백을 위한 save가 2번 호출되어야 함 (1번: 사용, 2번: 롤백)
            verify(userPointRepository, times(2)).save(any(UserPoint.class));
        }

        @ParameterizedTest(name = "잔액 부족: 초기 잔액 {0}, 사용 금액 {1}")
        @CsvSource({
                "0, 1000",
                "5000, 5001",
                "100, 1000"
        })
        void 잔액_부족_예외(long initialBalance, long useAmount) {
            // given
            long userId = 1L;
            UserPoint userPoint = UserPoint.builder()
                    .id(userId)
                    .balance(initialBalance)
                    .version(0L)
                    .build();

            given(userPointRepository.findByUserId(userId)).willReturn(Optional.of(userPoint));
            given(pointService.usePoint(any(UserPoint.class), anyLong(), any(String.class)))
                    .willAnswer(invocation -> {
                        UserPoint up = invocation.getArgument(0);
                        up.usePoint(useAmount); // 도메인 검증 로직 실행
                        return null;
                    });

            // when & then
            PointUseUseCase.Input input = new PointUseUseCase.Input(userId, useAmount, "테스트");
            assertThatThrownBy(() -> pointUseUseCase.execute(input))
                    .isInstanceOf(PointRangeException.class);
        }

        @ParameterizedTest(name = "최소 사용 금액 미만: {0}")
        @CsvSource({"0", "50", "99"})
        void 최소_사용_금액_미만_예외(long useAmount) {
            // given
            long userId = 1L;
            UserPoint userPoint = UserPoint.builder()
                    .id(userId)
                    .balance(10000L)
                    .version(0L)
                    .build();

            given(userPointRepository.findByUserId(userId)).willReturn(Optional.of(userPoint));
            given(pointService.usePoint(any(UserPoint.class), anyLong(), any(String.class)))
                    .willAnswer(invocation -> {
                        UserPoint up = invocation.getArgument(0);
                        up.usePoint(useAmount); // 도메인 검증 로직 실행
                        return null;
                    });

            // when & then
            PointUseUseCase.Input input = new PointUseUseCase.Input(userId, useAmount, "테스트");
            assertThatThrownBy(() -> pointUseUseCase.execute(input))
                    .isInstanceOf(PointRangeException.class);
        }
    }
}
