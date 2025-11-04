package io.hhplus.tdd.domain.point.presentation;


import io.hhplus.tdd.domain.point.application.GetPointHistoryListQuery;
import io.hhplus.tdd.domain.point.application.GetUserPointQuery;
import io.hhplus.tdd.domain.point.application.PointChargeUseCase;
import io.hhplus.tdd.domain.point.application.PointUseUseCase;
import io.hhplus.tdd.domain.point.presentation.dto.req.UserPointChargeReqDTO;
import io.hhplus.tdd.domain.point.presentation.dto.req.UserPointUseReqDTO;
import io.hhplus.tdd.domain.point.presentation.dto.res.PointHistoryResDTO;
import io.hhplus.tdd.domain.point.presentation.dto.res.PointResDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "포인트 관리 API", description = "사용자 포인트 충전, 사용, 조회 기능을 제공합니다.")
@RestController
@RequestMapping("/api/point")
@RequiredArgsConstructor
@Validated
public class PointController {

    private final GetPointHistoryListQuery getPointHistoryListQuery;
    private final GetUserPointQuery getUserPointQuery;
    private final PointChargeUseCase pointChargeUseCase;
    private final PointUseUseCase pointUseUseCase;

    @GetMapping("/balance/{userId}")
    public PointResDTO getBalance(@PathVariable Long userId){
        GetUserPointQuery.Output output = getUserPointQuery.execute(new GetUserPointQuery.Input(userId));
        return PointResDTO.of(output.balance());
    }

    @GetMapping("/{userId}")
    public List<PointHistoryResDTO> getPointHistory(@PathVariable Long userId){
        return getPointHistoryListQuery.execute(new GetPointHistoryListQuery.Input(userId)).stream()
                .map(output -> PointHistoryResDTO.of(output.id(), output.type(), output.amount(), output.balanceAfter(), output.description()))
                .collect(Collectors.toUnmodifiableList());
    }

    @PostMapping("/charge")
    public PointResDTO chargeBalance(@RequestBody @Validated UserPointChargeReqDTO chargeReq){
        PointChargeUseCase.Output output = pointChargeUseCase.execute(
                new PointChargeUseCase.Input(chargeReq.userId(), chargeReq.amount(), chargeReq.description())
        );
        return PointResDTO.of(output.balance());
    }

    @PostMapping("/use")
    public PointResDTO useBalance(@RequestBody @Validated UserPointUseReqDTO useReq){
        PointUseUseCase.Output output = pointUseUseCase.execute(
                new PointUseUseCase.Input(useReq.userId(), useReq.amount(), useReq.description())
        );
        return PointResDTO.of(output.balance());
    }


}
