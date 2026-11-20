package com.vibenote.recommendation.dto;

import com.vibenote.concept.dto.ConceptSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/** 추천 카드 하나 (F-16 3.1절). */
@Schema(description = "추천 카드")
public record RecommendationItemResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "카드 요약")
                ConceptSummaryResponse concept,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "추천 점수 (min(오답,3)×3 + 내 코드 2 + 찜한 분야 1 + 수준 적합도)",
                        example = "10")
                int score,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "점수의 근거 신호")
                RecommendationSignals signals,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "추천 이유 한 줄 (100자 이하)",
                        example = "퀴즈에서 두 번 헷갈린 JWT 인증이 내 코드의 로그인 필터에 쓰였으니 지금 다시 보면 좋아요.")
                String reason,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "추천 이유 출처",
                        example = "AI")
                ReasonSource reasonSource) {}
