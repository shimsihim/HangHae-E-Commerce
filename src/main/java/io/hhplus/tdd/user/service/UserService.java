package io.hhplus.tdd.user.service;

import io.hhplus.tdd.user.domain.User;
import io.hhplus.tdd.user.dto.request.PointChargeDTO;
import io.hhplus.tdd.user.dto.request.PointUseDTO;
import io.hhplus.tdd.user.dto.response.UserPointDTO;
import io.hhplus.tdd.user.dto.response.UserPointTransactionDTO;

public interface UserService {
    User getUserByUserId(long userId);
    UserPointDTO getPointAmount(long userId);
    UserPointTransactionDTO usePoint(long userId , PointUseDTO pointUseDTO);
    UserPointTransactionDTO chargePoint(long userId , PointChargeDTO pointChargeDTO);
}
