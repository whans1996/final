package com.vibenote.recommendation;

import com.vibenote.common.Level;
import com.vibenote.concept.dto.ConceptSummaryResponse;
import com.vibenote.recommendation.dto.RecommendationSignals;
import java.util.ArrayList;
import java.util.List;

/**
 * LLM 없이 신호로 만드는 추천 이유 (F-16 1.2절). LLM이 실패한 카드의 {@code RULE} 이유와, 프롬프트의 "근거" 칸(6.2절)에 같은 조각을 쓴다.
 * 다만 근거 칸의 오답 횟수만 신호 키처럼 3에서 멈춘다({@link #grounds}).
 */
final class RuleReasons {

    static final int MAX_FRAGMENTS = 2;
    static final String POPULAR = "많은 회원이 찜한 카드예요.";
    static final String FRESH = "새로 살펴보기 좋은 카드예요.";

    private RuleReasons() {}

    /**
     * 프롬프트 "근거" 칸의 조각 전부 (6.2절). 오답은 신호 키처럼 {@link RecommendationScorer#WRONG_CAP}회에서 멈춰 "3번 이상"으로
     * 쓴다. 키가 같으면 캐시한 AI 이유를 다시 쓰므로, 그 뒤 오답이 늘어도 이유 속 횟수가 틀리지 않게 한다.
     */
    static List<String> grounds(
            RecommendationSignals signals, ConceptSummaryResponse concept, Level level) {
        int wrong = signals.wrongCount();
        String count =
                wrong >= RecommendationScorer.WRONG_CAP
                        ? RecommendationScorer.WRONG_CAP + "번 이상"
                        : wrong + "번";
        return fragments(signals, concept, level, count);
    }

    /** 규칙 문장의 조각 전부 (오답 → 내 코드 → 찜한 분야 → 수준 맞음 순, 끝에 마침표 없음). 오답은 실제 횟수다. */
    private static List<String> fragments(
            RecommendationSignals signals,
            ConceptSummaryResponse concept,
            Level level,
            String wrongCountText) {
        List<String> out = new ArrayList<>(4);
        if (signals.wrongCount() > 0) {
            out.add("퀴즈에서 " + wrongCountText + " 틀린 개념이에요");
        }
        if (signals.inMyCode()) {
            out.add("내 분석 코드에 나온 개념이에요");
        }
        if (signals.wishedCategory()) {
            out.add("찜한 카드와 같은 " + concept.category() + " 분야예요");
        }
        if (signals.levelFit() == 1) {
            out.add("내 수준(" + label(level) + ")에 맞는 카드예요");
        }
        return out;
    }

    /** 앞에서 두 조각까지 {@code ", "}로 잇고 마침표. 조각이 없으면 찜 수에 따라 두 문장 중 하나. */
    static String of(RecommendationSignals signals, ConceptSummaryResponse concept, Level level) {
        List<String> parts = fragments(signals, concept, level, signals.wrongCount() + "번");
        if (parts.isEmpty()) {
            return concept.wishCount() > 0 ? POPULAR : FRESH;
        }
        return String.join(", ", parts.subList(0, Math.min(MAX_FRAGMENTS, parts.size()))) + ".";
    }

    /** 수준의 한국어 라벨 (프롬프트 {@code level} 자리에도 쓴다). */
    static String label(Level level) {
        return switch (level) {
            case BEGINNER -> "입문";
            case JUNIOR -> "주니어";
            case SENIOR -> "숙련";
        };
    }
}
