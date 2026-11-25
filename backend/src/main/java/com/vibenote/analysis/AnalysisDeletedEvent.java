package com.vibenote.analysis;

/** 분석이 삭제(soft delete)됨. 삭제 트랜잭션이 커밋된 뒤 분석 단위 캐시를 비우는 데 쓴다 (F-21a 5.3절 4). */
public record AnalysisDeletedEvent(long analysisId) {}
