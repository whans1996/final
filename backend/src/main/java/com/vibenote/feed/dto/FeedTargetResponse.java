package com.vibenote.feed.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 피드 항목의 대상 (F-04 AC6). */
@Schema(
        description =
                "활동 대상. id는 링크할 대상의 id (REVIEW·WISH는 카드, ANALYSIS는 분석, POST는 글, CHALLENGE_*는 챌린지)")
public record FeedTargetResponse(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "링크할 대상 ID. REVIEW는 리뷰 id가 아니라 카드 id",
                        example = "12")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "대상 이름. ANALYSIS는 분석 제목, 없으면 프로젝트명, 둘 다 없으면 \"제목 없는 분석\"",
                        example = "JWT 기반 인증")
                String title,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "REVIEW의 별점(1~5). 나머지 종류는 null",
                        example = "5")
                Integer rating) {}
