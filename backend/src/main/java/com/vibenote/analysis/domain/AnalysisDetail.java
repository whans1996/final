package com.vibenote.analysis.domain;

import com.vibenote.analysis.explain.AnalysisOverview;
import java.time.LocalDateTime;

/**
 * 분석 상세 조회 행: analyses + overview JSON + 파일 수 (F-06 AC9) + 주인 닉네임 (F-19 AC9) + 간이 모드 (F-27 AC10) +
 * 프로젝트·버전 (P-1a AC12).
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

    // ── P-1a AC12: 붙은 프로젝트의 지금 이름과 버전 (버전이 없으면 version* 모두 null) ──

    /** projects.name. 분석의 {@code projectName}(요청 때 이름)과 달리 이름을 바꾸면 따라 바뀐다. */
    private String projectRefName;

    private Integer versionNumber;
    private String versionCommitSha;
    private String versionCommitMessage;
    private LocalDateTime versionCommittedAt;

    public String getProjectRefName() {
        return projectRefName;
    }

    public void setProjectRefName(String projectRefName) {
        this.projectRefName = projectRefName;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getVersionCommitSha() {
        return versionCommitSha;
    }

    public void setVersionCommitSha(String versionCommitSha) {
        this.versionCommitSha = versionCommitSha;
    }

    public String getVersionCommitMessage() {
        return versionCommitMessage;
    }

    public void setVersionCommitMessage(String versionCommitMessage) {
        this.versionCommitMessage = versionCommitMessage;
    }

    public LocalDateTime getVersionCommittedAt() {
        return versionCommittedAt;
    }

    public void setVersionCommittedAt(LocalDateTime versionCommittedAt) {
        this.versionCommittedAt = versionCommittedAt;
    }
}
