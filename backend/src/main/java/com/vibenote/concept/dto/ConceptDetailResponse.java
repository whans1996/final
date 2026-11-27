package com.vibenote.concept.dto;

import com.vibenote.common.Level;
import com.vibenote.concept.domain.Concept;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 개념 카드 상세 (F-10). 상태·출처 구분·작성 시각은 내보내지 않는다 (AC1). */
@Schema(description = "개념 카드 상세")
public record ConceptDetailResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "카드 ID", example = "57")
                long id,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "개념 이름",
                        example = "JWT 기반 인증")
                String name,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description =
                                "카테고리 (java, spring, web, security, mybatis, vue, database,"
                                        + " javascript, tooling, typescript, react, cs)",
                        example = "security")
                String category,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "난이도",
                        example = "SENIOR")
                Level difficulty,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "한 줄 요약",
                        example = "JWT는 사용자 정보와 만료 시간을 담고 서명한 토큰이다.")
                String summary,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "쉬운 설명 본문 (Markdown 원문, 렌더링은 FE)",
                        example = "## 핵심\n- 서버는 **서명**과 만료만 검증한다.")
                String body,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "예제 코드 (언어 정보 없음)",
                        example = "String token = Jwts.builder().subject(email).compact();")
                String exampleCode,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "공식 문서 출처 링크",
                        example =
                                "https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html")
                String referenceUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "찜 수", example = "12")
                int wishCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "내가 찜했는지 (F-12). 비로그인·깨진 토큰이면 false",
                        example = "false")
                boolean wished,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "평균 별점 (0.0~5.0, 소수 1자리. 리뷰가 없으면 0.0)",
                        example = "4.5")
                double avgRating,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        requiredMode = Schema.RequiredMode.REQUIRED,
                                        description =
                                                "내 분석(삭제되지 않은 DONE)에서 등장한 위치, 최신순 최대 10개. 비로그인은 빈"
                                                        + " 배열"))
                List<ConceptAppearanceResponse> appearances,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        requiredMode = Schema.RequiredMode.REQUIRED,
                                        description = "관련 카드 최대 4개 (함께 등장한 횟수순, 모자라면 같은 카테고리 인기순)"))
                List<ConceptSummaryResponse> related,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "로그인 회원의 수준 (users.level). 비로그인·깨진 토큰이면 null (K-1)",
                        example = "JUNIOR")
                Level viewerLevel,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        requiredMode = Schema.RequiredMode.REQUIRED,
                                        description =
                                                "수준별 요약·본문. 언제나 3개이고 BEGINNER·JUNIOR·SENIOR 순. 화면은"
                                                    + " viewerLevel의 body가 있으면 그것을, 없으면 공통 body + 그"
                                                    + " 수준 summary를 보인다 (K-1)"))
                List<ConceptLevelResponse> levels) {

    public static ConceptDetailResponse of(
            Concept concept,
            boolean wished,
            List<ConceptAppearanceResponse> appearances,
            List<ConceptSummaryResponse> related,
            Level viewerLevel) {
        return new ConceptDetailResponse(
                concept.getId(),
                concept.getName(),
                concept.getCategory(),
                concept.getDifficulty(),
                concept.getSummary(),
                concept.getBody(),
                concept.getExampleCode(),
                concept.getReferenceUrl(),
                concept.getWishCount(),
                wished,
                concept.getAvgRating().doubleValue(),
                appearances,
                related,
                viewerLevel,
                ConceptLevelResponse.listOf(concept));
    }
}
