package com.vibenote.concept.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 리뷰를 쓰거나 고치거나 지운 뒤의 카드 평점 (F-11 AC4·AC9). 평균은 소수 1자리 반올림, 리뷰가 없으면 0.0. */
@Schema(description = "카드 평점과 리뷰 수")
public record ConceptRatingResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "평균 별점 (소수 1자리, 리뷰가 없으면 0.0)",
                        example = "4.2")
                double avgRating,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "리뷰 수", example = "12")
                long reviewCount) {}
