package com.vibenote.concept.domain;

/**
 * 내 분석에 카드가 등장한 위치 한 줄 (F-10, analysis_concepts ⋈ analyses ⋈ analysis_files). analysisTitle은 {@code
 * COALESCE(title, project_name)}이라 null일 수 있다.
 */
public class ConceptAppearance {

    private long analysisId;
    private String analysisTitle;
    private Long fileId;
    private String filePath;
    private String lineHint;
    private String contextNote;

    public long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(long analysisId) {
        this.analysisId = analysisId;
    }

    public String getAnalysisTitle() {
        return analysisTitle;
    }

    public void setAnalysisTitle(String analysisTitle) {
        this.analysisTitle = analysisTitle;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getLineHint() {
        return lineHint;
    }

    public void setLineHint(String lineHint) {
        this.lineHint = lineHint;
    }

    public String getContextNote() {
        return contextNote;
    }

    public void setContextNote(String contextNote) {
        this.contextNote = contextNote;
    }
}
