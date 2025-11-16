//package io.hhplus.tdd.domain.coupon.presentation;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import io.hhplus.tdd.domain.coupon.application.IssueUserCouponUseCase;
//import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
//import io.hhplus.tdd.domain.coupon.domain.model.DiscountType;
//import io.hhplus.tdd.domain.coupon.domain.model.Status;
//import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
//import io.hhplus.tdd.domain.coupon.infrastructure.repository.CouponRepository;
//import io.hhplus.tdd.domain.coupon.infrastructure.repository.UserCouponRepository;
//import io.hhplus.tdd.domain.coupon.presentation.dto.req.CouponIssueReqDTO;
//import jakarta.persistence.EntityManager;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
//import org.springframework.transaction.annotation.Transactional;
//import org.testcontainers.containers.MySQLContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import java.time.LocalDate;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@Testcontainers
//@Transactional
//@AutoConfigureMockMvc
//class CouponControllerTest {
//
//    @Container
//    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
//            .withDatabaseName("testdb")
//            .withUsername("test")
//            .withPassword("test");
//
//    @DynamicPropertySource
//    static void registerProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.datasource.url", mysql::getJdbcUrl);
//        registry.add("spring.datasource.username", mysql::getUsername);
//        registry.add("spring.datasource.password", mysql::getPassword);
//    }
//
//
//    @Autowired
//    private IssueUserCouponUseCase issueUserCouponUseCase;
//
//    @Autowired
//    private CouponRepository couponRepository;
//
//    @Autowired
//    private UserCouponRepository userCouponRepository;
//
//    @Autowired
//    private MockMvc mvc;
//
//    @Autowired
//    private EntityManager em;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Test
//    @DisplayName("쿠폰 발급 통합 테스트 - 사용자가 쿠폰을 정상적으로 발급받는다")
//    void 쿠폰_발급_성공() throws Exception {
//
//        // Given: 쿠폰 준비 (발급 가능한 쿠폰)
//        Coupon coupon = Coupon.builder()
//                .couponName("신규 회원 할인 쿠폰")
//                .discountType(DiscountType.PERCENTAGE)
//                .discountValue(10)
//                .totalQuantity(100)
//                .issuedQuantity(0)
//                .limitPerUser(1)
//                .duration(30)
//                .minOrderValue(10000)
//                .validFrom(LocalDate.now().minusDays(1))
//                .validUntil(LocalDate.now().plusDays(30))
//                .build();
//        Coupon savedCoupon = couponRepository.save(coupon);
//        em.clear();
//
//        long userId = 1L;
//
//        // When: 사용자가 쿠폰을 발급받는다
//
//        mvc.perform(MockMvcRequestBuilders.post("/api/coupon")
//                        .content(objectMapper.writeValueAsString(new CouponIssueReqDTO(1L,savedCoupon.getId()))))
//                //then
//                .andExpect(status().isOk())
//                .andDo(print());
//        IssueUserCouponUseCase.Input input = new IssueUserCouponUseCase.Input(savedCoupon.getId(), userId);
//        issueUserCouponUseCase.execute(input);
//
//        // Then: 사용자 쿠폰이 발급되었는지 확인
//        List<UserCoupon> userCoupons = userCouponRepository.findByUserIdAndCouponId(userId, savedCoupon.getId());
//        assertThat(userCoupons).hasSize(1);
//
//        UserCoupon issuedCoupon = userCoupons.get(0);
//        assertThat(issuedCoupon.getUserId()).isEqualTo(userId);
//        assertThat(issuedCoupon.getCouponId()).isEqualTo(savedCoupon.getId());
//        assertThat(issuedCoupon.getStatus()).isEqualTo(Status.ISSUED);
//        assertThat(issuedCoupon.getExpiredAt()).isEqualTo(LocalDate.now().plusDays(30));
//
//        // Then: 쿠폰의 발급 수량이 증가했는지 확인
//        Coupon updatedCoupon = couponRepository.findById(savedCoupon.getId()).orElseThrow();
//        assertThat(updatedCoupon.getIssuedQuantity()).isEqualTo(1);
//    }
//}