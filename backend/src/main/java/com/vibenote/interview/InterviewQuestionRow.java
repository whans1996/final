package com.vibenote.interview;

/** interview_questions 행 (V17). JSON 열은 배열로 읽는다. */
public class InterviewQuestionRow {

    private Long id;
    private long interviewId;
    private int no;
    private InterviewCategory category;
    private String question;
    private String[] keyPoints;
    private InterviewEvidence[] evidence;
    private Long[] conceptIds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(long interviewId) {
        this.interviewId = interviewId;
    }

    public int getNo() {
        return no;
    }

    public void setNo(int no) {
        this.no = no;
    }

    public InterviewCategory getCategory() {
        return category;
    }

    public void setCategory(InterviewCategory category) {
        this.category = category;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String[] getKeyPoints() {
        return keyPoints;
    }

    public void setKeyPoints(String[] keyPoints) {
        this.keyPoints = keyPoints;
    }

    public InterviewEvidence[] getEvidence() {
        return evidence;
    }

    public void setEvidence(InterviewEvidence[] evidence) {
        this.evidence = evidence;
    }

    public Long[] getConceptIds() {
        return conceptIds;
    }

    public void setConceptIds(Long[] conceptIds) {
        this.conceptIds = conceptIds;
    }
}
