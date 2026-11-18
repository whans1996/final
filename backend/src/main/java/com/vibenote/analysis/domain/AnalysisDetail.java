package com.vibenote.analysis.domain;

import com.vibenote.analysis.explain.AnalysisOverview;

/** 분석 상세 조회 행: analyses + overview JSON + 파일 수 (F-06 AC9). */
public class AnalysisDetail extends Analysis {

    private AnalysisOverview overview;
    private int fileCount;
    private int explainedFileCount;

    public AnalysisOverview getOverview() {
        return overview;
    }

    public void setOverview(AnalysisOverview overview) {
        this.overview = overview;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public int getExplainedFileCount() {
        return explainedFileCount;
    }

    public void setExplainedFileCount(int explainedFileCount) {
        this.explainedFileCount = explainedFileCount;
    }
}
