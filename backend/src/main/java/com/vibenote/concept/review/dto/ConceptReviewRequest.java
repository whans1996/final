package com.vibenote.concept.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 리뷰 작성·수정 요청 (F-11 AC3·AC5·AC8). 내용은 앞뒤 공백을 뺀 값으로 검증하고 저장한다. */
@Schema(description = "리뷰 작성·수정 요청")
public record ConceptReviewRequest(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "별점 (정수 1~5)",
                        minimum = "1",
                        maximum = "5",
                        example = "4")
                @NotNull(message = "별점을 선택해 주세요.")
                @Min(value = 1, message = "별점은 1~5 사이여야 합니다.")
                @Max(value = 5, message = "별점은 1~5 사이여야 합니다.")
                Integer rating,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "리뷰 내용 (앞뒤 공백 제외 1~200자)",
                        maxLength = 200,
                        example = "예제가 명확했어요")
                @NotBlank(message = "리뷰 내용을 입력해 주세요.")
                @Size(max = 200, message = "리뷰는 200자 이하로 입력해 주세요.")
                String content) {

    public ConceptReviewRequest {
        if (content != null) {
            content = content.strip();
        }
    }
}
