package com.vibenote.concept.domain;

import com.vibenote.common.Level;
import com.vibenote.concept.ConceptSource;
import com.vibenote.concept.ConceptStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** concepts 행. Controller 응답으로 직접 내보내지 않는다 (ConceptSummaryResponse 등 DTO로 변환). */
public class Concept {

    private Long id;
    private String name;
    private String category;
    private Level difficulty;
    private String summary;
    private String body;
    private String exampleCode;
    private ConceptStatus status;
    private ConceptSource source;
    private String referenceUrl;
    private int wishCount;
    private BigDecimal avgRating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isPublished() {
        return status == ConceptStatus.PUBLISHED;
    }

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Level getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Level difficulty) {
        this.difficulty = difficulty;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getExampleCode() {
        return exampleCode;
    }

    public void setExampleCode(String exampleCode) {
        this.exampleCode = exampleCode;
    }

    public ConceptStatus getStatus() {
        return status;
    }

    public void setStatus(ConceptStatus status) {
        this.status = status;
    }

    public ConceptSource getSource() {
        return source;
    }

    public void setSource(ConceptSource source) {
        this.source = source;
    }

    public String getReferenceUrl() {
        return referenceUrl;
    }

    public void setReferenceUrl(String referenceUrl) {
        this.referenceUrl = referenceUrl;
    }

    public int getWishCount() {
        return wishCount;
    }

    public void setWishCount(int wishCount) {
        this.wishCount = wishCount;
    }

    public BigDecimal getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(BigDecimal avgRating) {
        this.avgRating = avgRating;
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
