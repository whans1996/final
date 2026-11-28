package com.vibenote.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/** 지난 풀이 하나의 문항별 결과 (L-1b AC3). 선택지·위치는 저장된 순서 기준이다. */
@Schema(description = "지난 풀이 상세")
public record QuizAttemptDetailResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "풀이 ID", example = "16")
                long attemptId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "맞힌 문항 수", example = "3")
                int score,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "문항 수", example = "6")
                int total,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "제출 시각",
                        example = "2026-11-29T17:42:05+09:00")
                OffsetDateTime createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "문항별 결과 (no 순서)")
                List<QuizResultItem> results) {}
