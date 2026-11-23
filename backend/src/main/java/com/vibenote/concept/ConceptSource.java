package com.vibenote.concept;

/** concepts.source (V1, ADMIN은 V7). */
public enum ConceptSource {
    /** R__seed_concepts.sql 시드 (DS-05). */
    SEED,
    /** 분석에서 AI가 추출한 초안 (F-08). */
    AI,
    /** 관리자가 화면에서 직접 작성 (F-13). */
    ADMIN,
}
