package com.vibenote.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Locale;

/** 로그인 요청 (F-02). email은 가입 때와 같이 trim·소문자로 정규화한다. */
@Schema(description = "로그인 요청")
public record LoginRequest(
        @Schema(description = "이메일", example = "user@example.com")
                @NotBlank(message = "이메일을 입력하세요.")
                String email,
        @Schema(description = "비밀번호", example = "vibe1234") @NotBlank(message = "비밀번호를 입력하세요.")
                String password) {

    public LoginRequest {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
