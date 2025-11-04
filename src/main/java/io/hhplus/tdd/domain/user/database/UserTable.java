package io.hhplus.tdd.domain.user.database;

import io.hhplus.tdd.domain.user.domain.User;
import io.hhplus.tdd.domain.user.domain.UserRole;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 해당 Table 클래스는 변경하지 않고 공개된 API 만을 사용해 데이터를 제어합니다.
 */
@Component
public class UserTable {

    @PostConstruct
    void makeTestUser(){
        User user = User.builder()
                            .name("심경섭")
                            .role(UserRole.ROLE_USER)
                            .id(cursor.get())
                            .email("test@naver.com")
                            .balance(100000L)
                            .build();
        table.put(user.getId() , user);
    }

    private final Map<Long, User> table = new HashMap<>();
    private AtomicLong cursor = new AtomicLong(1);

    public User selectById(long id) {
        throttle(200);
        return table.get(id);
    }

    public User updateBalance(User user) {
        throttle(300);
        User newUser = table.get(user.getId());
        if(newUser != null){
            newUser.updateBalance(user.getBalance());
        }
        return newUser;
    }

    private void throttle(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep((long) (Math.random() * millis));
        } catch (InterruptedException ignored) {

        }
    }
}
