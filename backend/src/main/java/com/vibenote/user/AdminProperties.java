package com.vibenote.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 관리자 설정 ({@code app.admin.*}, F-00b AC6).
 *
 * @param bootstrapEmail 기동할 때 ADMIN으로 승격할 가입 회원 이메일. 비어 있으면 아무것도 하지 않는다. 저장소에는 값을 두지 않고 환경변수 {@code
 *     ADMIN_BOOTSTRAP_EMAIL}로 받는다
 */
@ConfigurationProperties("app.admin")
public record AdminProperties(String bootstrapEmail) {}
