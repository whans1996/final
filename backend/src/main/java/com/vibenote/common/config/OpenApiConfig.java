package com.vibenote.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(OpenAPI) 설정. 모든 API에 JWT Bearer 보안을 기본 적용하고, 공개 API는 {@code @SecurityRequirements()}로
 * 해제한다.
 */
@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI vibeNoteOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("VibeNote API")
                                .version("v1")
                                .description(
                                        "AI가 쓴 코드를 수준별 설명 → 개념 카드 → 퀴즈·복습으로 이해하게 돕는 학습 플랫폼 바이브노트의"
                                                + " REST API"))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        SECURITY_SCHEME,
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description("로그인으로 받은 Access 토큰")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}
