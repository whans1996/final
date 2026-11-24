package com.vibenote.post.domain;

import com.vibenote.post.AttachmentType;
import com.vibenote.post.BoardType;
import java.time.LocalDateTime;

/**
 * posts 행 + 작성자 닉네임·상태(users JOIN) + 댓글 수. 상세 조회는 첨부한 분석·카드의 표시용 컬럼(LEFT JOIN)도 채운다. Controller
 * 응답으로 직접 내보내지 않는다.
 */
public class Post {

    private Long id;
    private long userId;
    private BoardType boardType;
    private String title;
    private String content;
    private Long analysisId;
    private Long conceptId;
    private int viewCount;
    private long commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String authorNickname;
    private String authorStatus;

    // 상세 조회에서만 채운다 (첨부 표시·viewable 판단)
    private Long analysisOwnerId;
    private boolean analysisPublic;
    private boolean analysisOwnerActive;
    private boolean analysisDeleted;
    private String analysisTitle;
    private String analysisProjectName;
    private String conceptStatus;
    private String conceptName;
    private String conceptCategory;
    private String conceptDifficulty;
    private String conceptSummary;

    public Post() {}

    /** 새 글 (insert용). created_at·updated_at은 같은 시각으로 넣는다. */
    public Post(
            long userId,
            BoardType boardType,
            String title,
            String content,
            Long analysisId,
            Long conceptId,
            LocalDateTime createdAt) {
        this.userId = userId;
        this.boardType = boardType;
        this.title = title;
        this.content = content;
        this.analysisId = analysisId;
        this.conceptId = conceptId;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public boolean isAuthorWithdrawn() {
        return "WITHDRAWN".equals(authorStatus);
    }

    public AttachmentType getAttachmentType() {
        if (analysisId != null) return AttachmentType.ANALYSIS;
        if (conceptId != null) return AttachmentType.CONCEPT;
        return null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public BoardType getBoardType() {
        return boardType;
    }

    public void setBoardType(BoardType boardType) {
        this.boardType = boardType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Long analysisId) {
        this.analysisId = analysisId;
    }

    public Long getConceptId() {
        return conceptId;
    }

    public void setConceptId(Long conceptId) {
        this.conceptId = conceptId;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(long commentCount) {
        this.commentCount = commentCount;
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

    public String getAuthorNickname() {
        return authorNickname;
    }

    public void setAuthorNickname(String authorNickname) {
        this.authorNickname = authorNickname;
    }

    public String getAuthorStatus() {
        return authorStatus;
    }

    public void setAuthorStatus(String authorStatus) {
        this.authorStatus = authorStatus;
    }

    public Long getAnalysisOwnerId() {
        return analysisOwnerId;
    }

    public void setAnalysisOwnerId(Long analysisOwnerId) {
        this.analysisOwnerId = analysisOwnerId;
    }

    public boolean isAnalysisPublic() {
        return analysisPublic;
    }

    public void setAnalysisPublic(boolean analysisPublic) {
        this.analysisPublic = analysisPublic;
    }

    public boolean isAnalysisOwnerActive() {
        return analysisOwnerActive;
    }

    public void setAnalysisOwnerActive(boolean analysisOwnerActive) {
        this.analysisOwnerActive = analysisOwnerActive;
    }

    public boolean isAnalysisDeleted() {
        return analysisDeleted;
    }

    public void setAnalysisDeleted(boolean analysisDeleted) {
        this.analysisDeleted = analysisDeleted;
    }

    public String getAnalysisTitle() {
        return analysisTitle;
    }

    public void setAnalysisTitle(String analysisTitle) {
        this.analysisTitle = analysisTitle;
    }

    public String getAnalysisProjectName() {
        return analysisProjectName;
    }

    public void setAnalysisProjectName(String analysisProjectName) {
        this.analysisProjectName = analysisProjectName;
    }

    public String getConceptStatus() {
        return conceptStatus;
    }

    public void setConceptStatus(String conceptStatus) {
        this.conceptStatus = conceptStatus;
    }

    public String getConceptName() {
        return conceptName;
    }

    public void setConceptName(String conceptName) {
        this.conceptName = conceptName;
    }

    public String getConceptCategory() {
        return conceptCategory;
    }

    public void setConceptCategory(String conceptCategory) {
        this.conceptCategory = conceptCategory;
    }

    public String getConceptDifficulty() {
        return conceptDifficulty;
    }

    public void setConceptDifficulty(String conceptDifficulty) {
        this.conceptDifficulty = conceptDifficulty;
    }

    public String getConceptSummary() {
        return conceptSummary;
    }

    public void setConceptSummary(String conceptSummary) {
        this.conceptSummary = conceptSummary;
    }
}
