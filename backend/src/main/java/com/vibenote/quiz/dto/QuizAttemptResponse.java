package com.vibenote.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/** 풀이 채점 결과 (F-14 AC6·AC7). */
@Schema(description = "풀이 채점 결과")
public record QuizAttemptResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "풀이 ID", example = "16")
                long attemptId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "맞힌 문항 수", example = "1")
                int score,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "문항 수", example = "2")
                int total,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "제출 시각",
                        example = "2026-11-19T17:42:05+09:00")
                OffsetDateTime createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "문항별 결과 (no 순서)")
                List<QuizResultItem> results,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "틀린 문항의 공개 개념 중 새로 넣은 복습 계획 (conceptId 오름차순). 없으면 []")
                List<ReviewAddedItem> reviewAdded) {}
