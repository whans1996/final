package com.vibenote.concept.review.dto;

import com.vibenote.common.config.ClockConfig;
import com.vibenote.concept.review.domain.ConceptReview;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 리뷰 한 건 (F-11 AC1·AC2·AC7). 탈퇴한 작성자는 id를 숨기고 닉네임을 "탈퇴한 사용자"로 바꾼다. */
@Schema(description = "카드 리뷰")
public record ConceptReviewResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "리뷰 ID", example = "301")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "별점 (1~5)",
                        example = "5")
                int rating,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "리뷰 내용",
                        example = "토큰 서명과 만료 검증 순서가 한눈에 들어왔어요")
                String content,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "작성자 ID (탈퇴한 회원이면 null)",
                        example = "7")
                Long authorId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "작성자 현재 닉네임 (탈퇴한 회원이면 \"탈퇴한 사용자\")",
                        example = "코딩하는감자")
                String authorNickname,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "작성 시각",
                        example = "2026-12-02T21:14:03+09:00")
                OffsetDateTime createdAt) {

    public static final String WITHDRAWN_NICKNAME = "탈퇴한 사용자";

    public static ConceptReviewResponse from(ConceptReview row) {
        boolean withdrawn = row.isAuthorWithdrawn();
        return new ConceptReviewResponse(
                row.getId(),
                row.getRating(),
                row.getContent(),
                withdrawn ? null : row.getUserId(),
                withdrawn ? WITHDRAWN_NICKNAME : row.getAuthorNickname(),
                row.getCreatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime());
    }
}
