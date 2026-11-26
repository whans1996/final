package com.vibenote.user.dto;

import com.vibenote.common.Level;
import com.vibenote.common.config.ClockConfig;
import com.vibenote.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 공개 프로필 (F-03). 누구나 볼 수 있으므로 email·role은 넣지 않는다. */
@Schema(description = "공개 프로필")
public record UserProfileResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "회원 ID", example = "1")
                long id,
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
                        description = "가입 시각",
                        example = "2026-11-18T10:30:00+09:00")
                OffsetDateTime createdAt) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getBio(),
                user.getLevel(),
                user.getCreatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime());
    }
}
