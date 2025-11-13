package io.hhplus.tdd.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI(Swagger) 설정
 * - API 문서 자동 생성 및 테스트 환경 제공
 * - 접속 URL: http://localhost:8080/swagger-ui/index.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(servers());
    }

    private Info apiInfo() {
        return new Info()
                .title("포인트 관리 시스템 API")
                .description("""
                        ## 포인트 충전/사용/조회 시스템

                        ### 주요 기능
                        - 사용자 포인트 조회
                        - 포인트 충전 (최소 1,000원)
                        - 포인트 사용 (최소 100원)
                        - 포인트 거래 내역 조회

                        ### 제약 사항
                        - 최대 보유 포인트: 1,000,000,000원
                        - 동시성 제어: 사용자별 락 적용
                        - 데이터베이스 응답 지연: 200-300ms (네트워크 시뮬레이션)

                        ### 응답 형식
                        - 성공: 200 OK + 데이터
                        - 실패: 4xx/5xx + ErrorResponse
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("HH Plus TDD")
                        .email("support@hhplus.io")
                        .url("https://hhplus.io"));
    }

    private List<Server> servers() {
        return List.of(
                new Server()
                        .url("http://localhost:8080")
                        .description("로컬 개발 서버"),
                new Server()
                        .url("https://dev-api.hhplus.io")
                        .description("개발 서버"),
                new Server()
                        .url("https://api.hhplus.io")
                        .description("운영 서버")
        );
    }
}
