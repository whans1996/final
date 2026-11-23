package com.vibenote.team.domain;

import com.vibenote.common.Level;
import java.time.LocalDateTime;

/**
 * 팀 공유 분석 한 행 (team_analyses + analyses + 공유한 사람 닉네임 + 퀴즈 요약 + 요청자의 최근 풀이, F-20b). 요청자가 풀지 않았으면
 * {@code my*}가 null이다. Controller 응답으로 직접 내보내지 않는다.
 */
public class TeamAnalysis {

    private Long analysisId;
    private String title;
    private String projectName;
    private Level level;
    private Long sharedByUserId;
    private String sharedByNickname;
    private LocalDateTime sharedAt;
    private Long quizId;
    private int questionCount;
    private int solvedMemberCount;
    private Long myAttemptId;
    private Integer myScore;
    private Integer myTotal;
    private LocalDateTime myCreatedAt;

    public Long getAnalysisId() {
        return analysisId;
    }

    public String getTitle() {
        return title;
    }

    public String getProjectName() {
        return projectName;
    }

    public Level getLevel() {
        return level;
    }

    public Long getSharedByUserId() {
        return sharedByUserId;
    }

    public String getSharedByNickname() {
        return sharedByNickname;
    }

    public LocalDateTime getSharedAt() {
        return sharedAt;
    }

    public Long getQuizId() {
        return quizId;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public int getSolvedMemberCount() {
        return solvedMemberCount;
    }

    public Long getMyAttemptId() {
        return myAttemptId;
    }

    public Integer getMyScore() {
        return myScore;
    }

    public Integer getMyTotal() {
        return myTotal;
    }

    public LocalDateTime getMyCreatedAt() {
        return myCreatedAt;
    }
}
