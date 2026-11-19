package com.vibenote.concept.dto;

import com.vibenote.common.Level;
import com.vibenote.concept.domain.Concept;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 카드 요약. 카드 목록(F-09), 분석의 개념(F-08), 찜 목록(F-12), 복습(F-15), 추천(F-16)이 같은 모양을 쓴다 (F-00b AC4).
 * 본문·예제·상태는 넣지 않는다.
 */
@Schema(description = "개념 카드 요약")
public record ConceptSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "카드 ID", example = "12")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "개념 이름",
                        example = "JWT 인증")
                String name,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "카테고리 (java, spring, security, mybatis, web, vue)",
                        example = "security")
                String category,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "난이도",
                        example = "JUNIOR")
                Level difficulty,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "한 줄 요약",
                        example = "서버가 서명한 토큰으로 요청한 사람을 확인하는 방식이다.")
                String summary,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "평균 별점 (0.0~5.0, 소수 1자리. 리뷰가 없으면 0.0)",
                        example = "4.5")
                double avgRating,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "찜 수", example = "12")
                int wishCount) {

    public static ConceptSummaryResponse from(Concept concept) {
        return new ConceptSummaryResponse(
                concept.getId(),
                concept.getName(),
                concept.getCategory(),
                concept.getDifficulty(),
                concept.getSummary(),
                concept.getAvgRating().doubleValue(),
                concept.getWishCount());
    }
}
