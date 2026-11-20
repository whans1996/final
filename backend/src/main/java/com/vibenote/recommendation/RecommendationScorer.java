package com.vibenote.recommendation;

import com.vibenote.common.Level;
import com.vibenote.concept.dto.ConceptSummaryResponse;
import com.vibenote.recommendation.dto.RecommendationSignals;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 규칙 기반 추천 점수 (F-16 1.1절). {@code min(오답,3)×3 + 내 코드 2 + 찜한 분야 1 + 수준 적합도(1·0·-1)}, 점수 → 찜 수 → 평점 →
 * id 순. 순수 함수다.
 */
final class RecommendationScorer {

    /** 같은 퀴즈를 여러 번 풀어 한 카드가 추천을 독점하지 않게 오답 점수는 여기서 멈춘다. */
    static final int WRONG_CAP = 3;

    static final int WRONG_WEIGHT = 3;
    static final int IN_MY_CODE = 2;
    static final int WISHED_CATEGORY = 1;

    private static final Comparator<ScoredConcept> ORDER =
            Comparator.comparingInt(ScoredConcept::score)
                    .reversed()
                    .thenComparing(s -> s.concept().wishCount(), Comparator.reverseOrder())
                    .thenComparing(s -> s.concept().avgRating(), Comparator.reverseOrder())
                    .thenComparingLong(s -> s.concept().id());

    private RecommendationScorer() {}

    /**
     * @param candidates PUBLISHED 카드 전체
     * @param wrongCounts 카드 id → 내 오답 문항 수 (없으면 0)
     * @param myConceptIds 삭제되지 않은 내 분석에 나온 카드
     * @param wishedCategories 내가 찜한 PUBLISHED 카드의 카테고리
     * @param excludedIds 후보에서 뺄 카드 (복습 끝남, 오늘 복습, 찜한 카드)
     * @param level 회원 수준
     */
    record Input(
            List<ConceptSummaryResponse> candidates,
            Map<Long, Integer> wrongCounts,
            Set<Long> myConceptIds,
            Set<String> wishedCategories,
            Set<Long> excludedIds,
            Level level) {}

    /** 같으면 1, 한 단계 차이 0, 두 단계 차이 -1. */
    static int levelFit(Level user, Level difficulty) {
        return 1 - Math.abs(user.ordinal() - difficulty.ordinal());
    }

    /** 점수가 높은 순으로 최대 {@code size}개. 점수가 0 이하여도 빼지 않는다. */
    static List<ScoredConcept> rank(Input input, int size) {
        return input.candidates().stream()
                .filter(c -> !input.excludedIds().contains(c.id()))
                .map(c -> score(c, input))
                .sorted(ORDER)
                .limit(size)
                .toList();
    }

    private static ScoredConcept score(ConceptSummaryResponse concept, Input input) {
        RecommendationSignals signals =
                new RecommendationSignals(
                        input.wrongCounts().getOrDefault(concept.id(), 0),
                        input.myConceptIds().contains(concept.id()),
                        input.wishedCategories().contains(concept.category()),
                        levelFit(input.level(), concept.difficulty()));
        int wrong = Math.min(signals.wrongCount(), WRONG_CAP);
        int score =
                wrong * WRONG_WEIGHT
                        + (signals.inMyCode() ? IN_MY_CODE : 0)
                        + (signals.wishedCategory() ? WISHED_CATEGORY : 0)
                        + signals.levelFit();
        String key =
                "w%dc%dk%dl%d"
                        .formatted(
                                wrong,
                                signals.inMyCode() ? 1 : 0,
                                signals.wishedCategory() ? 1 : 0,
                                signals.levelFit());
        return new ScoredConcept(concept, signals, score, key);
    }
}
