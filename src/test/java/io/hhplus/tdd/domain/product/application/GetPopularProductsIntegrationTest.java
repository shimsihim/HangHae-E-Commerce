package io.hhplus.tdd.domain.product.application;

import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.domain.model.OrderItem;
import io.hhplus.tdd.domain.order.domain.model.OrderStatus;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderItemRepository;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderRepository;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
@Slf4j
class GetPopularProductsIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private GetPopularProductsUseCase getPopularProductsUseCase;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("인기 상품 조회 - DataLoader로 생성된 실제 주문 데이터 기반 검증")
    void 인기_상품_조회_성공() {
        // given
        Product product1 = new Product(null,"1번제품" , "설명" , 14000L);
        Product product2 = new Product(null,"2번제품" , "설명" , 14000L);
        Product product3 = new Product(null,"3번제품" , "설명" , 14000L);
        Product product4 = new Product(null,"4번제품" , "설명" , 14000L);
        Product product5 = new Product(null,"5번제품" , "설명" , 14000L);
        Product product6 = new Product(null,"6번제품" , "설명" , 14000L);
        Product product7 = new Product(null,"7번제품" , "설명" , 14000L);
        Product product8 = new Product(null,"8번제품" , "설명" , 14000L);
        Product product9 = new Product(null,"9번제품" , "설명" , 14000L);
        Product product10 = new Product(null,"10번제품" , "설명" , 14000L);
        product1 = productRepository.save(product1);
        product2 = productRepository.save(product2);
        product3 = productRepository.save(product3);
        product4 = productRepository.save(product4);
        product5 = productRepository.save(product5);
        product6 = productRepository.save(product6);
        product7 = productRepository.save(product7);
        product8 = productRepository.save(product8);
        product9 = productRepository.save(product9);
        product10 = productRepository.save(product10);

        Order order =  new Order(1L, 1L , null , OrderStatus.PAID , 150000L , 20000L , 0L , 130000L , LocalDateTime.now());
        orderRepository.save(order);
        OrderItem orderItem = new OrderItem(1L , order , order.getId(), product1.getId() , 1L , 1 , 15000L , 15000L);
        OrderItem orderItem2 = new OrderItem(2L , order , order.getId(), product2.getId() , 1L , 2 , 15000L , 15000L);
        OrderItem orderItem3 = new OrderItem(3L , order , order.getId(), product3.getId() , 1L , 3 , 15000L , 15000L);
        OrderItem orderItem4 = new OrderItem(4L , order , order.getId(), product4.getId() , 1L , 4 , 15000L , 15000L);
        OrderItem orderItem5 = new OrderItem(5L , order , order.getId(), product5.getId() , 1L , 5 , 15000L , 15000L);
        OrderItem orderItem6 = new OrderItem(6L , order , order.getId(), product6.getId() , 1L , 6 , 15000L , 15000L);
        OrderItem orderItem7 = new OrderItem(7L , order , order.getId(), product7.getId() , 1L , 7 , 15000L , 15000L);
        OrderItem orderItem8 = new OrderItem(8L , order , order.getId(), product8.getId() , 1L , 8 , 15000L , 15000L);
        OrderItem orderItem9 = new OrderItem(9L , order , order.getId(), product9.getId() , 1L , 9 , 15000L , 15000L);
        OrderItem orderItem10 = new OrderItem(10L , order , order.getId(), product10.getId() , 1L , 1 , 15000L , 15000L);
        OrderItem orderItem11 = new OrderItem(11L , order , order.getId(), product10.getId() , 1L , 10 , 15000L , 15000L);
        orderItemRepository.save(orderItem);
        orderItemRepository.save(orderItem2);
        orderItemRepository.save(orderItem3);
        orderItemRepository.save(orderItem4);
        orderItemRepository.save(orderItem5);
        orderItemRepository.save(orderItem6);
        orderItemRepository.save(orderItem7);
        orderItemRepository.save(orderItem8);
        orderItemRepository.save(orderItem9);
        orderItemRepository.save(orderItem10);
        orderItemRepository.save(orderItem11);

        em.flush();

        // when
        List<GetPopularProductsUseCase.Output> popularProducts = getPopularProductsUseCase.execute();

        // then
        popularProducts.stream().forEach(System.out::println);

        // 판매량 내림차순 검증
        for (int i = 0; i < popularProducts.size() - 1; i++) {
            assertThat(popularProducts.get(i).totalSalesQuantity())
                    .isGreaterThanOrEqualTo(popularProducts.get(i + 1).totalSalesQuantity());
        }
    }

}
