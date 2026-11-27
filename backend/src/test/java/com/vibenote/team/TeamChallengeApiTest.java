package com.vibenote.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.TestProjects;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * F-20c 팀 기간 목표 API (docs/specs/F-20c-team-challenge.md AC1~AC9). 오늘은 2026-11-24(Asia/Seoul)로 고정한다.
 * MyBatis 세션 캐시 때문에 데이터는 요청 전에 모두 준비한다.
 */
class TeamChallengeApiTest extends TeamTestSupport {

    private static final String CHALLENGES = "/api/teams/{id}/challenges";
    private static final String CHALLENGE = "/api/teams/{id}/challenges/{challengeId}";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);
    private static final String PASSWORD = "vibe1234";

    @TestBean Clock clock;

    static Clock clock() {
        return Clock.fixed(TODAY.atTime(9, 0).atZone(KST).toInstant(), KST);
    }

    @Autowired private PasswordEncoder passwordEncoder;

    // ── 데이터 준비 ──

    private long insertAnalysis(long userId, String title, boolean deleted) {
        jdbc.update(
                "INSERT INTO analyses (user_id, project_id, source_type, project_name, title,"
                    + " level, status, progress, created_at, deleted_at) VALUES (?, ?, 'ZIP', ?, ?,"
                    + " 'JUNIOR', 'DONE', 100, ?, IF(?, NOW(), NULL))",
                userId,
                TestProjects.zip(jdbc, userId),
                "proj-" + title,
                title,
                BASE,
                deleted);
        long analysisId = lastId();
        jdbc.update(
                "INSERT INTO quizzes (analysis_id, created_at) VALUES (?, ?)", analysisId, BASE);
        return analysisId;
    }

    /** 퀴즈 있는 DONE 분석을 만들고 팀에 공유한다. */
    private long sharedAnalysis(long teamId, long userId, String title) {
        long analysisId = insertAnalysis(userId, title, false);
        share(teamId, analysisId);
        return analysisId;
    }

    private void share(long teamId, long analysisId) {
        jdbc.update(
                "INSERT INTO team_analyses (team_id, analysis_id, shared_at) VALUES (?, ?, ?)",
                teamId,
                analysisId,
                BASE);
    }

    private void softDelete(long analysisId) {
        jdbc.update("UPDATE analyses SET deleted_at = NOW() WHERE id = ?", analysisId);
    }

    private long quizOf(long analysisId) {
        return jdbc.queryForObject(
                "SELECT id FROM quizzes WHERE analysis_id = ?", Long.class, analysisId);
    }

    /** 풀이 한 건 (문항 답은 넣지 않는다. 목표 판정은 score·total만 본다). */
    private long attempt(long analysisId, long userId, LocalDateTime at, int score, int total) {
        jdbc.update(
                "INSERT INTO quiz_attempts (quiz_id, user_id, score, total, created_at) VALUES (?,"
                        + " ?, ?, ?, ?)",
                quizOf(analysisId),
                userId,
                score,
                total,
                at);
        return lastId();
    }

    private long insertChallenge(
            long teamId, long analysisId, long createdBy, String start, String end, int rate) {
        jdbc.update(
                "INSERT INTO team_challenges (team_id, analysis_id, start_date, end_date,"
                        + " target_rate, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                teamId,
                analysisId,
                start,
                end,
                rate,
                createdBy,
                BASE);
        return lastId();
    }

    private int challengeCount(long teamId) {
        return count("SELECT COUNT(*) FROM team_challenges WHERE team_id = ?", teamId);
    }

    private boolean challengeExists(long challengeId) {
        return count("SELECT COUNT(*) FROM team_challenges WHERE id = ?", challengeId) == 1;
    }

    private static Map<String, Object> body(
            Object analysisId, String start, String end, Object rate) {
        Map<String, Object> body = new HashMap<>();
        body.put("analysisId", analysisId);
        body.put("startDate", start);
        body.put("endDate", end);
        body.put("targetRate", rate);
        return body;
    }

    private ResultActions create(long teamId, long userId, Map<String, Object> body)
            throws Exception {
        return perform(post(CHALLENGES, teamId), userId, objectMapper.writeValueAsString(body));
    }

    private void expectError(ResultActions actions, int status, String code) throws Exception {
        actions.andExpect(status().is(status)).andExpect(jsonPath("$.code").value(code));
    }

    /** 팀 T(팀장 u, 멤버 v)와 u가 공유한 분석 a. */
    private record Example(long u, long v, long team, long analysis) {}

    private Example example() {
        long u = insertUser("f20cu");
        long v = insertUser("f20cv");
        long team = insertTeam(u, "팀", "TEAM0001", BASE);
        addMember(team, v, BASE.plusMinutes(1));
        long a = sharedAnalysis(team, u, "spring");
        return new Example(u, v, team, a);
    }

    // ── AC1 ──

    @Test
    void AC1_팀장이_공유_분석에_목표를_만들면_201을_받는다() throws Exception {
        Example e = example();
        long b = sharedAnalysis(e.team(), e.v(), "b");

        create(e.team(), e.u(), body(e.analysis(), "2026-11-25", "2026-12-01", 80))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.challengeId").value(notNullValue()))
                .andExpect(jsonPath("$.analysisId").value(e.analysis()))
                .andExpect(jsonPath("$.title").value("spring"))
                .andExpect(jsonPath("$.projectName").value("proj-spring"))
                .andExpect(jsonPath("$.startDate").value("2026-11-25"))
                .andExpect(jsonPath("$.endDate").value("2026-12-01"))
                .andExpect(jsonPath("$.targetRate").value(80))
                .andExpect(jsonPath("$.status").value("UPCOMING"))
                .andExpect(jsonPath("$.memberCount").value(2))
                .andExpect(jsonPath("$.solvedMemberCount").value(0))
                .andExpect(jsonPath("$.achievedMemberCount").value(0))
                .andExpect(jsonPath("$.achieved").value(false))
                .andExpect(jsonPath("$.members", hasSize(2)))
                .andExpect(jsonPath("$.members[0].userId").value(e.u()))
                .andExpect(jsonPath("$.members[0].role").value("OWNER"))
                .andExpect(jsonPath("$.members[0].attempt").value(nullValue()))
                .andExpect(jsonPath("$.members[0].achieved").value(false))
                .andExpect(jsonPath("$.members[1].userId").value(e.v()))
                .andExpect(jsonPath("$.members[1].attempt").value(nullValue()))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()));

        create(e.team(), e.u(), body(b, "2026-11-24", "2026-11-30", 80))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ONGOING"));

        assertThat(challengeCount(e.team())).isEqualTo(2);
        assertThat(
                        count(
                                "SELECT COUNT(*) FROM team_challenges WHERE team_id = ? AND"
                                        + " analysis_id = ? AND start_date = '2026-11-25' AND"
                                        + " end_date = '2026-12-01' AND target_rate = 80 AND"
                                        + " created_by = ?",
                                e.team(),
                                e.analysis(),
                                e.u()))
                .isEqualTo(1);
    }

    // ── AC2 ──

    @Test
    void AC2_요청_본문이_잘못되면_필드별_사유와_400이고_행이_생기지_않는다() throws Exception {
        Example e = example();
        long a = e.analysis();
        Map<String, Map<String, Object>> cases = new LinkedHashMap<>();
        cases.put("analysisId", body(null, "2026-11-25", "2026-12-01", 80));
        cases.put("analysisId ", body(0, "2026-11-25", "2026-12-01", 80));
        cases.put("startDate", body(a, null, "2026-12-01", 80));
        cases.put("startDate ", body(a, "2026/11/25", "2026-12-01", 80));
        cases.put("startDate  ", body(a, "2026-11-23", "2026-12-01", 80));
        cases.put("startDate   ", body(a, "2027-11-25", "2027-11-30", 80)); // 오늘 + 366일 (#95)
        cases.put("startDate    ", body(a, "+10000-01-01", "+10000-01-05", 80)); // DATE 범위 밖
        cases.put("endDate", body(a, "2026-11-25", null, 80));
        cases.put("endDate ", body(a, "2026-11-25", "12-01", 80));
        cases.put("endDate  ", body(a, "2026-11-25", "2026-11-24", 80));
        cases.put("endDate   ", body(a, "2026-11-25", "2026-12-25", 80)); // 31일
        cases.put("targetRate", body(a, "2026-11-25", "2026-12-01", null));
        cases.put("targetRate ", body(a, "2026-11-25", "2026-12-01", 0));
        cases.put("targetRate  ", body(a, "2026-11-25", "2026-12-01", 101));

        for (Map.Entry<String, Map<String, Object>> c : cases.entrySet()) {
            String field = c.getKey().trim();
            create(e.team(), e.u(), c.getValue())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_400"))
                    .andExpect(jsonPath("$.details." + field).value(notNullValue()));
        }
        assertThat(challengeCount(e.team())).isZero();
    }

    @Test
    void AC2_경계값_목표_1과_100_시작일_오늘_기간_정확히_30일은_통과한다() throws Exception {
        Example e = example();
        long b = sharedAnalysis(e.team(), e.u(), "b");
        long c = sharedAnalysis(e.team(), e.u(), "c");

        create(e.team(), e.u(), body(e.analysis(), "2026-11-24", "2026-12-23", 1))
                .andExpect(status().isCreated());
        create(e.team(), e.u(), body(b, "2026-11-24", "2026-11-24", 100))
                .andExpect(status().isCreated());
        // 시작일 상한 = 오늘 + 365일 (#95)
        create(e.team(), e.u(), body(c, "2027-11-24", "2027-12-23", 80))
                .andExpect(status().isCreated());

        assertThat(challengeCount(e.team())).isEqualTo(3);
    }

    // ── AC3 ──

    @Test
    void AC3_공유되지_않았거나_삭제된_분석은_TEAM_006이다() throws Exception {
        Example e = example();
        long notShared = insertAnalysis(e.u(), "notshared", false);
        long deleted = sharedAnalysis(e.team(), e.u(), "deleted");
        softDelete(deleted);

        expectError(
                create(e.team(), e.u(), body(notShared, "2026-11-25", "2026-12-01", 80)),
                404,
                "TEAM_006");
        expectError(
                create(e.team(), e.u(), body(deleted, "2026-11-25", "2026-12-01", 80)),
                404,
                "TEAM_006");
        expectError(
                create(e.team(), e.u(), body(999_999_999L, "2026-11-25", "2026-12-01", 80)),
                404,
                "TEAM_006");
        assertThat(challengeCount(e.team())).isZero();
    }

    @Test
    void AC3_끝나지_않은_목표가_있으면_TEAM_011이고_모두_종료됐으면_만들_수_있다() throws Exception {
        Example e = example();
        long ended = sharedAnalysis(e.team(), e.u(), "ended");
        insertChallenge(e.team(), e.analysis(), e.u(), "2026-11-20", "2026-11-24", 80); // 오늘 마감
        insertChallenge(e.team(), ended, e.u(), "2026-11-17", "2026-11-23", 80); // 종료

        create(e.team(), e.u(), body(e.analysis(), "2026-11-25", "2026-12-01", 80))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TEAM_011"))
                .andExpect(jsonPath("$.message").value("이 분석에는 이미 예정되거나 진행 중인 팀 목표가 있습니다."));
        create(e.team(), e.u(), body(ended, "2026-11-25", "2026-12-01", 80))
                .andExpect(status().isCreated());

        assertThat(challengeCount(e.team())).isEqualTo(3);
    }

    @Test
    void AC3_목표가_20개면_TEAM_012이고_삭제된_분석의_목표는_세지_않는다() throws Exception {
        Example e = example();
        long old = sharedAnalysis(e.team(), e.u(), "old");
        long gone = sharedAnalysis(e.team(), e.u(), "gone");
        long c = sharedAnalysis(e.team(), e.u(), "c");
        for (int i = 0; i < 19; i++) {
            insertChallenge(e.team(), old, e.u(), "2026-11-01", "2026-11-07", 80);
        }
        insertChallenge(e.team(), gone, e.u(), "2026-11-25", "2026-11-30", 80);
        softDelete(gone);

        create(e.team(), e.u(), body(c, "2026-11-25", "2026-12-01", 80))
                .andExpect(status().isCreated());
        create(e.team(), e.u(), body(e.analysis(), "2026-11-25", "2026-12-01", 80))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TEAM_012"))
                .andExpect(jsonPath("$.message").value("팀 목표는 최대 20개입니다. 종료된 목표를 지워 주세요."));

        assertThat(challengeCount(e.team())).isEqualTo(21);
    }

    @Test
    void AC3_팀원은_본문이_잘못돼도_403이고_멤버가_아니면_404다() throws Exception {
        Example e = example();
        long outsider = insertUser("f20cout");
        Map<String, Object> valid = body(e.analysis(), "2026-11-25", "2026-12-01", 80);

        expectError(create(e.team(), e.v(), valid), 403, "TEAM_403");
        expectError(create(e.team(), e.v(), body(null, null, null, 0)), 403, "TEAM_403");
        expectError(create(e.team(), outsider, valid), 404, "TEAM_404");
        expectError(create(999_999_999L, e.u(), valid), 404, "TEAM_404");
        assertThat(challengeCount(e.team())).isZero();
    }

    // ── AC4 ──

    @Test
    void AC4_목록은_진행_중_예정_종료_순이고_삭제된_분석의_목표를_뺀다() throws Exception {
        Example e = example();
        long a1 = e.analysis();
        long a2 = sharedAnalysis(e.team(), e.u(), "a2");
        long a3 = sharedAnalysis(e.team(), e.u(), "a3");
        long a4 = sharedAnalysis(e.team(), e.u(), "a4");
        long a5 = sharedAnalysis(e.team(), e.u(), "a5");
        long g1 = insertChallenge(e.team(), a1, e.u(), "2026-11-20", "2026-11-23", 80);
        long g2 = insertChallenge(e.team(), a2, e.u(), "2026-11-24", "2026-11-30", 80);
        long g3 = insertChallenge(e.team(), a1, e.u(), "2026-11-26", "2026-11-28", 80);
        long g4 = insertChallenge(e.team(), a3, e.u(), "2026-11-22", "2026-11-26", 80);
        insertChallenge(e.team(), a4, e.u(), "2026-11-22", "2026-11-26", 80);
        long g6 = insertChallenge(e.team(), a5, e.u(), "2026-11-23", "2026-11-26", 80);
        softDelete(a4);
        long emptyTeam = insertTeam(e.u(), "빈 팀", "TEAM0002", BASE);

        perform(get(CHALLENGES, e.team()), e.v())
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[*].challengeId")
                                .value(contains((int) g6, (int) g4, (int) g2, (int) g3, (int) g1)));
        perform(get(CHALLENGES, e.team()).param("analysisId", Long.toString(a1)), e.v())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].challengeId").value(contains((int) g3, (int) g1)));
        perform(get(CHALLENGES, emptyTeam), e.u())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void AC4_analysisId가_숫자가_아니거나_1_미만이면_400이고_멤버가_아니면_404다() throws Exception {
        Example e = example();
        long outsider = insertUser("f20cout");

        expectError(
                perform(get(CHALLENGES, e.team()).param("analysisId", "abc"), e.u()),
                400,
                "COMMON_400");
        expectError(
                perform(get(CHALLENGES, e.team()).param("analysisId", "0"), e.u()),
                400,
                "COMMON_400");
        expectError(perform(get(CHALLENGES, e.team()), outsider), 404, "TEAM_404");
    }

    // ── AC5 ──

    /** 진행 중 목표 G(11-20~11-24, 80%)와 지금 멤버 u(팀장)·v·w, 떠난 x. */
    private record Progress(long u, long v, long w, long x, long team, long analysis, long g) {}

    private Progress progress() {
        long u = insertUser("f20cu");
        long v = insertUser("f20cv");
        long w = insertUser("f20cw");
        long x = insertUser("f20cx");
        long team = insertTeam(u, "팀", "TEAM0001", BASE);
        addMember(team, v, BASE.plusMinutes(1));
        addMember(team, w, BASE.plusMinutes(2));
        long a = sharedAnalysis(team, u, "spring");
        long g = insertChallenge(team, a, u, "2026-11-20", "2026-11-24", 80);
        attempt(a, u, LocalDateTime.of(2026, 11, 19, 23, 59, 59), 5, 5); // 기간 전
        attempt(a, u, LocalDateTime.of(2026, 11, 21, 12, 0), 3, 5);
        attempt(a, v, LocalDateTime.of(2026, 11, 20, 0, 0, 0), 4, 5); // 시작일 00:00
        attempt(a, x, LocalDateTime.of(2026, 11, 22, 12, 0), 5, 5); // 멤버 아님
        return new Progress(u, v, w, x, team, a, g);
    }

    @Test
    void AC5_멤버별_기간_안_최근_풀이로_달성을_판정하고_떠난_회원은_빠진다() throws Exception {
        Progress p = progress();

        perform(get(CHALLENGES, p.team()), p.w())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("ONGOING"))
                .andExpect(jsonPath("$[0].memberCount").value(3))
                .andExpect(jsonPath("$[0].solvedMemberCount").value(2))
                .andExpect(jsonPath("$[0].achievedMemberCount").value(1))
                .andExpect(jsonPath("$[0].achieved").value(false))
                .andExpect(jsonPath("$[0].members", hasSize(3)))
                .andExpect(jsonPath("$[0].members[0].userId").value(p.u()))
                .andExpect(jsonPath("$[0].members[0].attempt.score").value(3))
                .andExpect(jsonPath("$[0].members[0].attempt.total").value(5))
                .andExpect(jsonPath("$[0].members[0].attempt.attemptId").value(notNullValue()))
                .andExpect(jsonPath("$[0].members[0].attempt.createdAt").value(notNullValue()))
                .andExpect(jsonPath("$[0].members[0].achieved").value(false))
                .andExpect(jsonPath("$[0].members[1].userId").value(p.v()))
                .andExpect(jsonPath("$[0].members[1].achieved").value(true))
                .andExpect(jsonPath("$[0].members[2].userId").value(p.w()))
                .andExpect(jsonPath("$[0].members[2].attempt").value(nullValue()))
                .andExpect(jsonPath("$[0].members[2].achieved").value(false));
    }

    @Test
    void AC5_종료일_23시59분59초_풀이는_세고_다음_날_00시_풀이는_세지_않는다() throws Exception {
        Progress p = progress();
        attempt(p.analysis(), p.u(), LocalDateTime.of(2026, 11, 24, 23, 59, 59), 5, 5);
        attempt(p.analysis(), p.w(), LocalDateTime.of(2026, 11, 25, 0, 0, 0), 5, 5);

        perform(get(CHALLENGES, p.team()), p.u())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].solvedMemberCount").value(2))
                .andExpect(jsonPath("$[0].achievedMemberCount").value(2))
                .andExpect(jsonPath("$[0].achieved").value(false))
                .andExpect(jsonPath("$[0].members[0].attempt.score").value(5))
                .andExpect(jsonPath("$[0].members[0].achieved").value(true))
                .andExpect(jsonPath("$[0].members[2].attempt").value(nullValue()));
    }

    @Test
    void AC5_주인이_아닌_멤버까지_지금_멤버_전원이_달성하면_팀_달성이다() throws Exception {
        Progress p = progress();
        attempt(p.analysis(), p.u(), LocalDateTime.of(2026, 11, 24, 8, 0), 4, 5);
        attempt(p.analysis(), p.w(), LocalDateTime.of(2026, 11, 23, 8, 0), 4, 5);

        perform(get(CHALLENGES, p.team()), p.u())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].solvedMemberCount").value(3))
                .andExpect(jsonPath("$[0].achievedMemberCount").value(3))
                .andExpect(jsonPath("$[0].achieved").value(true));
    }

    // ── AC6 ──

    @Test
    void AC6_상태는_예정_진행_중_종료이고_종료_목표도_기간_안_풀이로_판정한다() throws Exception {
        long u = insertUser("f20cu");
        long team = insertTeam(u, "팀", "TEAM0001", BASE);
        long a1 = sharedAnalysis(team, u, "a1");
        long a2 = sharedAnalysis(team, u, "a2");
        long a3 = sharedAnalysis(team, u, "a3");
        long upcoming = insertChallenge(team, a1, u, "2026-11-25", "2026-11-30", 80);
        long ongoing = insertChallenge(team, a2, u, "2026-11-24", "2026-11-24", 80);
        long ended = insertChallenge(team, a3, u, "2026-11-17", "2026-11-23", 80);
        attempt(a1, u, LocalDateTime.of(2026, 11, 22, 12, 0), 5, 5); // 예정 목표 기간 밖
        attempt(a2, u, LocalDateTime.of(2026, 11, 23, 12, 0), 5, 5); // 진행 중 기간 밖
        attempt(a3, u, LocalDateTime.of(2026, 11, 20, 12, 0), 5, 5);

        perform(get(CHALLENGES, team), u)
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[*].challengeId")
                                .value(contains((int) ongoing, (int) upcoming, (int) ended)))
                .andExpect(jsonPath("$[0].status").value("ONGOING"))
                .andExpect(jsonPath("$[0].achieved").value(false))
                .andExpect(jsonPath("$[1].status").value("UPCOMING"))
                .andExpect(jsonPath("$[1].members[0].attempt").value(nullValue()))
                .andExpect(jsonPath("$[1].achieved").value(false))
                .andExpect(jsonPath("$[2].status").value("ENDED"))
                .andExpect(jsonPath("$[2].achievedMemberCount").value(1))
                .andExpect(jsonPath("$[2].achieved").value(true));
    }

    // ── AC7 ──

    @Test
    void AC7_팀장은_상태와_상관없이_목표를_지우고_풀이는_남는다() throws Exception {
        Example e = example();
        long b = sharedAnalysis(e.team(), e.u(), "b");
        long ended = insertChallenge(e.team(), e.analysis(), e.u(), "2026-11-17", "2026-11-23", 80);
        long ongoing = insertChallenge(e.team(), b, e.u(), "2026-11-24", "2026-11-30", 80);
        attempt(e.analysis(), e.v(), LocalDateTime.of(2026, 11, 20, 12, 0), 5, 5);

        perform(delete(CHALLENGE, e.team(), ended), e.u()).andExpect(status().isNoContent());
        perform(delete(CHALLENGE, e.team(), ongoing), e.u()).andExpect(status().isNoContent());

        assertThat(challengeCount(e.team())).isZero();
        assertThat(count("SELECT COUNT(*) FROM quiz_attempts WHERE user_id = ?", e.v()))
                .isEqualTo(1);
    }

    @Test
    void AC7_팀원은_403이고_없거나_다른_팀이거나_삭제된_분석의_목표는_TEAM_013이다() throws Exception {
        Example e = example();
        long outsider = insertUser("f20cout");
        long team2 = insertTeam(e.u(), "팀2", "TEAM0002", BASE);
        long other = sharedAnalysis(team2, e.u(), "other");
        long gone = sharedAnalysis(e.team(), e.u(), "gone");
        long g = insertChallenge(e.team(), e.analysis(), e.u(), "2026-11-24", "2026-11-30", 80);
        long otherTeam = insertChallenge(team2, other, e.u(), "2026-11-24", "2026-11-30", 80);
        long deleted = insertChallenge(e.team(), gone, e.u(), "2026-11-24", "2026-11-30", 80);
        softDelete(gone);

        expectError(perform(delete(CHALLENGE, e.team(), g), e.v()), 403, "TEAM_403");
        expectError(perform(delete(CHALLENGE, e.team(), 999_999_999L), e.v()), 403, "TEAM_403");
        expectError(perform(delete(CHALLENGE, e.team(), g), outsider), 404, "TEAM_404");
        perform(delete(CHALLENGE, e.team(), 999_999_999L), e.u())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEAM_013"))
                .andExpect(jsonPath("$.message").value("팀 목표를 찾을 수 없습니다."));
        expectError(perform(delete(CHALLENGE, e.team(), otherTeam), e.u()), 404, "TEAM_013");
        expectError(perform(delete(CHALLENGE, e.team(), deleted), e.u()), 404, "TEAM_013");

        assertThat(challengeExists(g)).isTrue();
        assertThat(challengeExists(otherTeam)).isTrue();
        assertThat(challengeExists(deleted)).isTrue();
    }

    // ── AC8 ──

    /** v가 T·T2에 공유한 a(목표 g1·g3), 팀장 u가 T에 공유한 b(목표 g2). */
    private record Cascade(
            long u, long v, long team, long team2, long a, long g1, long g2, long g3) {}

    private Cascade cascade() {
        long u = insertUser("f20cu");
        long v = insertUser("f20cv");
        long x = insertUser("f20cx");
        jdbc.update(
                "UPDATE users SET password_hash = ? WHERE id = ?",
                passwordEncoder.encode(PASSWORD),
                v);
        long team = insertTeam(u, "팀", "TEAM0001", BASE);
        long team2 = insertTeam(x, "팀2", "TEAM0002", BASE);
        addMember(team, v, BASE);
        addMember(team2, v, BASE);
        long a = insertAnalysis(v, "a", false);
        share(team, a);
        share(team2, a);
        long b = sharedAnalysis(team, u, "b");
        long g1 = insertChallenge(team, a, u, "2026-11-24", "2026-11-30", 80);
        long g2 = insertChallenge(team, b, u, "2026-11-24", "2026-11-30", 80);
        long g3 = insertChallenge(team2, a, x, "2026-11-24", "2026-11-30", 80);
        return new Cascade(u, v, team, team2, a, g1, g2, g3);
    }

    @Test
    void AC8_공유를_해제하면_그_팀의_그_분석_목표만_지워진다() throws Exception {
        Cascade c = cascade();

        perform(delete("/api/teams/{id}/analyses/{analysisId}", c.team(), c.a()), c.v())
                .andExpect(status().isNoContent());

        assertThat(challengeExists(c.g1())).isFalse();
        assertThat(challengeExists(c.g2())).isTrue();
        assertThat(challengeExists(c.g3())).isTrue();
    }

    @Test
    void AC8_공유자가_팀을_떠나면_그_팀의_목표만_지워진다() throws Exception {
        Cascade c = cascade();

        perform(delete("/api/teams/{id}/members/me", c.team()), c.v())
                .andExpect(status().isNoContent());

        assertThat(challengeExists(c.g1())).isFalse();
        assertThat(challengeExists(c.g2())).isTrue();
        assertThat(challengeExists(c.g3())).isTrue();
    }

    @Test
    void AC8_팀장이_공유자를_내보내면_그_팀의_목표만_지워진다() throws Exception {
        Cascade c = cascade();

        perform(delete("/api/teams/{id}/members/{userId}", c.team(), c.v()), c.u())
                .andExpect(status().isNoContent());

        assertThat(challengeExists(c.g1())).isFalse();
        assertThat(challengeExists(c.g2())).isTrue();
        assertThat(challengeExists(c.g3())).isTrue();
    }

    @Test
    void AC8_팀을_삭제하면_그_팀의_목표가_모두_지워진다() throws Exception {
        Cascade c = cascade();

        perform(delete("/api/teams/{id}", c.team()), c.u()).andExpect(status().isNoContent());

        assertThat(challengeCount(c.team())).isZero();
        assertThat(challengeExists(c.g3())).isTrue();
    }

    @Test
    void AC8_공유자가_회원_탈퇴하면_공유한_분석의_목표가_모두_지워진다() throws Exception {
        Cascade c = cascade();

        perform(
                        delete("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"password\":\"" + PASSWORD + "\"}"),
                        c.v())
                .andExpect(status().isNoContent());

        assertThat(challengeExists(c.g1())).isFalse();
        assertThat(challengeExists(c.g3())).isFalse();
        assertThat(challengeExists(c.g2())).isTrue();
    }

    @Test
    void AC8_분석이_soft_delete되면_행은_남고_목록에서_빠진다() throws Exception {
        Cascade c = cascade();
        softDelete(c.a());

        perform(get(CHALLENGES, c.team()), c.u())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].challengeId").value(contains((int) c.g2())));

        assertThat(challengeExists(c.g1())).isTrue();
        assertThat(challengeExists(c.g3())).isTrue();
    }

    // ── AC9 ──

    @Test
    void AC9_토큰이_없거나_만료_깨졌거나_탈퇴한_회원이면_새_API가_401이다() throws Exception {
        long u = insertUser("f20cu");
        long gone = insertUser("f20cgone");
        long team = insertTeam(gone, "팀", "TEAM0001", BASE);
        addMember(team, u, BASE.plusMinutes(1));
        long a = sharedAnalysis(team, gone, "a");
        long b = sharedAnalysis(team, gone, "b");
        long g = insertChallenge(team, a, gone, "2026-11-24", "2026-11-30", 80);
        withdraw(gone);
        String json = objectMapper.writeValueAsString(body(b, "2026-11-25", "2026-12-01", 80));

        List<Supplier<MockHttpServletRequestBuilder>> requests =
                List.of(
                        () -> post(CHALLENGES, team).contentType("application/json").content(json),
                        () -> get(CHALLENGES, team),
                        () -> delete(CHALLENGE, team, g));
        String[] authorizations = {null, expiredBearer(u), "Bearer not-a-jwt", bearer(gone)};

        for (Supplier<MockHttpServletRequestBuilder> request : requests) {
            for (String authorization : authorizations) {
                perform(request.get(), authorization, null)
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value("AUTH_401"));
            }
        }

        assertThat(challengeExists(g)).isTrue();
        assertThat(challengeCount(team)).isEqualTo(1);
    }
}
