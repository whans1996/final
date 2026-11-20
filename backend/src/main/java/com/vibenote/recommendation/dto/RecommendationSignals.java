package com.vibenote.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 추천 점수의 근거가 된 신호 (F-16 1.1절). */
@Schema(description = "추천 신호")
public record RecommendationSignals(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "내 퀴즈 오답 중 이 카드를 다룬 문항 수 (상한 없는 실제 개수. 점수에는 3까지만 넣는다)",
                        example = "2")
                int wrongCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "삭제되지 않은 내 분석에 이 카드가 나왔는지",
                        example = "true")
                boolean inMyCode,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "내가 찜한 카드 중 같은 카테고리가 있는지",
                        example = "false")
                boolean wishedCategory,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "회원 수준과 카드 난이도의 적합도 (같음 1, 한 단계 차이 0, 두 단계 차이 -1)",
                        example = "1",
                        allowableValues = {"-1", "0", "1"})
                int levelFit) {}
