package io.hhplus.tdd.domain.order.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.DiscountType;
import io.hhplus.tdd.domain.coupon.domain.model.Status;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.CouponRepository;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.UserCouponRepository;
import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.domain.model.OrderItem;
import io.hhplus.tdd.domain.order.domain.model.OrderStatus;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderItemRepository;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderRepository;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.infrastructure.repository.PointHistoryRepository;
import io.hhplus.tdd.domain.point.infrastructure.repository.UserPointRepository;
import io.hhplus.tdd.domain.point.exception.PointRangeException;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductOptionRepository;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductRepository;
import io.hhplus.tdd.domain.product.exception.ProductException;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
@Slf4j
class MakeOrderIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private EntityManager em;

    @Autowired
    private MakeOrderUseCase makeOrderUseCase;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("정상 주문 생성 - 쿠폰과 포인트를 사용한 주문")
    void 주문_생성_성공_쿠폰_포인트_사용() {
        // given
        long initialBalance = 50000L;

        // 사용자 포인트 생성
        UserPoint userPoint = UserPoint.builder()
                .id(1L)
                .balance(initialBalance)
                .version(0L)
                .build();
        userPoint = userPointRepository.save(userPoint);
        long userId = userPoint.getId();

        // 상품 및 상품 옵션 생성
        Product product = Product.builder()
                .name("테스트 상품")
                .basePrice(30000L)
                .build();
        productRepository.save(product);

        ProductOption option = ProductOption.builder()
                .product(product)
                .productId(product.getId())
                .optionName("기본 옵션")
                .price(30000L)
                .quantity(10L)
                .build();
        productOptionRepository.save(option);

        // 쿠폰 생성 및 발급
        Coupon coupon = Coupon.builder()
                .couponName("할인 쿠폰")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(5000)
                .totalQuantity(100)
                .issuedQuantity(1)
                .limitPerUser(1)
                .duration(30)
                .minOrderValue(10000)
                .validFrom(LocalDate.now().minusDays(1))
                .validUntil(LocalDate.now().plusDays(30))
                .build();
        couponRepository.save(coupon);

        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .couponId(coupon.getId())
                .status(Status.ISSUED)
                .expiredAt(LocalDate.now().plusDays(30))
                .build();
        userCouponRepository.save(userCoupon);

        em.flush();
        em.clear();

        // when
        MakeOrderUseCase.Input input = MakeOrderUseCase.Input.of(
                userId,
                List.of(MakeOrderUseCase.Input.ItemInfo.of(product.getId(), option.getId(), 2L)),
                userCoupon.getId(),
                10000L
        );

        MakeOrderUseCase.Output output = makeOrderUseCase.execute(input);

        // then
        assertThat(output.orderId()).isNotNull();
        assertThat(output.totalAmount()).isEqualTo(60000L); // 30000 * 2
        assertThat(output.discountAmount()).isEqualTo(5000L);
        assertThat(output.usePointAmount()).isEqualTo(10000L);
        assertThat(output.finalAmount()).isEqualTo(45000L); // 60000 - 5000 - 10000

        // DB 검증
        Order savedOrder = orderRepository.findById(output.orderId()).orElseThrow();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PAID);

        // 재고 감소 검증
        ProductOption updatedOption = productOptionRepository.findById(option.getId()).orElseThrow();
        assertThat(updatedOption.getQuantity()).isEqualTo(8L); // 10 - 2

        // 포인트 차감 검증
        UserPoint updatedPoint = userPointRepository.findById(userId).orElseThrow();
        assertThat(updatedPoint.getBalance()).isEqualTo(40000L); // 50000 - 10000

        // 쿠폰 사용 처리 검증
        UserCoupon updatedCoupon = userCouponRepository.findById(userCoupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getStatus()).isEqualTo(Status.USED);

        // 주문 항목 검증
        List<OrderItem> orderItems = orderItemRepository.findAll();
        assertThat(orderItems).hasSize(1);
        assertThat(orderItems.get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("재고 부족 시 주문 실패 및 롤백")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void 주문_실패_재고_부족_롤백() {
        // given
        long userId = 2L;

        UserPoint userPoint = UserPoint.builder()
                .id(userId)
                .balance(100000L)
                .version(0L)
                .build();
        userPoint = userPointRepository.save(userPoint);
        userId = userPoint.getId();
        Product product = Product.builder()
                .name("재고 부족 상품")
                .basePrice(10000L)
                .build();
        productRepository.save(product);

        ProductOption option = ProductOption.builder()
                .product(product)
                .productId(product.getId())
                .optionName("부족 옵션")
                .price(10000L)
                .quantity(3L) // 재고 3개
                .build();
        productOptionRepository.save(option);


        // when
        MakeOrderUseCase.Input input = MakeOrderUseCase.Input.of(
                userId,
                List.of(MakeOrderUseCase.Input.ItemInfo.of(product.getId(), option.getId(), 5L)), // 5개 주문 (재고 부족)
                null,
                0L
        );

        // then
        Throwable throwable = catchThrowable(() -> makeOrderUseCase.execute(input));
        assertThat(throwable).isInstanceOf(ProductException.class);
        ProductException productException = (ProductException) throwable;
        assertThat(productException.getErrCode()).isEqualTo(ErrorCode.PRODUCT_NOT_ENOUGH);

        // 롤백 검증 - 재고가 원래대로 유지되어야 함
        em.clear();
        ProductOption checkOption = productOptionRepository.findById(option.getId()).orElseThrow();
        assertThat(checkOption.getQuantity()).isEqualTo(3L);

        // 주문이 생성되지 않아야 함
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).isEmpty();

        // 포인트가 차감되지 않아야 함
        UserPoint checkPoint = userPointRepository.findById(userId).orElseThrow();
        assertThat(checkPoint.getBalance()).isEqualTo(100000L);
    }

    @Test
    @DisplayName("포인트 부족 시 주문 실패 및 롤백")
    void 주문_실패_포인트_부족_롤백() {
        // given
        long userId = 3L;
        long userBalance = 5000L;

        long optionPrice = 20000L;

        UserPoint userPoint = UserPoint.builder()
                .id(userId)
                .balance(userBalance) // 포인트 5000
                .version(0L)
                .build();
        userPoint = userPointRepository.save(userPoint);
        userId = userPoint.getId();

        Product product = Product.builder()
                .name("일반 상품")
                .basePrice(20000L)
                .build();
        productRepository.save(product);

        ProductOption option = ProductOption.builder()
                .product(product)
                .productId(product.getId())
                .optionName("일반 옵션")
                .price(optionPrice)
                .quantity(10L)
                .build();
        productOptionRepository.save(option);

        em.flush();
        em.clear();

        // when
        MakeOrderUseCase.Input input = MakeOrderUseCase.Input.of(
                userId,
                List.of(MakeOrderUseCase.Input.ItemInfo.of(product.getId(), option.getId(), 1L)),
                null,
                10000L // 포인트 10000 사용 시도 (잔액 부족)
        );

        // then
        Throwable throwable = catchThrowable(() -> makeOrderUseCase.execute(input));
        assertThat(throwable).isInstanceOf(PointRangeException.class);
        PointRangeException ex =  (PointRangeException) throwable;
        assertThat(ex.getErrCode()).isEqualTo(ErrorCode.USER_POINT_NOT_ENOUGH);

        // 롤백 검증 - 재고가 차감되지 않아야 함
        em.clear();
        ProductOption checkOption = productOptionRepository.findById(option.getId()).orElseThrow();
        assertThat(checkOption.getQuantity()).isEqualTo(10L);

        // 주문이 생성되지 않아야 함
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).isEmpty();

        // 포인트가 차감되지 않아야 함
        UserPoint checkPoint = userPointRepository.findById(userId).orElseThrow();
        assertThat(checkPoint.getBalance()).isEqualTo(5000L);
    }

