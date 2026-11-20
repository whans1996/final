package com.vibenote.quiz;

import java.util.List;

/**
 * 검증·셔플을 마친 문항 하나 (F-14 AC3). 저장 순서가 곧 문항 번호다.
 *
 * @param conceptId 이번 개념 목록에 있는 카드 id. 없으면 null
 * @param choices 4개, 섞은 뒤의 순서
 * @param answerIndex 섞은 뒤의 정답 위치 (0~3)
 */
public record GeneratedQuestion(
        Long conceptId,
        String question,
        List<String> choices,
        int answerIndex,
        String explanation) {}
