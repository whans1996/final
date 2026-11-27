package com.vibenote.concept.admin.dto;

import com.vibenote.common.Level;
import com.vibenote.common.config.ClockConfig;
import com.vibenote.concept.ConceptSource;
import com.vibenote.concept.ConceptStatus;
import com.vibenote.concept.domain.Concept;
import com.vibenote.concept.dto.ConceptLevelResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/** 관리자 카드 상세 (F-13 AC3). 상태와 상관없이 모든 컬럼과 연결된 분석 수·리뷰 수를 준다. */
@Schema(description = "관리자 카드 상세")
public record AdminConceptDetailResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "카드 ID", example = "91")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "개념 이름",
                        example = "낙관적 락")
                String name,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description =
                                "카테고리 (java, spring, web, security, mybatis, vue, database,"
                                        + " javascript, tooling, typescript, react, cs)",
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
                        nullable = true,
                        description = "본문 (Markdown 원문)",
                        example = "## 핵심\n- 엔티티에 버전 컬럼을 두고 UPDATE 조건에 넣는다.")
                String body,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "예제 코드",
                        example =
                                "UPDATE orders SET status = ?, version = version + 1 WHERE id = ?"
                                        + " AND version = ?")
                String exampleCode,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "공식 문서 출처 링크",
                        example =
                                "https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html")
                String referenceUrl,
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
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "리뷰 수", example = "0")
                long reviewCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "이 카드가 연결된 분석 수 (삭제된 분석은 연결이 지워져 빠진다)",
                        example = "2")
                long linkedAnalysisCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "작성 시각",
                        example = "2026-11-20T14:20:00+09:00")
                OffsetDateTime createdAt,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "수정 시각",
                        example = "2026-11-20T14:20:00+09:00")
                OffsetDateTime updatedAt,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        requiredMode = Schema.RequiredMode.REQUIRED,
                                        description =
                                                "수준별 요약·본문. 언제나 3개이고 BEGINNER·JUNIOR·SENIOR 순"
                                                        + " (K-1)"))
                List<ConceptLevelResponse> levels) {

    public static AdminConceptDetailResponse of(
            Concept concept, long reviewCount, long linkedAnalysisCount) {
        return new AdminConceptDetailResponse(
                concept.getId(),
                concept.getName(),
                concept.getCategory(),
                concept.getDifficulty(),
                concept.getSummary(),
                concept.getBody(),
                concept.getExampleCode(),
                concept.getReferenceUrl(),
                concept.getStatus(),
                concept.getSource(),
                concept.getWishCount(),
                concept.getAvgRating().doubleValue(),
                reviewCount,
                linkedAnalysisCount,
                concept.getCreatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime(),
                concept.getUpdatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime(),
                ConceptLevelResponse.listOf(concept));
    }
}
