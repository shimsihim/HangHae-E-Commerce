package io.hhplus.tdd.domain.coupon.domain.repository;

import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository {
    List<UserCoupon> findByUserId(long userId);
    List<UserCoupon> findByUserIdAndCouponId(long userId , long couponId);
    Optional<UserCoupon> findByUserIdAndUserCouponIdAndStatus(long userId , long userCouponId , String status);
    UserCoupon save(UserCoupon userCoupon);
}
