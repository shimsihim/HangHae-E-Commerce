package io.hhplus.tdd.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 역직렬화 설정
        mapper.configure(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false  // 미지 필드 무시
        );

        // Java 8 날짜/시간 모듈 등록
        mapper.registerModule(new JavaTimeModule());

        // 날짜 직렬화 형식 설정
        mapper.configure(
            SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
            false  // ISO 8601 형식 사용
        );

        // null 값 처리 (선택사항)
        mapper.configure(
            SerializationFeature.WRITE_NULL_MAP_VALUES,
            false  // null 값은 출력하지 않음
        );

        return mapper;
    }
}
