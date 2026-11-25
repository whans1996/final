package com.vibenote.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.ObjectMapper;

/**
 * 통합 테스트 공통 부모. Testcontainers MySQL 8 싱글톤 컨테이너 + Flyway + Mock LLM.
 *
 * <p>컨테이너는 JVM당 한 번만 띄우고 모든 테스트 클래스가 함께 쓴다(Spring 컨텍스트 캐시도 공유). 테스트끼리 데이터가 섞이지 않게
 * {@code @Transactional} 롤백이나 {@code @Sql} 정리 스크립트를 쓴다.
 *
 * <p>{@code properties}는 설정 파일과 저장소 루트 {@code .env}보다 우선하므로, 개발자 PC 설정과 상관없이 항상 mock LLM과 테스트용 JWT
 * 키를 쓴다.
 */
@SpringBootTest(
        properties = {
            "llm.provider=mock",
            "app.jwt.secret=test-only-jwt-secret-for-integration-tests-0123456789",
            "app.upload-dir=build/test-uploads",
            // 고정 시각 테스트와 겹치지 않게 일괄 작업은 끄고 테스트가 직접 부른다 (#146)
            "challenge.recount-cron=-"
        })
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    protected static final MySQLContainer MYSQL =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("vibenote")
                    .withUsername("vibenote")
                    .withPassword("vibenote")
                    .withCommand(
                            "--character-set-server=utf8mb4",
                            "--collation-server=utf8mb4_0900_ai_ci",
                            // 배포·CI MySQL처럼 UTC로 띄운다. 세션 시간대는 앱이 연결마다 +09:00으로 맞춘다 (#17)
                            "--default-time-zone=+00:00");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired protected MockMvc mockMvc;

    @Autowired protected ObjectMapper objectMapper;
}
