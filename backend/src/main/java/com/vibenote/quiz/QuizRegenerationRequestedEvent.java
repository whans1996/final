package com.vibenote.quiz;

/** 퀴즈 재생성 요청이 커밋되면 실행기에 넘긴다 (L-1a AC5, {@link QuizRegenerationLauncher}). */
public record QuizRegenerationRequestedEvent(long analysisId) {}
