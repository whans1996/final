package com.vibenote.quiz.dto;

import com.vibenote.quiz.QuizStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/** 퀴즈 재생성 접수 (L-1a AC5). 결과는 {@code GET /api/analyses/{id}/status}의 quizStatus로 확인한다. */
@Schema(description = "퀴즈 재생성 접수")
public record QuizRegenerationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "분석 ID", example = "42")
                long analysisId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "퀴즈 상태 (접수 직후 항상 GENERATING)",
                        example = "GENERATING")
                QuizStatus quizStatus,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "이번 요청을 뺀 남은 재생성 횟수",
                        example = "2",
                        minimum = "0")
                int quizRegenerationsLeft) {}
