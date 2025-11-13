package io.hhplus.tdd.domain.coupon.infrastructure.repository;

import io.hhplus.tdd.domain.coupon.domain.model.Status;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    List<UserCoupon> findByUserId(long userId);
    List<UserCoupon> findByUserIdAndCouponId(long userId , long couponId);
    Optional<UserCoupon> findByUserIdAndIdAndStatus(long userId , long userCouponId , Status status);
}
