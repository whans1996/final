package com.vibenote.analysis.structure;

import java.time.Duration;

/**
 * 구조 계산 요청 하나의 작업 예산 (F-21a 5.3절 3). 토큰화·선언 읽기·호출 분류·URL 계산·경로 비교·Java 이름 해석의 반복마다 차감하고, 단계 수나 마감
 * 시각을 넘으면 {@link Exhausted}를 던진다. 한 번 다 쓰면 그 뒤의 차감도 모두 실패한다. 읽기 단계는 파일 단위로 잡아 남은 파일을 PARSE_ERROR로,
 * HTTP 판정은 남은 호출을 URL_UNKNOWN으로 두고 응답의 edgesTruncated를 켠다.
 *
 * <p>식·비교 단위 상한(피연산자 2,000개 등)은 그대로 두고, 이 예산은 그 상한들이 곱해져도 요청 하나가 끝나도록 막는 바깥 울타리다.
 */
public final class WorkBudget {

    /** 예산을 다 써서 읽지 않은 파일의 PARSE_ERROR detail. */
    public static final String EXHAUSTED_DETAIL = "계산량 상한을 넘어 해석하지 않았습니다.";

    /** 마감 시각은 이만큼 차감할 때마다 확인한다 ({@code System.nanoTime} 호출을 줄인다). */
    private static final long CLOCK_EVERY = 1_024;

    private final long deadline;
    private final boolean timed;
    private long remaining;
    private long sinceClock;
    private boolean exhausted;

    /**
     * @param maxSteps 차감할 수 있는 단계 수
     * @param maxTime 만든 때부터의 계산 시간. null이면 시간 제한 없음
     */
    public WorkBudget(long maxSteps, Duration maxTime) {
        this.remaining = maxSteps;
        this.timed = maxTime != null;
        long nanos = maxTime == null ? 0 : saturatedNanos(maxTime);
        this.deadline = System.nanoTime() + nanos;
    }

    /** 단위 테스트용. 단계·시간 제한이 없다. */
    public static WorkBudget unlimited() {
        return new WorkBudget(Long.MAX_VALUE, null);
    }

    public void spend() {
        spend(1);
    }

    /** steps만큼 차감한다. 다 썼으면 {@link Exhausted}. */
    public void spend(long steps) {
        if (exhausted) {
            throw new Exhausted();
        }
        remaining -= steps;
        sinceClock += steps;
        if (remaining < 0 || (timed && sinceClock >= CLOCK_EVERY && clockExpired())) {
            exhausted = true;
            throw new Exhausted();
        }
    }

    public boolean exhausted() {
        return exhausted;
    }

    private boolean clockExpired() {
        sinceClock = 0;
        return System.nanoTime() - deadline >= 0;
    }

    /** 아주 긴 시간도 넘치지 않게 나노초로 (계산 시간 상한은 몇 초다). */
    private static long saturatedNanos(Duration duration) {
        try {
            return Math.min(duration.toNanos(), Long.MAX_VALUE / 2);
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE / 2;
        }
    }

    /** 예산을 다 썼다. 스택 트레이스를 만들지 않는다 (흐름 제어용). */
    public static final class Exhausted extends RuntimeException {
        Exhausted() {
            super(EXHAUSTED_DETAIL, null, false, false);
        }
    }
}
