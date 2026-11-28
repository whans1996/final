package com.vibenote.interview;

/** 면접 질문 생성이 요청됨 (새 행, FAILED 다시 시도, DONE 다시 만들기). 요청 트랜잭션이 커밋된 뒤 생성을 시작하는 데 쓴다. */
public record InterviewRequestedEvent(long interviewId) {}
