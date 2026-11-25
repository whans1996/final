package com.vibenote.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.QueryTimeoutException;

/**
 * 끝난 챌린지의 미달성 참여 다시 세기 (#146). 분석 완료 집계가 실패한 뒤 기간 안에 다음 분석이 없으면 남던 progress 부족을, 챌린지가 끝난 뒤 하루 한 번
 * 도는 일괄 작업이 바로잡는다. 오늘은 2026-11-24다.
 */
class ChallengeRecountJobTest extends ChallengeTestSupport {

    @Autowired ChallengeRecountJob job;

    @Test
    void 어제_끝난_챌린지의_놓친_분석을_세고_목표에_닿으면_달성한다() {
        long u = insertUser("f25u");
        long c = insertChallenge("C", "2026-11-17", "2026-11-23", "ANALYSIS_COUNT", 2);
        insertParticipant(c, u);
        insertAnalysis(u, "DONE", "2026-11-20 09:00:00", false);
        insertAnalysis(u, "DONE", "2026-11-23 23:30:00", false);

        job.recountEnded();

        assertThat(participant(c, u).get("progress")).isEqualTo(2);
        assertThat(participant(c, u).get("completed_at")).isEqualTo(FIXED_NOW);
        assertThat(activityCount(u, "CHALLENGE_COMPLETE", c)).isEqualTo(1);

        job.recountEnded();

        assertThat(activityCount(u, "CHALLENGE_COMPLETE", c)).isEqualTo(1);
    }

    @Test
    void 목표에_못_미치면_progress만_바꾼다() {
        long u = insertUser("f25u");
        long c = insertChallenge("C", "2026-11-17", "2026-11-23", "ANALYSIS_COUNT", 5);
        insertParticipant(c, u);
        insertAnalysis(u, "DONE", "2026-11-23 09:00:00", false);

        job.recountEnded();

        assertThat(progressOf(c, u)).isEqualTo(1);
        assertThat(participant(c, u).get("completed_at")).isNull();
        assertThat(activityCount(u, "CHALLENGE_COMPLETE", c)).isZero();
    }

    @Test
    void 끝난_지_7일_안의_챌린지만_다시_세고_진행_중인_챌린지는_두지_않는다() {
        long u = insertUser("f25u");
        long sevenDays = insertChallenge("7일 전", "2026-11-10", "2026-11-17", "ANALYSIS_COUNT", 5);
        long eightDays = insertChallenge("8일 전", "2026-11-10", "2026-11-16", "ANALYSIS_COUNT", 5);
        long ongoing = insertChallenge("진행 중", "2026-11-10", "2026-11-24", "ANALYSIS_COUNT", 5);
        insertParticipant(sevenDays, u);
        insertParticipant(eightDays, u);
        insertParticipant(ongoing, u);
        insertAnalysis(u, "DONE", "2026-11-12 09:00:00", false);

        job.recountEnded();

        assertThat(progressOf(sevenDays, u)).isEqualTo(1);
        assertThat(progressOf(eightDays, u)).isZero();
        assertThat(progressOf(ongoing, u)).isZero();
    }

    @Test
    void 이미_달성한_참여는_다시_세지_않는다() {
        long u = insertUser("f25u");
        long c = insertChallenge("C", "2026-11-17", "2026-11-23", "ANALYSIS_COUNT", 1);
        insertParticipant(c, u, 1, "2026-11-20 10:00:00", "2026-11-17 09:00:00");
        insertAnalysis(u, "DONE", "2026-11-20 09:00:00", false);
        insertAnalysis(u, "DONE", "2026-11-21 09:00:00", false);

        job.recountEnded();

        assertThat(progressOf(c, u)).isEqualTo(1);
        assertThat(participant(c, u).get("completed_at"))
                .isEqualTo(LocalDateTime.of(2026, 11, 20, 10, 0));
        assertThat(activityCount(u, "CHALLENGE_COMPLETE", c)).isZero();
    }

    @Test
    void 한_참여의_집계가_실패해도_나머지는_센다() {
        long u = insertUser("f25u");
        long failing = insertChallenge("실패", "2026-11-17", "2026-11-23", "ANALYSIS_COUNT", 5);
        long other = insertChallenge("정상", "2026-11-17", "2026-11-23", "ANALYSIS_COUNT", 5);
        insertParticipant(failing, u);
        insertParticipant(other, u);
        insertAnalysis(u, "DONE", "2026-11-23 09:00:00", false);
        doThrow(new QueryTimeoutException("잠금 대기 시간 초과"))
                .when(progressService)
                .recountOpen(eq(failing), anyLong());

        job.recountEnded();

        assertThat(progressOf(failing, u)).isZero();
        assertThat(progressOf(other, u)).isEqualTo(1);
    }
}
