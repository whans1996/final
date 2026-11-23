package com.vibenote.team.dto;

import com.vibenote.common.Level;
import com.vibenote.common.config.ClockConfig;
import com.vibenote.team.TeamRole;
import com.vibenote.team.domain.TeamMember;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 팀 상세의 멤버 항목 (F-20a AC4). */
@Schema(description = "팀 멤버")
public record TeamMemberResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "회원 ID", example = "7")
                long userId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "닉네임",
                        example = "learner")
                String nickname,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "학습 수준",
                        example = "BEGINNER")
                Level level,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "역할",
                        allowableValues = {"OWNER", "MEMBER"},
                        example = "MEMBER")
                TeamRole role,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "가입 시각",
                        example = "2026-11-24T11:30:00+09:00")
                OffsetDateTime joinedAt) {

    public static TeamMemberResponse from(TeamMember row) {
        return new TeamMemberResponse(
                row.getUserId(),
                row.getNickname(),
                row.getLevel(),
                row.getRole(),
                row.getJoinedAt().atZone(ClockConfig.ZONE).toOffsetDateTime());
    }
}
