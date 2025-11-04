package io.hhplus.tdd.domain.user.repository;

import io.hhplus.tdd.domain.user.database.UserTable;
import io.hhplus.tdd.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserTable userTable;

    @Override
    public Optional<User> selectById(long userId) {
        return Optional.of(userTable.selectById(userId));
    }

    @Override
    public User updateUserBalance(User user) {
        return userTable.updateBalance(user);
    }

}
