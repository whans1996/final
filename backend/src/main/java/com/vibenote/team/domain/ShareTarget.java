package com.vibenote.team.domain;

import com.vibenote.analysis.AnalysisStatus;

/** 공유하려는 미삭제 분석의 주인·상태·퀴즈 유무 (F-20b AC2). */
public record ShareTarget(long userId, AnalysisStatus status, boolean hasQuiz) {}
