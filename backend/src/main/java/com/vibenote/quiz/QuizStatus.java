package com.vibenote.quiz;

/**
 * 분석의 퀴즈 상태 ({@code analyses.quiz_status}, L-1a AC4). QUIZ 단계를 끝내기 전에는 null이다.
 *
 * <p>GENERATING: 재생성 중 · READY: 퀴즈 있음 · FAILED: 생성 실패(빈 결과), 재생성할 수 있다
 */
public enum QuizStatus {
    GENERATING,
    READY,
    FAILED
}
