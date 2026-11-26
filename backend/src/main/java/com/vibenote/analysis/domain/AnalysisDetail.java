package com.vibenote.analysis.domain;

import com.vibenote.analysis.explain.AnalysisOverview;

/**
 * 분석 상세 조회 행: analyses + overview JSON + 파일 수 (F-06 AC9) + 주인 닉네임 (F-19 AC9) + 간이 모드 (F-27 AC10).
 */
public class AnalysisDetail extends Analysis {

    private AnalysisOverview overview;
    private int fileCount;
    private int explainedFileCount;
    private String ownerNickname;

    /** 성공한 Ollama(간이 모드) 호출이 있는지 (F-27 AC10). */
    private boolean liteMode;

    public boolean isLiteMode() {
        return liteMode;
    }

    public void setLiteMode(boolean liteMode) {
        this.liteMode = liteMode;
    }

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

    public String getOwnerNickname() {
        return ownerNickname;
    }

    public void setOwnerNickname(String ownerNickname) {
        this.ownerNickname = ownerNickname;
    }
}
