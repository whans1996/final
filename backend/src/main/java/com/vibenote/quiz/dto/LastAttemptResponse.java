package com.vibenote.quiz.dto;

import com.vibenote.common.config.ClockConfig;
import com.vibenote.quiz.domain.QuizAttempt;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 내 가장 최근 풀이 요약 (F-14 AC5). */
@Schema(description = "내 가장 최근 풀이")
public record LastAttemptResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "풀이 ID", example = "15")
                long attemptId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "맞힌 문항 수", example = "5")
                int score,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "문항 수", example = "6")
                int total,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "제출 시각",
                        example = "2026-11-19T17:30:12+09:00")
                OffsetDateTime createdAt) {

    public static LastAttemptResponse from(QuizAttempt attempt) {
        return new LastAttemptResponse(
                attempt.getId(),
                attempt.getScore(),
                attempt.getTotal(),
                attempt.getCreatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime());
    }
}
