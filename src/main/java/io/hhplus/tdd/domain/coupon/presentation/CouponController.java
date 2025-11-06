package io.hhplus.tdd.domain.coupon.presentation;

import io.hhplus.tdd.domain.coupon.application.GetAllCouponListUseCase;
import io.hhplus.tdd.domain.coupon.application.GetUserCouponListUseCase;
import io.hhplus.tdd.domain.coupon.application.IssueUserCouponUseCase;
import io.hhplus.tdd.domain.coupon.application.UseUserCouponUseCase;
import io.hhplus.tdd.domain.coupon.presentation.dto.req.CouponIssueReqDTO;
import io.hhplus.tdd.domain.coupon.presentation.dto.req.CouponUseReqDTO;
import io.hhplus.tdd.domain.coupon.presentation.dto.res.CouponResDTO;
import io.hhplus.tdd.domain.coupon.presentation.dto.res.UserCouponResDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "쿠폰 관리 API", description = "쿠폰 조회 및 발급.")
@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
@Validated
public class CouponController {

    private final GetUserCouponListUseCase getUserCouponListUseCase;
    private final GetAllCouponListUseCase getAllCouponListUseCase;
    private final IssueUserCouponUseCase issueUserCouponUseCase;
    private final UseUserCouponUseCase useUserCouponUseCase;

    @GetMapping()
    public List<CouponResDTO> getAllCouponList(){
        return getAllCouponListUseCase.execute().stream()
                .map(out ->CouponResDTO.of(out.id(),out.couponName(),out.discountType(),out.discountValue(),
                        out.totalQuantity(),out.issuedQuantity(),out.limitPerUser(),
                        out.duration(),out.minOrderValue(),out.validFrom(),out.validUntil()))
                .toList(); // 추후 매퍼를 통한 변환 예정
    }

    @GetMapping("/{userId}")
    public List<UserCouponResDTO> getUserCouponList(@PathVariable Long userId){
        return getUserCouponListUseCase.execute(new GetUserCouponListUseCase.Input(userId)).stream()
                .map(UserCouponResDTO :: from)
                .toList();
    }

    @PostMapping("/issue")
    public void issueUserCoupopn(@RequestBody @Validated CouponIssueReqDTO couponIssueReqDTO){
        issueUserCouponUseCase.execute(
                new IssueUserCouponUseCase.Input(couponIssueReqDTO.couponId(), couponIssueReqDTO.userId())
        );
    }

    @PostMapping("/use")
    public void useUserCoupon(@RequestBody @Validated CouponUseReqDTO couponUseReqDTO){
        useUserCouponUseCase.execute(new UseUserCouponUseCase.Input(couponUseReqDTO.userId(),couponUseReqDTO.userCouponId()));
    }
}
