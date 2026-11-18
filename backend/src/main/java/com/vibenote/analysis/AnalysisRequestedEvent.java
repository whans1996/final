package com.vibenote.analysis;

/** 분석 요청이 접수됨. 요청 트랜잭션이 커밋된 뒤 파이프라인을 시작하는 데 쓴다. */
public record AnalysisRequestedEvent(long analysisId) {}
