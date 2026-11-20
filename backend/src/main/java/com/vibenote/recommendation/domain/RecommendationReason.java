package com.vibenote.recommendation.domain;

/**
 * {@code recommendation_reasons} 행 (회원은 조회 조건).
 *
 * @param signalKey 이유를 만들 때의 신호 키. 지금 신호 키와 다르면 다시 만든다
 */
public record RecommendationReason(long conceptId, String reason, String signalKey) {}
