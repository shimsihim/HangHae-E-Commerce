package io.hhplus.tdd.user.service;

import io.hhplus.tdd.common.lock.LockAnn;
import io.hhplus.tdd.pointHistory.domain.TransactionType;
import io.hhplus.tdd.pointHistory.service.PointHistoryService;
import io.hhplus.tdd.user.database.UserTable;
import io.hhplus.tdd.user.domain.User;
import io.hhplus.tdd.user.dto.request.PointChargeDTO;
import io.hhplus.tdd.user.dto.request.PointUseDTO;
import io.hhplus.tdd.user.dto.response.UserPointDTO;
import io.hhplus.tdd.user.dto.response.UserPointTransactionDTO;
import io.hhplus.tdd.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final PointHistoryService pointHistoryService;
    private final UserRepository userPointRepository;

    @Override
    public User getUserByUserId(long userId) {
        return userPointRepository.selectById(userId).orElseThrow(()-> new RuntimeException());
    }

    @Override
    public UserPointDTO getPointAmount(long userId) {
        User user = userPointRepository.selectById(userId).orElseThrow(()-> new RuntimeException());
        return UserPointDTO.from(user);
    }

    @LockAnn
    @Override
    public UserPointTransactionDTO chargePoint(long userId, PointChargeDTO pointChargeDTO) {
        User user = userPointRepository.selectById(userId).orElseThrow(()-> new RuntimeException());
        user.chargePoint(pointChargeDTO.amount());

        User afterSave = userPointRepository.updateUserBalance(user);
        long afterBalance = afterSave.getBalance();
        pointHistoryService.addUseHistory(userId, pointChargeDTO.amount() , afterBalance , "");
        return new UserPointTransactionDTO(pointChargeDTO.amount(), afterBalance , TransactionType.CHARGE);
    }

    @LockAnn
    @Override
    public UserPointTransactionDTO usePoint(long userId, PointUseDTO pointUseDTO) {
        User user = userPointRepository.selectById(userId).orElseThrow(()-> new RuntimeException());
        user.usePoint(pointUseDTO.amount());

        User afterSave = userPointRepository.updateUserBalance(user);
        long afterBalance = afterSave.getBalance();
        pointHistoryService.addUseHistory(userId, pointUseDTO.amount() , afterBalance , "");
        return new UserPointTransactionDTO(pointUseDTO.amount(), afterBalance , TransactionType.USE);
    }
}
