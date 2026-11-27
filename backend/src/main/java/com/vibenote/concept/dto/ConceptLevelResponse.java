package com.vibenote.concept.dto;

import com.vibenote.common.Level;
import com.vibenote.concept.domain.Concept;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.List;

/** 카드의 수준별 한 줄 요약·본문 하나 (K-1 AC2). 카드 상세·관리자 상세에 언제나 3개가 BEGINNER·JUNIOR·SENIOR 순으로 온다. */
@Schema(description = "카드의 수준별 요약·본문")
public record ConceptLevelResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "수준", example = "JUNIOR")
                Level level,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "그 수준 독자에게 맞춘 한 줄 요약 (300자 이하). 없으면 null",
                        example =
                                "생성자 파라미터로 받은 빈을 컨테이너가 주입하고, @RequiredArgsConstructor가 그 생성자를 만들어"
                                        + " 줍니다.")
                String summary,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description =
                                "그 수준의 본문 (Markdown 원문). 없으면 null이고 화면은 공통 body + 그 수준 summary를"
                                        + " 보인다",
                        example = "## 핵심\n- 빈은 타입으로 찾고, 같은 타입이 둘이면 @Qualifier로 고른다.")
                String body) {

    /** BEGINNER·JUNIOR·SENIOR 순서의 3개. */
    public static List<ConceptLevelResponse> listOf(Concept concept) {
        return Arrays.stream(Level.values())
                .map(l -> new ConceptLevelResponse(l, concept.summaryFor(l), concept.bodyFor(l)))
                .toList();
    }
}
