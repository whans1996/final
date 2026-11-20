package com.vibenote.recommendation;

import static com.vibenote.recommendation.RecommendationScorerTest.card;
import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.common.Level;
import com.vibenote.recommendation.dto.RecommendationSignals;
import org.junit.jupiter.api.Test;

/** F-16 1.2절 규칙 문장. */
class RuleReasonsTest {

    @Test
    void 신호_순서대로_앞에서_두_개까지_잇는다() {
        String reason =
                RuleReasons.of(
                        new RecommendationSignals(2, true, true, 1),
                        card(1, "security", Level.JUNIOR, 0, 0),
                        Level.JUNIOR);

        assertThat(reason).isEqualTo("퀴즈에서 2번 틀린 개념이에요, 내 분석 코드에 나온 개념이에요.");
    }

    @Test
    void 오답_수는_상한_없이_실제_개수로_쓴다() {
        String reason =
                RuleReasons.of(
                        new RecommendationSignals(5, false, false, 0),
                        card(1, "security", Level.JUNIOR, 0, 0),
                        Level.JUNIOR);

        assertThat(reason).isEqualTo("퀴즈에서 5번 틀린 개념이에요.");
    }

    @Test
    void 찜한_분야와_수준_조각() {
        String reason =
                RuleReasons.of(
                        new RecommendationSignals(0, false, true, 1),
                        card(1, "spring", Level.SENIOR, 0, 0),
                        Level.SENIOR);

        assertThat(reason).isEqualTo("찜한 카드와 같은 spring 분야예요, 내 수준(숙련)에 맞는 카드예요.");
        assertThat(
                        RuleReasons.of(
                                new RecommendationSignals(0, false, false, 1),
                                card(1, "web", Level.BEGINNER, 0, 0),
                                Level.BEGINNER))
                .isEqualTo("내 수준(입문)에 맞는 카드예요.");
    }

    @Test
    void 신호가_없으면_찜_수에_따라_두_문장_중_하나() {
        RecommendationSignals none = new RecommendationSignals(0, false, false, 0);

        assertThat(RuleReasons.of(none, card(1, "java", Level.SENIOR, 0, 3), Level.JUNIOR))
                .isEqualTo("많은 회원이 찜한 카드예요.");
        assertThat(RuleReasons.of(none, card(1, "java", Level.SENIOR, 0, 0), Level.JUNIOR))
                .isEqualTo("새로 살펴보기 좋은 카드예요.");
        assertThat(
                        RuleReasons.of(
                                new RecommendationSignals(0, false, false, -1),
                                card(1, "java", Level.SENIOR, 0, 0),
                                Level.BEGINNER))
                .isEqualTo("새로 살펴보기 좋은 카드예요.");
    }

    @Test
    void AC5_프롬프트_근거의_오답_횟수는_신호_키처럼_3번에서_멈춘다() {
        // 신호 키는 min(오답,3)이라 오답이 3회를 넘어도 캐시를 다시 만들지 않는다.
        // 그래서 AI 이유에 남는 횟수가 그 뒤 오답 수와 어긋나지 않게 "3번 이상"으로 묶는다.
        for (int wrong : new int[] {3, 4, 7}) {
            assertThat(
                            RuleReasons.grounds(
                                    new RecommendationSignals(wrong, false, false, 0),
                                    card(1, "java", Level.JUNIOR, 0, 0),
                                    Level.JUNIOR))
                    .containsExactly("퀴즈에서 3번 이상 틀린 개념이에요");
        }
        assertThat(
                        RuleReasons.grounds(
                                new RecommendationSignals(2, false, false, 0),
                                card(1, "java", Level.JUNIOR, 0, 0),
                                Level.JUNIOR))
                .containsExactly("퀴즈에서 2번 틀린 개념이에요");
    }

    @Test
    void 프롬프트_근거는_조각을_모두_준다() {
        assertThat(
                        RuleReasons.grounds(
                                new RecommendationSignals(1, true, true, 1),
                                card(1, "java", Level.JUNIOR, 0, 0),
                                Level.JUNIOR))
                .containsExactly(
                        "퀴즈에서 1번 틀린 개념이에요",
                        "내 분석 코드에 나온 개념이에요",
                        "찜한 카드와 같은 java 분야예요",
                        "내 수준(주니어)에 맞는 카드예요");
    }
}
