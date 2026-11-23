package com.vibenote.concept.admin.dto;

import com.vibenote.common.Level;
import com.vibenote.common.config.ClockConfig;
import com.vibenote.concept.ConceptSource;
import com.vibenote.concept.ConceptStatus;
import com.vibenote.concept.domain.Concept;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 관리자 카드 목록 한 행 (F-13 AC1). 본문·예제는 넣지 않는다. */
@Schema(description = "관리자 카드 목록 항목")
public record AdminConceptSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "카드 ID", example = "91")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "개념 이름",
                        example = "낙관적 락")
                String name,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "카테고리 (java, spring, web, security, mybatis, vue)",
                        example = "spring")
                String category,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "난이도",
                        example = "SENIOR")
                Level difficulty,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "한 줄 요약",
                        example = "충돌이 드물다고 보고 저장 시점에 버전으로 충돌을 확인하는 동시성 제어 방식이다.")
                String summary,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "상태 (DRAFT 검수 대기, PUBLISHED 공개, HIDDEN 숨김)",
                        example = "DRAFT")
                ConceptStatus status,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "출처 (SEED 시드, AI 분석 초안, ADMIN 관리자 작성)",
                        example = "AI")
                ConceptSource source,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "찜 수", example = "0")
                int wishCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "평균 별점 (0.0~5.0, 리뷰가 없으면 0.0)",
                        example = "0.0")
                double avgRating,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "수정 시각",
                        example = "2026-11-20T14:20:00+09:00")
                OffsetDateTime updatedAt) {

    public static AdminConceptSummaryResponse from(Concept concept) {
        return new AdminConceptSummaryResponse(
                concept.getId(),
                concept.getName(),
                concept.getCategory(),
                concept.getDifficulty(),
                concept.getSummary(),
                concept.getStatus(),
                concept.getSource(),
                concept.getWishCount(),
                concept.getAvgRating().doubleValue(),
                concept.getUpdatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime());
    }
}
