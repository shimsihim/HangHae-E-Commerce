package io.hhplus.tdd.domain.user.service;

import io.hhplus.tdd.domain.user.domain.User;
import io.hhplus.tdd.domain.user.dto.request.PointChargeDTO;
import io.hhplus.tdd.domain.user.dto.request.PointUseDTO;
import io.hhplus.tdd.domain.user.dto.response.UserPointDTO;
import io.hhplus.tdd.domain.user.dto.response.UserPointTransactionDTO;

public interface UserService {
    User getUserByUserId(long userId);
    UserPointDTO getPointAmount(long userId);
    UserPointTransactionDTO usePoint(long userId , PointUseDTO pointUseDTO);
    UserPointTransactionDTO chargePoint(long userId , PointChargeDTO pointChargeDTO);
}
