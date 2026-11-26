package com.vibenote.prompteval;

import static com.vibenote.prompteval.GradeFixtures.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** E-1 AC8: 퀴즈 기계 검사와 판정 LLM의 정답 유일률. */
class QuizCheckTest {

    @TempDir Path dir;

    static ObjectNode question(List<String> choices, int answerIndex, String explanation) {
        ObjectNode q = JSON.createObjectNode();
        q.put("question", "서비스는 어떻게 받나요?");
        choices.forEach(q.putArray("choices")::add);
        q.put("answerIndex", answerIndex);
        q.put("explanation", explanation);
        return q;
    }

    static final List<String> FOUR = List.of("생성자로 받는다", "new로 만든다", "정적 필드로 둔다", "매번 조회한다");

    @Test
    void AC8_해설의_선지_위치_언급을_잡는다() {
        for (String text :
                List.of(
                        "2번이 맞습니다.",
                        "3 번 보기는 틀렸습니다.",
                        "첫 번째 보기는 결합이 강합니다.",
                        "두번째 선지가 정답입니다.",
                        "A) 처럼 쓰면 안 됩니다.",
                        "위의 보기 중 생성자만 맞습니다.",
                        "보기 4는 틀립니다.",
                        "②가 정답입니다.",
                        "마지막 보기는 틀립니다.")) {
            assertThat(QuizCheck.mentionsPosition(text)).as(text).isTrue();
        }
        for (String text :
                List.of(
                        "생성자 주입은 final 필드로 바꿀 수 없게 합니다.",
                        "@Transactional은 런타임 예외에서 롤백합니다.",
                        "HTTP 200 응답을 돌려줍니다.",
                        "두 번 호출하면 같은 결과입니다.")) {
            assertThat(QuizCheck.mentionsPosition(text)).as(text).isFalse();
        }

        assertThat(QuizCheck.mechanical(question(FOUR, 0, "생성자로 받으면 바꿔 끼우기 쉽습니다."))).isTrue();
        assertThat(QuizCheck.mechanical(question(FOUR, 0, "2번 보기는 결합이 강해집니다."))).isFalse();
        assertThat(
                        QuizCheck.mechanical(
                                question(
                                        List.of("생성자로 받는다", "생성자로 받는다 ", "정적 필드로 둔다", "매번 조회한다"),
                                        0,
                                        "설명")))
                .as("보기가 서로 달라야 한다 (앞뒤 공백 무시)")
                .isFalse();
        assertThat(QuizCheck.mechanical(question(FOUR.subList(0, 3), 0, "설명"))).isFalse();
        assertThat(QuizCheck.mechanical(question(FOUR, 4, "설명"))).isFalse();
        assertThat(QuizCheck.mechanical(question(FOUR, -1, "설명"))).isFalse();
    }

    @Test
    void AC8_정답_유일률은_하나만_고르고_위치가_같을_때() {
        JsonNode report = GradeFixtures.report(GradeFixtures.zip(dir));
        // 수준마다 2문항: "예외가" 문항(answerIndex 1)은 [1]로 유일·일치.
        // DI 문항(answerIndex 0)은 입문 [0] 일치, 주니어 [0,1] 둘, 시니어 [2] 위치 다름
        LlmClient judge =
                request -> {
                    String p = request.prompt();
                    String correct =
                            p.contains("예외가")
                                    ? "[1]"
                                    : p.contains("입문 설명")
                                            ? "[0]"
                                            : p.contains("주니어 설명") ? "[0,1]" : "[2]";
                    return new LlmResponse("{\"correct\":" + correct + "}", "gemini-test", 1, 1);
                };

        JsonNode e = JSON.valueToTree(QuizCheck.grade(report, Optional.of(judge)));

        assertThat(e.path("questions").asInt()).isEqualTo(6);
        assertThat(e.path("uniqueAnswer").asDouble()).isCloseTo(4.0 / 6, within(1e-3));
        assertThat(e.path("mechanical").asDouble()).isCloseTo(0.5, within(1e-3));
        assertThat(e.path("excluded").asInt()).isZero();
    }
}
