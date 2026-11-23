package com.vibenote.concept.domain;

import com.vibenote.concept.ConceptStatus;

/** 상태별 카드 수 한 행 (F-13 AC2). */
public record ConceptStatusCount(ConceptStatus status, long count) {}
