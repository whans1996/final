package com.vibenote.team.dto;

import com.vibenote.team.TeamChallengeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** 팀 목표 하나와 지금 멤버의 진행 (F-20c AC1·AC4~AC6). 달성은 조회할 때마다 계산한다. */
@Schema(description = "팀 목표")
public record TeamChallengeResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "목표 ID", example = "7")
                long challengeId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "분석 ID", example = "42")
                long analysisId,
        @Schema(description = "분석 제목 (없으면 null)", example = "spring-realworld") String title,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "프로젝트 이름",
                        example = "spring-realworld")
                String projectName,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "시작일 (Asia/Seoul, 포함)",
                        example = "2026-11-24")
                LocalDate startDate,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "종료일 (Asia/Seoul, 포함)",
                        example = "2026-11-30")
                LocalDate endDate,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "목표 정답률 (%)",
                        example = "80")
                int targetRate,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "상태",
                        allowableValues = {"UPCOMING", "ONGOING", "ENDED"},
                        example = "ONGOING")
                TeamChallengeStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "지금 멤버 수", example = "3")
                int memberCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "기간 안에 푼 멤버 수",
                        example = "2")
                int solvedMemberCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "달성한 멤버 수",
                        example = "1")
                int achievedMemberCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "팀 달성 (지금 멤버 전원 달성)",
                        example = "false")
                boolean achieved,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "멤버별 진행 (팀장 먼저, 가입 순)")
                List<TeamChallengeMemberResponse> members,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "만든 시각",
                        example = "2026-11-24T10:00:00+09:00")
                OffsetDateTime createdAt) {}
