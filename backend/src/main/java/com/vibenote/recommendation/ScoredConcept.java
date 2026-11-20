package com.vibenote.recommendation;

import com.vibenote.concept.dto.ConceptSummaryResponse;
import com.vibenote.recommendation.dto.RecommendationSignals;

/**
 * 점수를 매긴 추천 후보.
 *
 * @param signalKey 신호 키 {@code w{0..3}c{0|1}k{0|1}l{-1|0|1}}. 이유 캐시를 다시 만들지 판단한다
 */
record ScoredConcept(
        ConceptSummaryResponse concept,
        RecommendationSignals signals,
        int score,
        String signalKey) {}
