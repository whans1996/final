package com.vibenote.concept.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 리뷰 작성·수정 결과: 저장된 리뷰와 다시 계산한 카드 평점 (F-11 AC3·AC8). */
@Schema(description = "리뷰 저장 결과")
public record ConceptReviewSaveResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "저장된 리뷰")
                ConceptReviewResponse review,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "평균 별점 (소수 1자리)",
                        example = "4.3")
                double avgRating,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "리뷰 수", example = "13")
                long reviewCount) {

    public static ConceptReviewSaveResponse of(
            ConceptReviewResponse review, ConceptRatingResponse rating) {
        return new ConceptReviewSaveResponse(review, rating.avgRating(), rating.reviewCount());
    }
}
