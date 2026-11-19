package com.vibenote.quiz.domain;

/**
 * quiz_questions 행 (V1). 문항 번호는 같은 퀴즈 안에서 id 오름차순 순서다.
 *
 * @param conceptId 문항이 다루는 카드. 없거나 카드가 지워졌으면 null
 * @param choices 선택지 4개 (JSON 문자열 배열)
 * @param answerIndex 정답 위치 (0부터)
 */
public record QuizQuestion(
        long id,
        long quizId,
        Long conceptId,
        String question,
        String[] choices,
        int answerIndex,
        String explanation) {}
