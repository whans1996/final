package com.vibenote.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 분석의 퀴즈 (F-14 AC5). */
@Schema(description = "분석의 퀴즈")
public record QuizResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "퀴즈 ID", example = "3")
                long quizId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "분석 ID", example = "7")
                long analysisId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "문항 5~10개 (no 순서)")
                List<QuizQuestionResponse> questions,
        @Schema(description = "내 가장 최근 풀이. 풀이가 없으면 null", nullable = true)
                LastAttemptResponse lastAttempt) {}
