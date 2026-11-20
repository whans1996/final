package com.vibenote.concept.review.domain;

import java.time.LocalDateTime;

/** concept_reviews 행 + 작성자 닉네임·상태 (users JOIN). Controller 응답으로 직접 내보내지 않는다. */
public class ConceptReview {

    private Long id;
    private long conceptId;
    private long userId;
    private int rating;
    private String content;
    private LocalDateTime createdAt;
    private String authorNickname;
    private String authorStatus;

    public ConceptReview() {}

    public ConceptReview(
            long conceptId, long userId, int rating, String content, LocalDateTime createdAt) {
        this.conceptId = conceptId;
        this.userId = userId;
        this.rating = rating;
        this.content = content;
        this.createdAt = createdAt;
    }

    public boolean isAuthorWithdrawn() {
        return "WITHDRAWN".equals(authorStatus);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getConceptId() {
        return conceptId;
    }

    public void setConceptId(long conceptId) {
        this.conceptId = conceptId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
}