//    @Test
//    @DisplayName("여러 상품 주문 중 하나가 재고 부족 시 전체 롤백")
//    void 주문_실패_부분_재고_부족_전체_롤백() {
//        // given
//
//        UserPoint userPoint = UserPoint.builder()
//                .id(0l)
//                .balance(200000L)
//                .version(0L)
//                .build();
//        userPoint = userPointRepository.save(userPoint);
//        long userId = userPoint.getId();
//
//        Product product1 = Product.builder()
//                .name("상품 1")
//                .basePrice(10000L)
//                .build();
//        productRepository.save(product1);
//
//        ProductOption option1 = ProductOption.builder()
//                .product(product1)
//                .productId(product1.getId())
//                .optionName("옵션 1")
//                .price(10000L)
//                .quantity(10L) // 충분한 재고
//                .build();
//        productOptionRepository.save(option1);
//
//        Product product2 = Product.builder()
//                .name("상품 2")
//                .basePrice(20000L)
//                .build();
//        productRepository.save(product2);
//
//        ProductOption option2 = ProductOption.builder()
//                .product(product2)
//                .productId(product2.getId())
//                .optionName("옵션 2")
//                .price(20000L)
//                .quantity(2L) // 부족한 재고
//                .build();
//        productOptionRepository.save(option2);
//
//        em.flush();
//        em.clear();
//
//        // when
//        MakeOrderUseCase.Input input = MakeOrderUseCase.Input.of(
//                userId,
//                List.of(
//                        MakeOrderUseCase.Input.ItemInfo.of(product1.getId(), option1.getId(), 3L), // OK
//                        MakeOrderUseCase.Input.ItemInfo.of(product2.getId(), option2.getId(), 5L)  // 재고 부족
//                ),
//                null,
//                0L
//        );
//
//        // then
//        assertThatThrownBy(() -> makeOrderUseCase.execute(input))
//                .isInstanceOf(ProductException.class);
//
//        // 롤백 검증 - 모든 재고가 원래대로 유지되어야 함
//        em.clear();
//        ProductOption checkOption1 = productOptionRepository.findById(option1.getId()).orElseThrow();
//        ProductOption checkOption2 = productOptionRepository.findById(option2.getId()).orElseThrow();
//        assertThat(checkOption1.getQuantity()).isEqualTo(10L);
//        assertThat(checkOption2.getQuantity()).isEqualTo(2L);
//
//        // 주문이 생성되지 않아야 함
//        List<Order> orders = orderRepository.findAll();
//        assertThat(orders).isEmpty();
//    }
}
