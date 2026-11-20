package com.vibenote.recommendation;

import static com.vibenote.analysis.explain.ExplainPrompts.escapeDelimiters;

import com.vibenote.common.Level;
import com.vibenote.concept.dto.ConceptSummaryResponse;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RECOMMEND 프롬프트의 카드 줄 (F-16 6.2절). 카드마다 {@code id | 이름 | 카테고리 | 난이도 | 요약: … | 근거: …} 한 줄이다. Mock은
 * 줄 맨 앞의 {@code id |}로 카드를 찾는다.
 */
final class RecommendationPrompts {

    static final int MAX_CARDS = 10;
    static final int SUMMARY_CHARS = 100;
    static final String NO_GROUNDS = "없음(수준 맞춤·인기)";

    private RecommendationPrompts() {}

    static String cards(List<ScoredConcept> cards, Level level) {
        return cards.stream()
                .limit(MAX_CARDS)
                .map(c -> line(c, level))
                .collect(Collectors.joining("\n"));
    }

    private static String line(ScoredConcept scored, Level level) {
        ConceptSummaryResponse c = scored.concept();
        List<String> fragments = RuleReasons.fragments(scored.signals(), c, level);
        String grounds = fragments.isEmpty() ? NO_GROUNDS : String.join(" / ", fragments);
        return c.id()
                + " | "
                + oneLine(c.name())
                + " | "
                + c.category()
                + " | "
                + c.difficulty()
                + " | 요약: "
                + oneLine(cut(c.summary()))
                + " | 근거: "
                + grounds;
    }

    private static String cut(String value) {
        if (value == null) {
            return "";
        }
        return value.codePointCount(0, value.length()) > SUMMARY_CHARS
                ? value.substring(0, value.offsetByCodePoints(0, SUMMARY_CHARS))
                : value;
    }

    /** 줄바꿈을 공백으로 바꿔 카드 하나가 한 줄이 되게 하고, 구분자 문자열을 바꿔 넣는다. */
    private static String oneLine(String value) {
        return escapeDelimiters(value.strip().replaceAll("\\s+", " "));
    }
}
