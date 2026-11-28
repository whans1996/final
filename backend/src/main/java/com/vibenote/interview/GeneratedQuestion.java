package com.vibenote.interview;

import java.util.List;

/**
 * 검증을 마친 질문 하나 (AC3). 저장 직전 값이라 {@code no}는 1부터 다시 매긴 번호다.
 *
 * @param keyPoints 1~5개
 * @param evidence 1~3개
 * @param conceptIds 0~3개, 프롬프트 개념 목록 안의 id
 */
public record GeneratedQuestion(
        int no,
        InterviewCategory category,
        String question,
        List<String> keyPoints,
        List<InterviewEvidence> evidence,
        List<Long> conceptIds) {}
