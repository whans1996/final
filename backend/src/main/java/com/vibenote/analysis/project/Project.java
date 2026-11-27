package com.vibenote.analysis.project;

import com.vibenote.analysis.SourceType;
import com.vibenote.analysis.fetch.RepositoryUrl;
import java.time.LocalDateTime;

/** projects 행 (P-1a 4절). Controller 응답으로 직접 내보내지 않는다 (DTO로 변환). */
public class Project {

    private Long id;
    private Long userId;
    private SourceType sourceType;
    private String sourceUrl;
    private String name;
    private String latestCommitSha;
    private String latestCommitMessage;
    private LocalDateTime latestCommittedAt;
    private LocalDateTime latestCheckedAt;
    private LocalDateTime createdAt;

    /** 저장소 프로젝트. 주소는 정규화한 값, 이름은 저장소 이름이다 (AC1). */
    static Project newUrl(long userId, RepositoryUrl repository, LocalDateTime createdAt) {
        Project project = newZip(userId, repository.name(), createdAt);
        project.sourceType = SourceType.URL;
        project.sourceUrl = repository.normalized();
        return project;
    }

    /** ZIP 프로젝트. 이름은 파일 이름이다 (AC4). */
    static Project newZip(long userId, String name, LocalDateTime createdAt) {
        Project project = new Project();
        project.userId = userId;
        project.sourceType = SourceType.ZIP;
        project.name = name;
        project.createdAt = createdAt;
        return project;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLatestCommitSha() {
        return latestCommitSha;
    }

    public void setLatestCommitSha(String latestCommitSha) {
        this.latestCommitSha = latestCommitSha;
    }

    public String getLatestCommitMessage() {
        return latestCommitMessage;
    }

    public void setLatestCommitMessage(String latestCommitMessage) {
        this.latestCommitMessage = latestCommitMessage;
    }

    public LocalDateTime getLatestCommittedAt() {
        return latestCommittedAt;
    }

    public void setLatestCommittedAt(LocalDateTime latestCommittedAt) {
        this.latestCommittedAt = latestCommittedAt;
    }

    public LocalDateTime getLatestCheckedAt() {
        return latestCheckedAt;
    }

    public void setLatestCheckedAt(LocalDateTime latestCheckedAt) {
        this.latestCheckedAt = latestCheckedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
