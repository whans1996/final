package com.vibenote.analysis;

import org.springframework.stereotype.Component;

/** 분석 파이프라인 (docs/specs/F-05a-analysis-request-zip.md 5절). */
@Component
public class AnalysisPipeline {

    /** 분석 하나를 끝까지 실행한다. 분석 전용 실행기 스레드에서 부른다. */
    public void run(long analysisId) {}
}
