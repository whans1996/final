package com.vibenote.analysis;

/** 분석 진행 상태 (analyses.status). 단계 순서와 진행률은 AnalysisPipeline이 정한다 (F-05a 5.1절). */
public enum AnalysisStatus {
    FETCHING,
    FILTERING,
    SELECTING,
    EXPLAINING,
    EXTRACTING,
    QUIZ,
    DONE,
    FAILED
}
