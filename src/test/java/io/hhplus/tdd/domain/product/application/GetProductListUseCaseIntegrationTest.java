package io.hhplus.tdd.domain.product.application;

import io.hhplus.tdd.common.cache.MultiGetCacheService;
import io.hhplus.tdd.common.cache.RedisKey;
import io.hhplus.tdd.domain.ContainerIntegrationTest;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.model.ProductCache;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@DisplayName("상품 리스트 조회 - 개별 상품 캐싱 전략 테스트")
class GetProductListUseCaseIntegrationTest extends ContainerIntegrationTest {

    @Autowired
    private GetProductListUseCase getProductListUseCase;

    @SpyBean
    private ProductRepository productRepository;

    @Autowired
    private MultiGetCacheService multiGetCacheService;

    private List<Long> productIds;

    @BeforeEach
    void setUp() {
        Product product1 = Product.builder().name("testproduct1").basePrice(10000L).build();
        Product product2 = Product.builder().name("testproduct2").basePrice(20000L).build();
        Product product3 = Product.builder().name("testproduct3").basePrice(30000L).build();
        Product product4 = Product.builder().name("testproduct4").basePrice(40000L).build();
        Product product5 = Product.builder().name("testproduct5").basePrice(50000L).build();

        List<Product> savedProducts = productRepository.saveAll(List.of(product1, product2, product3, product4, product5));
        productIds = savedProducts.stream().map(Product::getId).toList();

        // setup 과정에서의 saveAll 호출 기록이 테스트에 영향을 주지 않도록 초기화
        clearInvocations(productRepository);
    }

    @AfterEach
    void tearDown() {
        if (productIds != null && !productIds.isEmpty()) {
            multiGetCacheService.deleteByIds(RedisKey.CACHE_PRODUCT_BY_ID, productIds);
        }
        productRepository.deleteAllById(productIds);
    }

    @Test
    void 첫_조회시_DB에서조회_두번째_캐시_조회() {
        // given
        GetProductListUseCase.Input input = new GetProductListUseCase.Input(0, 3);

        // when첫 번째 조회
        List<GetProductListUseCase.Output> firstResult = getProductListUseCase.execute(input);

        // then
        assertThat(firstResult).hasSize(3);
        // DB 조회가 최소 1번 이상 발생했는지
        verify(productRepository, atLeastOnce()).findAllById(any());

        // 호출 기록 초기화 (첫 번째 호출 카운트를 지움)
        clearInvocations(productRepository);

        // when 두 번째 조회 캐시에서 조회하기
        List<GetProductListUseCase.Output> secondResult = getProductListUseCase.execute(input);

        // then
        assertThat(secondResult).hasSize(3);
        assertThat(secondResult.get(0).name()).isEqualTo(firstResult.get(0).name());

        // DB 조회가 0번 발생했는지
        verify(productRepository, times(0)).findAllById(any());
    }

    @Test
    void 다른_페이지_조회시_다른_상품_반환() {
        // given
        GetProductListUseCase.Input firstPageInput = new GetProductListUseCase.Input(0, 3);
        GetProductListUseCase.Input secondPageInput = new GetProductListUseCase.Input(1, 3);

        // when - 첫 번째 페이지 조회
        List<GetProductListUseCase.Output> firstPageResult = getProductListUseCase.execute(firstPageInput);

        // when - 두 번째 페이지 조회
        List<GetProductListUseCase.Output> secondPageResult = getProductListUseCase.execute(secondPageInput);

        // then
        assertThat(firstPageResult).hasSize(3);
        assertThat(firstPageResult.get(0).name()).isEqualTo("testproduct1");
        assertThat(firstPageResult.get(1).name()).isEqualTo("testproduct2");
        assertThat(firstPageResult.get(2).name()).isEqualTo("testproduct3");

        assertThat(secondPageResult).hasSize(2);
        assertThat(secondPageResult.get(0).name()).isEqualTo("testproduct4");
        assertThat(secondPageResult.get(1).name()).isEqualTo("testproduct5");
    }

    @Test
    void 재조회시_동일_데이터_반환_캐시() {
        // given
        GetProductListUseCase.Input page1 = new GetProductListUseCase.Input(0, 2);
        GetProductListUseCase.Input page2 = new GetProductListUseCase.Input(1, 2);

        // when
        List<GetProductListUseCase.Output> firstPage = getProductListUseCase.execute(page1);
        List<GetProductListUseCase.Output> secondPage = getProductListUseCase.execute(page2);

        // 다시 첫 페이지 조회 (캐시에서 조회)
        List<GetProductListUseCase.Output> firstPageAgain = getProductListUseCase.execute(page1);

        // then
        assertThat(firstPage).hasSize(2);
        assertThat(firstPage.get(0).name()).isEqualTo("testproduct1");
        assertThat(firstPage.get(1).name()).isEqualTo("testproduct2");

        assertThat(secondPage).hasSize(2);
        assertThat(secondPage.get(0).name()).isEqualTo("testproduct3");
        assertThat(secondPage.get(1).name()).isEqualTo("testproduct4");

        // 캐시에서 조회한 결과도 동일해야 함
        assertThat(firstPageAgain).hasSize(2);
        assertThat(firstPageAgain.get(0).name()).isEqualTo("testproduct1");
        assertThat(firstPageAgain.get(1).name()).isEqualTo("testproduct2");
    }
}
