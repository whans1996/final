package com.vibenote.auth.dto;

import com.vibenote.common.Level;
import io.swagger.v3.oas.annotations.media.Schema;

/** 로그인·재발급 응답 (F-02). Refresh 토큰은 본문이 아니라 HttpOnly 쿠키 refresh_token으로 보낸다. */
@Schema(description = "로그인·토큰 재발급 응답")
public record LoginResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "Access 토큰 (Authorization: Bearer)",
                        example = "eyJhbGciOiJIUzI1NiJ9...")
                String accessToken,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "Access 토큰 유효 시간(초)",
                        example = "1800")
                long expiresIn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "로그인한 회원")
                LoginUser user) {

    @Schema(description = "로그인한 회원 정보")
    public record LoginUser(
            @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            description = "회원 ID",
                            example = "1")
                    long id,
            @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            description = "닉네임",
                            example = "코딩초보")
                    String nickname,
            @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            description = "학습 수준",
                            example = "BEGINNER")
                    Level level,
            @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            description = "권한",
                            example = "USER",
                            allowableValues = {"USER", "ADMIN"})
                    String role) {}
}
