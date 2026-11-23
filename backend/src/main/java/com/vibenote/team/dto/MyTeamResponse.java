package com.vibenote.team.dto;

import com.vibenote.common.config.ClockConfig;
import com.vibenote.team.TeamRole;
import com.vibenote.team.domain.MyTeam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 내 팀 목록 항목 (F-20a AC3). */
@Schema(description = "내 팀 목록 항목")
public record MyTeamResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "팀 ID", example = "12")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "팀 이름",
                        example = "스프링 스터디")
                String name,
        @Schema(nullable = true, description = "소개", example = "F-05 분석 결과를 같이 공부합니다")
                String description,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "내 역할",
                        allowableValues = {"OWNER", "MEMBER"},
                        example = "OWNER")
                TeamRole myRole,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "멤버 수 (팀장 포함)",
                        example = "3")
                int memberCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "팀장 닉네임",
                        example = "vibecoder")
                String ownerNickname,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "내가 가입한 시각",
                        example = "2026-11-24T10:00:00+09:00")
                OffsetDateTime joinedAt) {

    public static MyTeamResponse from(MyTeam row) {
        return new MyTeamResponse(
                row.getId(),
                row.getName(),
                row.getDescription(),
                row.getMyRole(),
                row.getMemberCount(),
                row.getOwnerNickname(),
                row.getJoinedAt().atZone(ClockConfig.ZONE).toOffsetDateTime());
    }
}
