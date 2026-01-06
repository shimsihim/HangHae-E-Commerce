package io.hhplus.tdd.domain.point.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.exception.UserNotFoundException;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.infrastructure.repository.UserPointRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GetUserPointQueryTest {

    @InjectMocks
    GetUserPointQuery getUserPointQuery;

    @Mock
    UserPointRepository userPointRepository;

    @Test
    void 사용자_포인트_조회_성공() {
        // given
        long userId = 1L;
        long balance = 100000L;

        UserPoint userPoint = UserPoint.builder()
                .id(userId)
                .balance(balance)
                .build();

        given(userPointRepository.findById(userId)).willReturn(Optional.of(userPoint));

        // when
        GetUserPointQuery.Input input = new GetUserPointQuery.Input(userId);
        GetUserPointQuery.Output result = getUserPointQuery.execute(input);

        // then
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.balance()).isEqualTo(balance);
        verify(userPointRepository).findById(userId);
    }

    @Test
    void 사용자가_존재하지_않는_경우_예외_발생() {
        // given
        long userId = 9999L;
        given(userPointRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        GetUserPointQuery.Input input = new GetUserPointQuery.Input(userId);
        assertThatThrownBy(() -> getUserPointQuery.execute(input))
                .isInstanceOf(UserNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(userPointRepository).findById(userId);
    }
}
