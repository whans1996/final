package com.vibenote.quiz.dto;

import com.vibenote.common.config.ClockConfig;
import com.vibenote.quiz.domain.QuizAttempt;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 내 풀이 목록의 한 회차 (L-1b AC1). */
@Schema(description = "내 풀이 한 회차 요약")
public record QuizAttemptSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "풀이 ID", example = "31")
                long attemptId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "맞힌 문항 수", example = "5")
                int score,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "문항 수", example = "6")
                int total,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "제출 시각",
                        example = "2026-11-30T21:14:03+09:00")
                OffsetDateTime createdAt) {

    public static QuizAttemptSummaryResponse from(QuizAttempt attempt) {
        return new QuizAttemptSummaryResponse(
                attempt.getId(),
                attempt.getScore(),
                attempt.getTotal(),
                attempt.getCreatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime());
    }
}
