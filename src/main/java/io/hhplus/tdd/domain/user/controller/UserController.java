package io.hhplus.tdd.domain.user.controller;


import io.hhplus.tdd.common.aop.LoginUser;
import io.hhplus.tdd.domain.user.domain.User;
import io.hhplus.tdd.domain.user.dto.request.PointChargeDTO;
import io.hhplus.tdd.domain.user.dto.request.PointUseDTO;
import io.hhplus.tdd.domain.user.dto.response.UserPointDTO;
import io.hhplus.tdd.domain.user.dto.response.UserPointTransactionDTO;
import io.hhplus.tdd.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "유저 관리 API", description = "사용자 정보 관리 기능을 제공합니다.")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Validated
public class UserController {
    private final UserService userService;

    @GetMapping
    public UserPointDTO getPointAmount(@LoginUser User user){
        long id = user.getId();
        return userService.getPointAmount(user.getId());
    }

    public UserPointTransactionDTO usePoint(@LoginUser User user , @RequestBody PointUseDTO pointUseDTO){
        return userService.usePoint(user.getId() , pointUseDTO);
    }

    public UserPointTransactionDTO chargePoint(@LoginUser User user ,@RequestBody PointChargeDTO pointChargeDTO){
        return userService.chargePoint(user.getId() , pointChargeDTO);
    }

}
