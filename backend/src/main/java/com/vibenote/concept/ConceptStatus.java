package com.vibenote.concept;

/** concepts.status (V1). 일반 사용자에게는 PUBLISHED만 보인다 (NF-04). */
public enum ConceptStatus {
    /** AI 추출 초안. 관리자 검수 전 (F-08 → F-13). */
    DRAFT,
    PUBLISHED,
    /** 공개 후 내린 카드. 리뷰·찜이 CASCADE로 지워지지 않게 DELETE 대신 쓴다. */
    HIDDEN,
}
