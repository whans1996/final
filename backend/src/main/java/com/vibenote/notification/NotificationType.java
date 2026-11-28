package com.vibenote.notification;

/**
 * notifications.type (V18, VARCHAR(40)). 헤더 알림 종류 (NT-1·NT-2).
 *
 * <p>새 종류는 <b>자기 도메인 블록 안에만</b> 추가하고 모든 항목 끝에 쉼표를 단다 ({@code ActivityType}과 같은 규칙,
 * docs/conventions.md 9절). 컬럼이 VARCHAR라 마이그레이션은 필요 없고, FE는 모르는 종류도 기본 아이콘으로 그린다.
 */
public enum NotificationType {
    // ── SOCIAL (B NT-1) ──
    COMMENT,
    FOLLOW,
    CHALLENGE_COMPLETE,

// ── ANALYSIS·REVIEW·TEAM (A NT-2) ──
;
}
