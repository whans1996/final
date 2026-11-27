package com.vibenote.analysis.domain;

/** 내 분석 목록 행: analyses + 붙은 버전 번호 (P-1a AC12). 버전이 없으면(FETCHING·FAILED) null이다. */
public class AnalysisListItem extends Analysis {

    private Integer versionNumber;

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }
}
