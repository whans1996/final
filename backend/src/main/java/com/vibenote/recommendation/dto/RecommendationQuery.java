package com.vibenote.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** 추천 요청 {@code ?size=5}. 정수가 아니거나 범위 밖이면 COMMON_400({@code details.size}). */
@Schema(description = "추천 요청")
public record RecommendationQuery(
        @Schema(description = "추천 개수 (1~10)", example = "5", defaultValue = "5")
                @Min(value = 1, message = "size는 1 이상이어야 합니다.")
                @Max(value = 10, message = "size는 10 이하여야 합니다.")
                Integer size) {

    public static final int DEFAULT_SIZE = 5;

    public RecommendationQuery {
        if (size == null) size = DEFAULT_SIZE;
    }
}
