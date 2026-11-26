package com.vibenote.user.dto;

import com.vibenote.common.Level;
import com.vibenote.common.config.ClockConfig;
import com.vibenote.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 내 프로필 (F-03). 본인에게만 주므로 email·role을 포함한다. */
@Schema(description = "내 프로필")
public record MyProfileResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "회원 ID", example = "1")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "이메일",
                        example = "user@example.com")
                String email,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "닉네임", example = "코딩초보")
                String nickname,
        @Schema(
                        description = "소개. 없으면 null",
                        example = "AI로 만든 Spring 프로젝트를 공부하고 있어요.",
                        nullable = true)
                String bio,
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
                String role,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "가입 시각",
                        example = "2026-11-18T10:30:00+09:00")
                OffsetDateTime createdAt) {

    public static MyProfileResponse from(User user) {
        return new MyProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getBio(),
                user.getLevel(),
                user.getRole(),
                user.getCreatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime());
    }
}
