package com.vibenote.feed;

/**
 * activities.type (V2). 피드(F-04)에 보이는 활동 종류.
 *
 * <p>새 종류는 V 마이그레이션의 ENUM과 함께 <b>자기 도메인 블록 안에만</b> 추가하고 모든 항목 끝에 쉼표를 단다 (docs/conventions.md 9절).
 */
public enum ActivityType {
    // ── ANALYSIS (A F-05·F-19) ──
    ANALYSIS,

    // ── CONCEPT (B F-11·F-12) ──
    REVIEW,
    WISH,

    // ── POST (B F-17) ──
    POST,

    // ── CHALLENGE (A F-25) ──
    CHALLENGE_JOIN,
    CHALLENGE_COMPLETE,
    ;
}
