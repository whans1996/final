package com.vibenote.challenge;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** F-24 AC4 상태 계산. 시작일·종료일은 양 끝을 포함한다. */
class ChallengeStatusTest {

    static final LocalDate START = LocalDate.of(2026, 11, 20);
    static final LocalDate END = LocalDate.of(2026, 11, 30);

    @Test
    void AC4_오늘이_시작일보다_앞이면_예정이다() {
        assertThat(ChallengeStatus.of(START, END, START.minusDays(1)))
                .isEqualTo(ChallengeStatus.UPCOMING);
    }

    @Test
    void AC4_시작일과_종료일_당일은_진행_중이다() {
        assertThat(ChallengeStatus.of(START, END, START)).isEqualTo(ChallengeStatus.ONGOING);
        assertThat(ChallengeStatus.of(START, END, END)).isEqualTo(ChallengeStatus.ONGOING);
        assertThat(ChallengeStatus.of(START, START, START)).isEqualTo(ChallengeStatus.ONGOING);
    }

    @Test
    void AC4_종료일_다음_날부터는_종료다() {
        assertThat(ChallengeStatus.of(START, END, END.plusDays(1)))
                .isEqualTo(ChallengeStatus.ENDED);
    }
}
