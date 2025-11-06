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
@DisplayName("포인트 충전 UseCase 테스트")
class PointChargeUseCaseTest {

    @InjectMocks
    PointChargeUseCase pointChargeUseCase;

    @Mock
    UserPointRepository userPointRepository;

    @Mock
    PointHistoryRepository pointHistoryRepository;

    @Mock
    PointService pointService;

    @Nested
    @DisplayName("포인트 충전 성공")
    class ChargeSuccess {

        @ParameterizedTest(name = "초기 잔액 {0}, 충전 금액 {1} → 최종 잔액 {2}")
        @CsvSource({
                "0, 1000, 1000",
                "5000, 10000, 15000",
                "100000, 50000, 150000"
        })
        void 정상_충전_테스트(long initialBalance, long chargeAmount, long expectedBalance) {
            // given
            long userId = 1L;
            UserPoint userPoint = UserPoint.builder()
                    .id(userId)
                    .balance(initialBalance)
                    .version(0L)
                    .build();

            PointHistory pointHistory = PointHistory.createForCharge(userId, chargeAmount, expectedBalance, "테스트 충전");

            given(userPointRepository.findByUserId(userId)).willReturn(Optional.of(userPoint));
            given(pointService.chargePoint(any(UserPoint.class), anyLong(), any(String.class)))
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
            PointChargeUseCase.Input input = new PointChargeUseCase.Input(userId, chargeAmount, "테스트 충전");
            PointChargeUseCase.Output output = pointChargeUseCase.execute(input);

            // then
            assertThat(output.userId()).isEqualTo(userId);
            assertThat(output.balance()).isEqualTo(expectedBalance);
            verify(userPointRepository, times(1)).findByUserId(userId);
            verify(pointService, times(1)).chargePoint(any(UserPoint.class), eq(chargeAmount), eq("테스트 충전"));
            verify(userPointRepository, times(1)).save(any(UserPoint.class));
            verify(pointHistoryRepository, times(1)).save(any(PointHistory.class));
        }
    }

    @Nested
    @DisplayName("포인트 충전 실패")
    class ChargeFailure {

        @Test
        @DisplayName("사용자 미존재 시 예외 발생")
        void 사용자_미존재_예외() {
            // given
            long userId = 999L;
            long chargeAmount = 10000L;

            given(userPointRepository.findByUserId(userId)).willReturn(Optional.empty());

            // when & then
            PointChargeUseCase.Input input = new PointChargeUseCase.Input(userId, chargeAmount, "테스트");
            assertThatThrownBy(() -> pointChargeUseCase.execute(input))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userPointRepository, times(1)).findByUserId(userId);
            verify(pointService, never()).chargePoint(any(), anyLong(), any());
        }

        @Test
        @DisplayName("히스토리 저장 실패 시 롤백")
        void 히스토리_저장_실패_롤백() {
            // given
            long userId = 1L;
            long initialBalance = 10000L;
            long chargeAmount = 5000L;

            UserPoint userPoint = UserPoint.builder()
                    .id(userId)
                    .balance(initialBalance)
                    .version(0L)
                    .build();

            PointHistory pointHistory = PointHistory.createForCharge(userId, chargeAmount, initialBalance + chargeAmount, "테스트");

            given(userPointRepository.findByUserId(userId)).willReturn(Optional.of(userPoint));
            given(pointService.chargePoint(any(UserPoint.class), anyLong(), any(String.class)))
                    .willReturn(pointHistory);
            given(userPointRepository.save(any(UserPoint.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(pointHistoryRepository.save(any(PointHistory.class)))
                    .willThrow(new RuntimeException("히스토리 저장 실패"));

            // when & then
            PointChargeUseCase.Input input = new PointChargeUseCase.Input(userId, chargeAmount, "테스트");
            assertThatThrownBy(() -> pointChargeUseCase.execute(input))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("히스토리 저장 실패");

            // 롤백을 위한 save가 2번 호출되어야 함 (1번: 충전, 2번: 롤백)
            verify(userPointRepository, times(2)).save(any(UserPoint.class));
        }
    }

    @Nested
    @DisplayName("포인트 충전 유효성 검증")
    class ChargeValidation {

        @ParameterizedTest(name = "최소 금액 미만: {0}")
        @CsvSource({"0", "100", "999"})
        void 최소_충전_금액_미만_예외(long chargeAmount) {
            // given
            long userId = 1L;
            UserPoint userPoint = UserPoint.builder()
                    .id(userId)
                    .balance(10000L)
                    .version(0L)
                    .build();

            given(userPointRepository.findByUserId(userId)).willReturn(Optional.of(userPoint));
            given(pointService.chargePoint(any(UserPoint.class), anyLong(), any(String.class)))
                    .willAnswer(invocation -> {
                        UserPoint up = invocation.getArgument(0);
                        up.chargePoint(chargeAmount); // 도메인 검증 로직 실행
                        return null;
                    });

            // when & then
            PointChargeUseCase.Input input = new PointChargeUseCase.Input(userId, chargeAmount, "테스트");
            assertThatThrownBy(() -> pointChargeUseCase.execute(input))
                    .isInstanceOf(PointRangeException.class);
        }

        @ParameterizedTest(name = "최대 포인트 초과: 초기 잔액 {0}, 충전 금액 {1}")
        @CsvSource({
                "999000000, 1001000",
                "500000000, 500000001"
        })
        void 최대_포인트_초과_예외(long initialBalance, long chargeAmount) {
            // given
            long userId = 1L;
            UserPoint userPoint = UserPoint.builder()
                    .id(userId)
                    .balance(initialBalance)
                    .version(0L)
                    .build();

            given(userPointRepository.findByUserId(userId)).willReturn(Optional.of(userPoint));
            given(pointService.chargePoint(any(UserPoint.class), anyLong(), any(String.class)))
                    .willAnswer(invocation -> {
                        UserPoint up = invocation.getArgument(0);
                        up.chargePoint(chargeAmount); // 도메인 검증 로직 실행
                        return null;
                    });

            // when & then
            PointChargeUseCase.Input input = new PointChargeUseCase.Input(userId, chargeAmount, "테스트");
            assertThatThrownBy(() -> pointChargeUseCase.execute(input))
                    .isInstanceOf(PointRangeException.class);
        }
    }
}
