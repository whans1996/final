package com.vibenote.team;

import com.vibenote.common.config.ClockConfig;
import com.vibenote.quiz.dto.LastAttemptResponse;
import com.vibenote.team.TeamUnderstandingCalculator.Member;
import com.vibenote.team.domain.PeriodAttempt;
import com.vibenote.team.dto.TeamChallengeMemberResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 팀 목표 상태·달성 계산 (F-20c AC5·AC6). DB를 보지 않는 순수 계산이다.
 *
 * <p>멤버 달성 = 기간 안 가장 최근 풀이의 {@code score × 100 ≥ 목표 정답률 × total} (반올림하지 않는다). 팀 달성 = 지금 멤버 전원 달성.
 */
final class TeamChallengeCalculator {

    record Result(
            int solvedMemberCount,
            int achievedMemberCount,
            boolean achieved,
            List<TeamChallengeMemberResponse> members) {}

    private TeamChallengeCalculator() {}

    static TeamChallengeStatus status(LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (today.isBefore(startDate)) return TeamChallengeStatus.UPCOMING;
        if (today.isAfter(endDate)) return TeamChallengeStatus.ENDED;
        return TeamChallengeStatus.ONGOING;
    }

    static boolean achieved(int score, int total, int targetRate) {
        return total > 0 && score * 100L >= (long) targetRate * total;
    }

    /** members 순서가 곧 응답 순서다. attemptsByUser에 멤버가 아닌 회원이 있으면 무시한다. */
    static Result evaluate(
            List<Member> members, Map<Long, PeriodAttempt> attemptsByUser, int targetRate) {
        List<TeamChallengeMemberResponse> rows = new ArrayList<>(members.size());
        int solved = 0;
        int achievedCount = 0;
        for (Member m : members) {
            PeriodAttempt a = attemptsByUser.get(m.userId());
            boolean ok = a != null && achieved(a.score(), a.total(), targetRate);
            if (a != null) solved++;
            if (ok) achievedCount++;
            rows.add(
                    new TeamChallengeMemberResponse(
                            m.userId(), m.nickname(), m.role(), toAttempt(a), ok));
        }
        boolean teamAchieved = !members.isEmpty() && achievedCount == members.size();
        return new Result(solved, achievedCount, teamAchieved, List.copyOf(rows));
    }

    private static LastAttemptResponse toAttempt(PeriodAttempt a) {
        if (a == null) return null;
        return new LastAttemptResponse(
                a.attemptId(),
                a.score(),
                a.total(),
                a.createdAt().atZone(ClockConfig.ZONE).toOffsetDateTime());
    }
}
