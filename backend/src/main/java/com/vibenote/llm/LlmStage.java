package com.vibenote.llm;

import java.util.Locale;

/**
 * LLM을 호출하는 분석 단계. 프롬프트는 {@code prompts/<stage>/<level>.md}, Mock 응답은 {@code
 * llm-fixtures/<stage>.json}.
 */
public enum LlmStage {
    OVERVIEW,
    FILE,
    FLOW,
    CONCEPT,
    QUIZ,
    RECOMMEND,
    /** 분석 파이프라인 밖, 사용자 요청 시 면접 예상 질문 (I-1a). */
    INTERVIEW;

    /** 리소스 경로에 쓰는 소문자 이름 (예: {@code overview}). */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
