package com.vibenote.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 학습 통계 (F-03). 프로필에서 누구나 볼 수 있다. */
@Schema(description = "학습 통계")
public record UserStatsResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "완료된 분석 수 (삭제한 분석 제외)",
                        example = "3")
                long analysisCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "학습한 카드 수: 퀴즈에서 맞힌 카드와 복습을 완료한 카드 (중복 제외)",
                        example = "12")
                long learnedConceptCount,
        @Schema(description = "퀴즈 정답률(%, 정수). 푼 퀴즈가 없으면 null", example = "75", nullable = true)
                Integer quizAccuracy) {}
