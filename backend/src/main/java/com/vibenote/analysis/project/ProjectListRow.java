package com.vibenote.analysis.project;

import com.vibenote.analysis.SourceType;
import java.time.LocalDateTime;

/** 프로젝트 목록 한 행 (AC6). {@code lastAnalyzedAt}은 삭제하지 않은 분석 중 가장 늦은 요청 시각이다. */
public class ProjectListRow {

    private Long id;
    private String name;
    private SourceType sourceType;
    private String sourceUrl;
    private LocalDateTime lastAnalyzedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public LocalDateTime getLastAnalyzedAt() {
        return lastAnalyzedAt;
    }

    public void setLastAnalyzedAt(LocalDateTime lastAnalyzedAt) {
        this.lastAnalyzedAt = lastAnalyzedAt;
    }
}
