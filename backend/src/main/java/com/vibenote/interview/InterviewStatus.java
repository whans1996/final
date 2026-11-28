package com.vibenote.interview;

/** 면접 질문 생성 상태 (I-1a 3절). {@code NONE}은 행이 없다는 뜻으로 응답에만 쓴다. */
public enum InterviewStatus {
    NONE,
    PENDING,
    DONE,
    FAILED
}
