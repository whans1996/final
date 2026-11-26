package com.vibenote.follow.dto;

import com.vibenote.common.Level;
import com.vibenote.common.config.ClockConfig;
import com.vibenote.follow.domain.FollowUser;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 팔로워·팔로잉 목록 항목 (F-04 AC5). */
@Schema(description = "팔로워·팔로잉 목록 항목")
public record FollowUserResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "회원 ID", example = "7")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "닉네임",
                        example = "java러버")
                String nickname,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "학습 수준",
                        example = "JUNIOR")
                Level level,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "팔로우한 시각 (follows.created_at)",
                        example = "2026-11-20T15:02:11+09:00")
                OffsetDateTime followedAt) {

    public static FollowUserResponse from(FollowUser row) {
        return new FollowUserResponse(
                row.getId(),
                row.getNickname(),
                row.getLevel(),
                row.getFollowedAt().atZone(ClockConfig.ZONE).toOffsetDateTime());
    }
}
