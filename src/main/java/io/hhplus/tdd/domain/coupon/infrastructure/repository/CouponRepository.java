package io.hhplus.tdd.domain.coupon.infrastructure.repository;

import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    @Query("SELECT uc.coupon FROM UserCoupon uc WHERE uc.id = :userCouponId ")
    Optional<Coupon> findCouponWithUserCoupon(Long userCouponId);
}
