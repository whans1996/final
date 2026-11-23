package com.vibenote.team;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.team.TeamChallengeCalculator.Result;
import com.vibenote.team.TeamUnderstandingCalculator.Member;
import com.vibenote.team.domain.PeriodAttempt;
import com.vibenote.team.dto.TeamChallengeMemberResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** F-20c 팀 목표 계산 (AC5·AC6). */
class TeamChallengeCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);
    private static final LocalDateTime AT = LocalDateTime.of(2026, 11, 24, 15, 0);
    private static final Member A = new Member(1, "a", TeamRole.OWNER);
    private static final Member B = new Member(2, "b", TeamRole.MEMBER);
    private static final Member C = new Member(3, "c", TeamRole.MEMBER);

    private static PeriodAttempt attempt(long userId, int score, int total) {
        return new PeriodAttempt(7, userId, 100 + userId, score, total, AT);
    }

    @Test
    void AC6_오늘이_시작일_전이면_예정_기간_안이면_진행_중_종료일_뒤면_종료다() {
        assertThat(TeamChallengeCalculator.status(TODAY.plusDays(1), TODAY.plusDays(6), TODAY))
                .isEqualTo(TeamChallengeStatus.UPCOMING);
        assertThat(TeamChallengeCalculator.status(TODAY, TODAY, TODAY))
                .isEqualTo(TeamChallengeStatus.ONGOING);
        assertThat(TeamChallengeCalculator.status(TODAY.minusDays(3), TODAY, TODAY))
                .isEqualTo(TeamChallengeStatus.ONGOING);
        assertThat(TeamChallengeCalculator.status(TODAY.minusDays(7), TODAY.minusDays(1), TODAY))
                .isEqualTo(TeamChallengeStatus.ENDED);
    }

    @Test
    void AC5_정답률이_목표와_같으면_달성이고_반올림하지_않는다() {
        assertThat(TeamChallengeCalculator.achieved(4, 5, 80)).isTrue();
        assertThat(TeamChallengeCalculator.achieved(3, 5, 80)).isFalse();
        // 2/3 = 66.67% → 67%로 반올림하지 않는다
        assertThat(TeamChallengeCalculator.achieved(2, 3, 67)).isFalse();
        assertThat(TeamChallengeCalculator.achieved(2, 3, 66)).isTrue();
        assertThat(TeamChallengeCalculator.achieved(5, 5, 100)).isTrue();
        assertThat(TeamChallengeCalculator.achieved(0, 5, 1)).isFalse();
    }

    @Test
    void AC5_멤버_순서를_지키고_풀이_없는_멤버는_attempt_null_미달성이다() {
        Result r =
                TeamChallengeCalculator.evaluate(
                        List.of(A, B, C), Map.of(1L, attempt(1, 3, 5), 2L, attempt(2, 4, 5)), 80);

        assertThat(r.members())
                .extracting(TeamChallengeMemberResponse::userId)
                .containsExactly(1L, 2L, 3L);
        assertThat(r.members())
                .extracting(TeamChallengeMemberResponse::achieved)
                .containsExactly(false, true, false);
        assertThat(r.members().get(0).attempt().score()).isEqualTo(3);
        assertThat(r.members().get(0).attempt().attemptId()).isEqualTo(101);
        assertThat(r.members().get(2).attempt()).isNull();
        assertThat(r.solvedMemberCount()).isEqualTo(2);
        assertThat(r.achievedMemberCount()).isEqualTo(1);
        assertThat(r.achieved()).isFalse();
    }

    @Test
    void AC5_지금_멤버_전원이_달성하면_팀_달성이고_멤버가_아닌_풀이는_무시한다() {
        Result r =
                TeamChallengeCalculator.evaluate(
                        List.of(A, B),
                        Map.of(1L, attempt(1, 5, 5), 2L, attempt(2, 4, 5), 9L, attempt(9, 0, 5)),
                        80);

        assertThat(r.achievedMemberCount()).isEqualTo(2);
        assertThat(r.solvedMemberCount()).isEqualTo(2);
        assertThat(r.achieved()).isTrue();
    }

    @Test
    void AC6_아무도_풀지_않았으면_팀_달성이_아니다() {
        Result r = TeamChallengeCalculator.evaluate(List.of(A), Map.of(), 1);

        assertThat(r.solvedMemberCount()).isZero();
        assertThat(r.achieved()).isFalse();
    }
}
