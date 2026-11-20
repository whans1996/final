package com.vibenote.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vibenote.analysis.explain.LlmJsonCall.InvalidResponseException;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** F-16 AC7, 6.3절. */
class RecommendationResponseParserTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static Map<Long, String> parse(String json, Long... ids) {
        JsonNode root = JSON.readTree(json);
        return RecommendationResponseParser.parse(root, Set.of(ids));
    }

    @Test
    void AC7_앞뒤_공백을_떼고_줄바꿈과_연속_공백을_공백_하나로_바꾼다() {
        Map<Long, String> reasons =
                parse(
                        """
                        {"reasons": [{"conceptId": 1, "reason": "  첫 줄\\n  둘째   줄\\t끝  "}]}
                        """,
                        1L);

        assertThat(reasons).containsExactly(Map.entry(1L, "첫 줄 둘째 줄 끝"));
    }

    @Test
    void AC7_100자를_넘으면_100자에서_자른다() {
        String longReason = "가".repeat(120);
        Map<Long, String> reasons =
                parse(
                        "{\"reasons\": [{\"conceptId\": 1, \"reason\": \"" + longReason + "\"}]}",
                        1L);

        assertThat(reasons.get(1L)).isEqualTo("가".repeat(100));
    }

    @Test
    void AC7_모르는_id_빈_값_문자열이_아닌_값_중복_id는_버린다() {
        Map<Long, String> reasons =
                parse(
                        """
                        {"reasons": [
                          {"conceptId": 9, "reason": "요청하지 않은 카드"},
                          {"conceptId": 2, "reason": "   "},
                          {"conceptId": 3, "reason": 7},
                          {"conceptId": "4", "reason": "id가 문자열"},
                          {"conceptId": 1, "reason": "첫 번째"},
                          {"conceptId": 1, "reason": "두 번째"},
                          "문자열 항목",
                          {"conceptId": 5}
                        ]}
                        """,
                        1L,
                        2L,
                        3L,
                        4L,
                        5L);

        assertThat(reasons).containsExactly(Map.entry(1L, "첫 번째"));
    }

    @Test
    void AC7_같은_id의_첫_항목이_버려져도_두_번째_항목은_버린다() {
        Map<Long, String> reasons =
                parse(
                        """
                        {"reasons": [
                          {"conceptId": 1, "reason": ""},
                          {"conceptId": 1, "reason": "두 번째"}
                        ]}
                        """,
                        1L);

        assertThat(reasons).isEmpty();
    }

    @Test
    void 루트가_객체가_아니거나_reasons가_배열이_아니면_형식_오류() {
        assertThatThrownBy(() -> parse("[]", 1L)).isInstanceOf(InvalidResponseException.class);
        assertThatThrownBy(() -> parse("{}", 1L)).isInstanceOf(InvalidResponseException.class);
        assertThatThrownBy(() -> parse("{\"reasons\": {}}", 1L))
                .isInstanceOf(InvalidResponseException.class);
    }

    @Test
    void 빈_배열은_형식_오류가_아니다() {
        assertThat(parse("{\"reasons\": []}", 1L)).isEmpty();
    }
}
