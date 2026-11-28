package com.vibenote.quiz.domain;

import java.time.LocalDateTime;

/**
 * 오답 노트 쿼리의 행 (L-1b, {@code QuizMapper.findWrongNotes}). 마지막 답이 틀린 문항 하나. {@code analysisTitle}은 분석
 * 제목 → 프로젝트 이름이고 둘 다 비면 null이다.
 */
public record WrongNoteRow(
        long questionId,
        long quizId,
        long analysisId,
        String analysisTitle,
        Long conceptId,
        String question,
        String[] choices,
        int answerIndex,
        String explanation,
        int selectedIndex,
        int wrongCount,
        LocalDateTime lastAnsweredAt) {}
