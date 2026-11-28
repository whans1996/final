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
    private String summaryBeginner;
    private String summaryJunior;
    private String summarySenior;
    private String body;
    private String bodyBeginner;
    private String bodyJunior;
    private String bodySenior;
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

    /** 그 수준의 한 줄 요약 (K-1). 없으면 null. */
    public String summaryFor(Level level) {
        return switch (level) {
            case BEGINNER -> summaryBeginner;
            case JUNIOR -> summaryJunior;
            case SENIOR -> summarySenior;
        };
    }

    /** 그 수준의 본문 (K-1). 없으면 null. */
    public String bodyFor(Level level) {
        return switch (level) {
            case BEGINNER -> bodyBeginner;
            case JUNIOR -> bodyJunior;
            case SENIOR -> bodySenior;
        };
    }

    /** 그 수준의 요약·본문을 바꾼다 (K-1). */
    public void setLevelText(Level level, String summary, String body) {
        switch (level) {
            case BEGINNER -> {
                summaryBeginner = summary;
                bodyBeginner = body;
            }
            case JUNIOR -> {
                summaryJunior = summary;
                bodyJunior = body;
            }
            case SENIOR -> {
                summarySenior = summary;
                bodySenior = body;
            }
        }
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

    public String getSummaryBeginner() {
        return summaryBeginner;
    }

    public void setSummaryBeginner(String summaryBeginner) {
        this.summaryBeginner = summaryBeginner;
    }

    public String getSummaryJunior() {
        return summaryJunior;
    }

    public void setSummaryJunior(String summaryJunior) {
        this.summaryJunior = summaryJunior;
    }

    public String getSummarySenior() {
        return summarySenior;
    }

    public void setSummarySenior(String summarySenior) {
        this.summarySenior = summarySenior;
    }

    public String getBodyBeginner() {
        return bodyBeginner;
    }

    public void setBodyBeginner(String bodyBeginner) {
        this.bodyBeginner = bodyBeginner;
    }

    public String getBodyJunior() {
        return bodyJunior;
    }

    public void setBodyJunior(String bodyJunior) {
        this.bodyJunior = bodyJunior;
    }

    public String getBodySenior() {
        return bodySenior;
    }

    public void setBodySenior(String bodySenior) {
        this.bodySenior = bodySenior;
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
