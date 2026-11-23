package com.vibenote.analysis.domain;

import com.vibenote.analysis.AnalysisStatus;
import com.vibenote.analysis.SourceType;
import com.vibenote.analysis.fetch.RepositoryUrl;
import com.vibenote.common.Level;
import java.time.LocalDateTime;

/** analyses 행 (overview·memo 제외). Controller 응답으로 직접 내보내지 않는다 (DTO로 변환). */
public class Analysis {

    private Long id;
    private Long userId;
    private SourceType sourceType;
    private String sourceUrl;
    private String projectName;
    private String title;
    private Level level;
    private AnalysisStatus status;
    private int progress;
    private boolean isPublic;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 막 접수된 ZIP 분석 (FETCHING, progress 0, 비공개). 제목은 프로젝트 이름으로 시작한다. */
    public static Analysis newZip(
            long userId, String projectName, Level level, LocalDateTime createdAt) {
        Analysis analysis = new Analysis();
        analysis.userId = userId;
        analysis.sourceType = SourceType.ZIP;
        analysis.projectName = projectName;
        analysis.title = projectName;
        analysis.level = level;
        analysis.status = AnalysisStatus.FETCHING;
        analysis.createdAt = createdAt;
        return analysis;
    }

    /** 막 접수된 저장소 URL 분석 (F-05b AC1). 주소는 정규화한 값, 이름은 마지막 조각이다. */
    public static Analysis newUrl(
            long userId, RepositoryUrl repository, Level level, LocalDateTime createdAt) {
        Analysis analysis = newZip(userId, repository.name(), level, createdAt);
        analysis.sourceType = SourceType.URL;
        analysis.sourceUrl = repository.normalized();
        return analysis;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public AnalysisStatus getStatus() {
        return status;
    }

    public void setStatus(AnalysisStatus status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
