package com.vibenote.common.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 업로드 파일 정적 제공 (F-24 AC10). {@code {app.upload-dir}}에는 분석 ZIP 임시 파일({@code analysis-tmp/})도 있으므로 폴더
 * 전체가 아니라 챌린지 이미지 폴더만 연다. SecurityConfig의 {@code /uploads/**} 공개 규칙은 그대로 두고, 핸들러가 없는 경로는 404다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String challengeImageLocation;

    public WebConfig(@Value("${app.upload-dir}") String uploadDir) {
        String location =
                Path.of(uploadDir, "challenges").toAbsolutePath().normalize().toUri().toString();
        this.challengeImageLocation = location.endsWith("/") ? location : location + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/challenges/**")
                .addResourceLocations(challengeImageLocation);
    }
}
