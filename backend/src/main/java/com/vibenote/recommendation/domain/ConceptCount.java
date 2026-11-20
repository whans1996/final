package com.vibenote.recommendation.domain;

/** 카드별 개수 (오답 문항 수). */
public record ConceptCount(long conceptId, int count) {}
