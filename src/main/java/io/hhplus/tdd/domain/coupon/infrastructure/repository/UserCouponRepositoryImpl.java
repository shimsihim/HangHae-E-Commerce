package io.hhplus.tdd.domain.coupon.infrastructure.repository;

import io.hhplus.tdd.domain.coupon.database.UserCouponTable;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.domain.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserCouponRepositoryImpl implements UserCouponRepository {

    private final UserCouponTable userCouponTable;

    @Override
    public List<UserCoupon> findByUserId(long userId) {
        return userCouponTable.selectAll().stream().filter(userCopon -> userCopon.getUserId() == userId).toList();
    }

    @Override
    public List<UserCoupon> findByUserIdAndCouponId(long userId, long couponId) {
        return userCouponTable.selectAll().stream().filter(userCopon -> userCopon.getUserId() == userId && userCopon.getCouponId() == couponId).toList();
    }

    @Override
    public Optional<UserCoupon> findByUserIdAndUserCouponIdAndStatus(long userId, long userCouponId, String status) {
        return userCouponTable.selectAll().stream().filter(userCopon -> userCopon.getUserId() == userId && userCopon.getId() == userCouponId && userCopon.getStatus().toString().equals(status)).findFirst();
    }

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        return userCouponTable.save(userCoupon);
    }
}



