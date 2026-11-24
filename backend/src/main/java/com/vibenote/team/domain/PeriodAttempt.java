package com.vibenote.team.domain;

import java.time.LocalDateTime;

/** 팀 목표 기간 안에 지금 멤버가 푼 가장 최근 풀이 (F-20c AC5). 목표·멤버마다 많아야 하나다. */
public record PeriodAttempt(
        long challengeId,
        long userId,
        long attemptId,
        int score,
        int total,
        LocalDateTime createdAt) {}
