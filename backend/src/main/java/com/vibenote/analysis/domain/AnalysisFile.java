package com.vibenote.analysis.domain;

import com.vibenote.analysis.pipeline.SelectedFile;

/**
 * analysis_files 행. role·summary는 EXPLAINING 단계가 채운다 (F-06). 목록 조회에서는 content가 null이다. 인덱스 전체가 저장되고
 * 설명 대상은 selected인 행이다 (F-26a). 조회 쿼리는 selected·importance를 읽지 않는다.
 */
public class AnalysisFile {

    private Long id;
    private long analysisId;
    private String path;
    private String layer;
    private boolean selected = true;
    private Integer importance;
    private String role;
    private String content;
    private String summary;

    public static AnalysisFile of(long analysisId, SelectedFile file) {
        AnalysisFile row = new AnalysisFile();
        row.analysisId = analysisId;
        row.path = file.path();
        row.layer = file.layer().value();
        row.selected = file.selected();
        row.importance = file.importance();
        row.content = file.content();
        return row;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Integer getImportance() {
        return importance;
    }

    public void setImportance(Integer importance) {
        this.importance = importance;
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
