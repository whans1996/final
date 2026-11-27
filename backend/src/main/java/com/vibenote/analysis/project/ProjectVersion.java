package com.vibenote.analysis.project;

import com.vibenote.analysis.fetch.RepositoryCommit;
import java.time.LocalDateTime;

/** project_versions 행 (P-1a 4절). 커밋 정보가 없는 버전(ZIP·커밋 조회 실패·백필)은 커밋 필드가 모두 null이다. */
public class ProjectVersion {

    private Long id;
    private Long projectId;
    private int number;
    private String commitSha;
    private String commitMessage;
    private LocalDateTime committedAt;
    private LocalDateTime createdAt;

    static ProjectVersion of(
            long projectId, int number, RepositoryCommit commit, LocalDateTime createdAt) {
        ProjectVersion version = new ProjectVersion();
        version.projectId = projectId;
        version.number = number;
        if (commit != null) {
            version.commitSha = commit.sha();
            version.commitMessage = commit.message();
            version.committedAt = commit.committedAt();
        }
        version.createdAt = createdAt;
        return version;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public LocalDateTime getCommittedAt() {
        return committedAt;
    }

    public void setCommittedAt(LocalDateTime committedAt) {
        this.committedAt = committedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
