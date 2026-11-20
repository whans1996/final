package com.vibenote.analysis.concept;

/**
 * 분석에 연결된 개념 하나. 퀴즈 프롬프트의 개념 목록에 쓴다 (F-14 AC2).
 *
 * @param status {@code concepts.status} (PUBLISHED·DRAFT. HIDDEN은 조회에서 뺀다)
 * @param contextNote 그 분석 코드에서 개념이 쓰인 방식. 없으면 null
 */
public record LinkedConcept(long conceptId, String name, String status, String contextNote) {}
