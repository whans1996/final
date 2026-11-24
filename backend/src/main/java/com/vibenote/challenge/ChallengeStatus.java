package com.vibenote.challenge;

import java.time.LocalDate;

/** 챌린지 상태 (F-24). 저장하지 않고 오늘(Asia/Seoul)과 기간을 비교해 계산한다. 기간은 양 끝을 포함한다 (F-20c 팀 목표와 같은 기준). */
public enum ChallengeStatus {
    /** 오늘 &lt; 시작일 */
    UPCOMING,
    /** 시작일 ≤ 오늘 ≤ 종료일 */
    ONGOING,
    /** 종료일 &lt; 오늘 */
    ENDED;

    public static ChallengeStatus of(LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (today.isBefore(startDate)) return UPCOMING;
        if (today.isAfter(endDate)) return ENDED;
        return ONGOING;
    }
}
