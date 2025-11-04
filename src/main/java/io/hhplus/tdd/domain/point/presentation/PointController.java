package io.hhplus.tdd.domain.point.presentation;


import io.hhplus.tdd.domain.point.application.GetPointHistoryListQuery;
import io.hhplus.tdd.domain.point.application.PointChargeHandler;
import io.hhplus.tdd.domain.point.application.PointUseHandler;
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
    private final PointChargeHandler pointChargeHandler;
    private final PointUseHandler pointUseHandler;


    @GetMapping("/{userId}")
    public List<PointHistoryResDTO> getPointHistory(@PathVariable Long userId){
        return getPointHistoryListQuery.handle(new GetPointHistoryListQuery.Input(userId)).stream()
                .map(output -> PointHistoryResDTO.of(output.id(), output.type(), output.amount(), output.balanceAfter(), output.description()))
                .collect(Collectors.toUnmodifiableList());
    }

    @PostMapping("/charge")
    public PointResDTO chargeBalance(@RequestBody @Validated UserPointChargeReqDTO chargeReq){
        PointChargeHandler.Output output = pointChargeHandler.handle(new PointChargeHandler.Input(chargeReq.userId(), chargeReq.amount(), chargeReq.description()));
        return PointResDTO.of(output.balance());
    }

    @PostMapping("/use")
    public PointResDTO useBalance(@RequestBody @Validated UserPointUseReqDTO useReq){
        PointUseHandler.Output output = pointUseHandler.handle(new PointUseHandler.Input(useReq.userId(), useReq.amount(), useReq.description()));
        return PointResDTO.of(output.balance());
    }


}
