package com.vibenote.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 맞춤 추천 목록 (F-16 3.1절). */
@Schema(description = "맞춤 추천 목록")
public record RecommendationListResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "추천 카드 (점수 → 찜 수 → 평점 → id 순, 최대 size개)")
                List<RecommendationItemResponse> items) {}
