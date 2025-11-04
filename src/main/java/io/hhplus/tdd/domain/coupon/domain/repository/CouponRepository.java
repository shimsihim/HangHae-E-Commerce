package io.hhplus.tdd.domain.coupon.domain.repository;

import io.hhplus.tdd.domain.coupon.domain.model.Coupon;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {
    List<Coupon> findAll();
    Optional<Coupon> findById(long couponId);
}
