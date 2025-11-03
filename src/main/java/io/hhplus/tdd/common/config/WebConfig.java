package io.hhplus.tdd.common.config;

import io.hhplus.tdd.common.aop.AuthenticationArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthenticationArgumentResolver authenticationArgumentResolver;

    WebConfig(AuthenticationArgumentResolver authenticationArgumentResolver){
        this.authenticationArgumentResolver = authenticationArgumentResolver;
    }


    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // 구현한 Resolver를 Spring MVC Argument Resolver 목록에 추가
        resolvers.add(authenticationArgumentResolver);
    }
}