package com.vibenote.analysis.domain;

import com.vibenote.analysis.pipeline.SelectedFile;

/** analysis_files 행. role·summary는 EXPLAINING 단계가 채운다 (F-06). 목록 조회에서는 content가 null이다. */
public class AnalysisFile {

    private Long id;
    private long analysisId;
    private String path;
    private String layer;
    private String role;
    private String content;
    private String summary;

    public static AnalysisFile of(long analysisId, SelectedFile file) {
        AnalysisFile row = new AnalysisFile();
        row.analysisId = analysisId;
        row.path = file.path();
        row.layer = file.layer().value();
        row.content = file.content();
        return row;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(long analysisId) {
        this.analysisId = analysisId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
