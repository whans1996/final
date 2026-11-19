package com.vibenote.concept;

/** 카드 목록 정렬 (F-09). SQL은 ConceptMapper.xml의 {@code <choose>}가 고정 문자열로 고른다. */
public enum ConceptSort {
    /** wish_count DESC, avg_rating DESC, id ASC. */
    POPULAR,
    /** created_at DESC, id DESC. */
    LATEST,
}
