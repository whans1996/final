package com.vibenote.team.dto;

import com.vibenote.common.Level;
import com.vibenote.common.config.ClockConfig;
import com.vibenote.quiz.dto.LastAttemptResponse;
import com.vibenote.team.domain.TeamAnalysis;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 팀 공유 분석 목록 항목 (F-20b AC3). */
@Schema(description = "팀 공유 분석")
public record TeamAnalysisResponse(
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
                        description = "설명 수준",
                        example = "JUNIOR")
                Level level,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "공유한 사람")
                SharedByResponse sharedBy,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "공유 시각",
                        example = "2026-11-25T14:20:00+09:00")
                OffsetDateTime sharedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "퀴즈 문항 수", example = "6")
                int questionCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "지금 멤버 중 한 번 이상 푼 사람 수",
                        example = "2")
                int solvedMemberCount,
        @Schema(description = "내 가장 최근 풀이 (안 풀었으면 null)") LastAttemptResponse myLastAttempt,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "내가 공유한 사람이거나 팀장이면 true",
                        example = "true")
                boolean canUnshare) {

    public static TeamAnalysisResponse of(TeamAnalysis row, boolean canUnshare) {
        LastAttemptResponse myLastAttempt =
                row.getMyAttemptId() == null
                        ? null
                        : new LastAttemptResponse(
                                row.getMyAttemptId(),
                                row.getMyScore(),
                                row.getMyTotal(),
                                row.getMyCreatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime());
        return new TeamAnalysisResponse(
                row.getAnalysisId(),
                row.getTitle(),
                row.getProjectName(),
                row.getLevel(),
                new SharedByResponse(row.getSharedByUserId(), row.getSharedByNickname()),
                row.getSharedAt().atZone(ClockConfig.ZONE).toOffsetDateTime(),
                row.getQuestionCount(),
                row.getSolvedMemberCount(),
                myLastAttempt,
                canUnshare);
    }
}
