package com.vibenote.team.domain;

import java.time.LocalDateTime;

/** 지금 멤버의 가장 최근 풀이와 그 답 한 개 (F-20b AC6). 답이 없는 풀이면 {@code questionId}·{@code correct}가 null이다. */
public record MemberAnswer(
        long userId,
        long attemptId,
        int score,
        int total,
        LocalDateTime createdAt,
        Long questionId,
        Boolean correct) {}
