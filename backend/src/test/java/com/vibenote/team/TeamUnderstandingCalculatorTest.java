package com.vibenote.team;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.team.TeamUnderstandingCalculator.Member;
import com.vibenote.team.TeamUnderstandingCalculator.Result;
import com.vibenote.team.domain.MemberAnswer;
import com.vibenote.team.domain.QuestionLayer;
import com.vibenote.team.dto.ModuleUnderstandingResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** F-20b 팀 이해도 계산 (AC6·AC7). */
class TeamUnderstandingCalculatorTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 11, 25, 15, 0);
    private static final Member A = new Member(1, "a", TeamRole.OWNER);
    private static final Member B = new Member(2, "b", TeamRole.MEMBER);
    private static final Member C = new Member(3, "c", TeamRole.MEMBER);

    /** userId의 풀이 하나를 문항 id 1..n의 정오로 만든다. */
    private static List<MemberAnswer> attempt(long userId, boolean... correct) {
        int score = 0;
        for (boolean c : correct) if (c) score++;
        List<MemberAnswer> rows = new ArrayList<>();
        for (int i = 0; i < correct.length; i++) {
            rows.add(
                    new MemberAnswer(
                            userId, 100 + userId, score, correct.length, AT, i + 1L, correct[i]));
        }
        return rows;
    }

    private static List<MemberAnswer> concat(List<MemberAnswer> a, List<MemberAnswer> b) {
        List<MemberAnswer> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }

    @Test
    void AC6_레이어_순서대로_문항이_있는_모듈만_주고_모르는_레이어와_null은_기타다() {
        List<QuestionLayer> questions =
                List.of(
                        new QuestionLayer(1, "vue"),
                        new QuestionLayer(2, null),
                        new QuestionLayer(3, "controller"),
                        new QuestionLayer(4, "weird"),
                        new QuestionLayer(5, "domain"));

        Result r = TeamUnderstandingCalculator.calculate(questions, List.of(A), List.of());

        assertThat(r.modules())
                .extracting(ModuleUnderstandingResponse::layer)
                .containsExactly("controller", "vue", "domain", "other");
        assertThat(r.modules().get(3).questionCount()).isEqualTo(2);
    }

    @Test
    void AC6_정답률은_푼_멤버들의_맞힌_수_합을_문항_수_합으로_나눠_반올림한다() {
        List<QuestionLayer> questions =
                List.of(
                        new QuestionLayer(1, "service"),
                        new QuestionLayer(2, "service"),
                        new QuestionLayer(3, "service"));
        // A 2/3, B 0/3, C 안 풂 → 2/6 = 33.3 → 33 / A 2/3 → 66.7 → 67
        Result two =
                TeamUnderstandingCalculator.calculate(
                        questions,
                        List.of(A, B, C),
                        concat(attempt(1, true, true, false), attempt(2, false, false, false)));
        Result one =
                TeamUnderstandingCalculator.calculate(
                        questions, List.of(A, B, C), attempt(1, true, true, false));

        assertThat(two.solvedMemberCount()).isEqualTo(2);
        assertThat(two.modules().get(0).correctRate()).isEqualTo(33);
        assertThat(one.modules().get(0).correctRate()).isEqualTo(67);
        assertThat(two.members().get(0).lastAttempt().score()).isEqualTo(2);
        assertThat(two.members().get(0).modules().get(0).correct()).isEqualTo(2);
        assertThat(two.members().get(0).modules().get(0).total()).isEqualTo(3);
        assertThat(two.members().get(2).lastAttempt()).isNull();
        assertThat(two.members().get(2).modules()).isEmpty();
    }

    @Test
    void AC7_아무도_풀지_않았으면_정답률이_null이고_멤버_순서는_입력_순서다() {
        Result r =
                TeamUnderstandingCalculator.calculate(
                        List.of(new QuestionLayer(1, "mapper")), List.of(B, A), List.of());

        assertThat(r.solvedMemberCount()).isZero();
        assertThat(r.modules().get(0).correctRate()).isNull();
        assertThat(r.members()).extracting(m -> m.userId()).containsExactly(2L, 1L);
    }
}
