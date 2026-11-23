package com.vibenote.team.dto;

import com.vibenote.common.config.ClockConfig;
import com.vibenote.team.TeamRole;
import com.vibenote.team.domain.Team;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/** 팀 상세 (F-20a AC4·AC7). 초대 코드는 팀장에게만 값이 있다. */
@Schema(description = "팀 상세")
public record TeamDetailResponse(
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
                        description = "요청한 사람의 역할",
                        allowableValues = {"OWNER", "MEMBER"},
                        example = "OWNER")
                TeamRole myRole,
        @Schema(nullable = true, description = "초대 코드. 팀원에게는 null", example = "K7M2QX9P")
                String inviteCode,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "멤버 수 (팀장 포함)",
                        example = "3")
                int memberCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "팀 최대 인원",
                        example = "10")
                int maxMembers,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "만든 시각",
                        example = "2026-11-24T10:00:00+09:00")
                OffsetDateTime createdAt,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "멤버 (팀장이 맨 앞, 나머지는 가입 순)")
                List<TeamMemberResponse> members) {

    public static TeamDetailResponse of(
            Team team, TeamRole myRole, int maxMembers, List<TeamMemberResponse> members) {
        return new TeamDetailResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                myRole,
                myRole == TeamRole.OWNER ? team.getInviteCode() : null,
                members.size(),
                maxMembers,
                team.getCreatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime(),
                members);
    }
}
