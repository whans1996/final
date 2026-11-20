package com.vibenote.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.common.Level;
import com.vibenote.concept.dto.ConceptSummaryResponse;
import com.vibenote.recommendation.dto.RecommendationSignals;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** F-16 1.1절 점수 규칙. */
class RecommendationScorerTest {

    static ConceptSummaryResponse card(
            long id, String category, Level difficulty, double avgRating, int wishCount) {
        return new ConceptSummaryResponse(
                id, "카드" + id, category, difficulty, "요약", avgRating, wishCount);
    }

    private static RecommendationScorer.Input input(
            List<ConceptSummaryResponse> candidates,
            Map<Long, Integer> wrong,
            Set<Long> mine,
            Set<String> wishedCategories,
            Set<Long> excluded,
            Level level) {
        return new RecommendationScorer.Input(
                candidates, wrong, mine, wishedCategories, excluded, level);
    }

    @Test
    void 오답_점수는_3회에서_멈추고_신호의_오답_수는_실제_개수다() {
        var ranked =
                RecommendationScorer.rank(
                        input(
                                List.of(card(1, "java", Level.SENIOR, 0, 0)),
                                Map.of(1L, 4),
                                Set.of(),
                                Set.of(),
                                Set.of(),
                                Level.BEGINNER),
                        5);

        assertThat(ranked).hasSize(1);
        assertThat(ranked.get(0).score()).isEqualTo(9 - 1);
        assertThat(ranked.get(0).signals())
                .isEqualTo(new RecommendationSignals(4, false, false, -1));
        assertThat(ranked.get(0).signalKey()).isEqualTo("w3c0k0l-1");
    }

    @Test
    void 수준_적합도는_같으면_1_한_단계_0_두_단계_마이너스1() {
        assertThat(RecommendationScorer.levelFit(Level.JUNIOR, Level.JUNIOR)).isEqualTo(1);
        assertThat(RecommendationScorer.levelFit(Level.JUNIOR, Level.SENIOR)).isZero();
        assertThat(RecommendationScorer.levelFit(Level.JUNIOR, Level.BEGINNER)).isZero();
        assertThat(RecommendationScorer.levelFit(Level.BEGINNER, Level.SENIOR)).isEqualTo(-1);
        assertThat(RecommendationScorer.levelFit(Level.SENIOR, Level.BEGINNER)).isEqualTo(-1);
    }

    @Test
    void 신호를_모두_더하고_신호_키를_만든다() {
        var ranked =
                RecommendationScorer.rank(
                        input(
                                List.of(card(7, "spring", Level.JUNIOR, 0, 0)),
                                Map.of(7L, 2),
                                Set.of(7L),
                                Set.of("spring"),
                                Set.of(),
                                Level.JUNIOR),
                        5);

        assertThat(ranked.get(0).score()).isEqualTo(6 + 2 + 1 + 1);
        assertThat(ranked.get(0).signals()).isEqualTo(new RecommendationSignals(2, true, true, 1));
        assertThat(ranked.get(0).signalKey()).isEqualTo("w2c1k1l1");
    }

    @Test
    void 점수_찜수_평점_id_순으로_정렬하고_제외_카드를_빼고_size만큼_자른다() {
        var ranked =
                RecommendationScorer.rank(
                        input(
                                List.of(
                                        card(5, "java", Level.JUNIOR, 4.0, 3),
                                        card(4, "java", Level.JUNIOR, 4.5, 3),
                                        card(3, "java", Level.JUNIOR, 4.5, 3),
                                        card(2, "java", Level.JUNIOR, 1.0, 9),
                                        card(1, "java", Level.SENIOR, 5.0, 99),
                                        card(9, "web", Level.JUNIOR, 5.0, 99),
                                        card(8, "java", Level.BEGINNER, 0, 0)),
                                Map.of(8L, 1),
                                Set.of(),
                                Set.of(),
                                Set.of(9L),
                                Level.JUNIOR),
                        5);

        assertThat(ranked).extracting(s -> s.concept().id()).containsExactly(8L, 2L, 3L, 4L, 5L);
        assertThat(ranked).extracting(ScoredConcept::score).containsExactly(3, 1, 1, 1, 1);
    }

    @Test
    void 점수가_0_이하여도_후보에_남는다() {
        var ranked =
                RecommendationScorer.rank(
                        input(
                                List.of(card(1, "java", Level.SENIOR, 0, 0)),
                                Map.of(),
                                Set.of(),
                                Set.of(),
                                Set.of(),
                                Level.BEGINNER),
                        5);

        assertThat(ranked).extracting(ScoredConcept::score).containsExactly(-1);
        assertThat(ranked.get(0).signalKey()).isEqualTo("w0c0k0l-1");
    }
}
