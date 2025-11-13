package io.hhplus.tdd.domain.coupon.infrastructure.repository;

import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
}
