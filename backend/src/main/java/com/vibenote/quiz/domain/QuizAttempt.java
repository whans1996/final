package com.vibenote.quiz.domain;

import java.time.LocalDateTime;

/** quiz_attempts 행 (V1). {@code score}는 맞힌 문항 수, {@code total}은 문항 수. */
public class QuizAttempt {

    private Long id;
    private long quizId;
    private long userId;
    private int score;
    private int total;
    private LocalDateTime createdAt;

    public static QuizAttempt of(
            long quizId, long userId, int score, int total, LocalDateTime now) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.quizId = quizId;
        attempt.userId = userId;
        attempt.score = score;
        attempt.total = total;
        attempt.createdAt = now;
        return attempt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getQuizId() {
        return quizId;
    }

    public void setQuizId(long quizId) {
        this.quizId = quizId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
