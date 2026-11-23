package com.vibenote.team;

/** 팀 목표 상태 (F-20c AC6). 오늘(Asia/Seoul) < 시작일 예정, 시작일 ≤ 오늘 ≤ 종료일 진행 중, 오늘 > 종료일 종료. */
public enum TeamChallengeStatus {
    UPCOMING,
    ONGOING,
    ENDED
}
