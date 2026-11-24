package com.vibenote.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** F-20b 팀 공유 분석·이해도 API (docs/specs/F-20b-team-understanding.md AC1~AC4, AC6~AC9). */
class TeamAnalysisApiTest extends TeamTestSupport {

    private static final String ANALYSES = "/api/teams/{id}/analyses";
    private static final String ANALYSIS = "/api/teams/{id}/analyses/{analysisId}";
    private static final String UNDERSTANDING =
            "/api/teams/{id}/analyses/{analysisId}/understanding";
    private static final String PASSWORD = "vibe1234";

    @Autowired private PasswordEncoder passwordEncoder;

    // ── 데이터 준비 ──

    private long insertAnalysis(long userId, String status, boolean deleted, String title) {
        jdbc.update(
                "INSERT INTO analyses (user_id, source_type, project_name, title, level, status,"
                        + " progress, created_at, deleted_at) VALUES (?, 'ZIP', ?, ?, 'JUNIOR', ?,"
                        + " 100, ?, IF(?, NOW(), NULL))",
                userId,
                "proj-" + title,
                title,
                status,
                BASE,
                deleted);
        return lastId();
    }

    private long doneAnalysis(long userId, String title) {
        return insertAnalysis(userId, "DONE", false, title);
    }

    /** DONE 분석과 그 퀴즈(1문항)를 만들고 분석 id를 돌려준다. */
    private long analysisWithQuiz(long userId, String title) {
        long analysisId = doneAnalysis(userId, title);
        insertQuestion(insertQuiz(analysisId), null);
        return analysisId;
    }

    private long insertQuiz(long analysisId) {
        jdbc.update(
                "INSERT INTO quizzes (analysis_id, created_at) VALUES (?, ?)", analysisId, BASE);
        return lastId();
    }

    private long quizOf(long analysisId) {
        return jdbc.queryForObject(
                "SELECT id FROM quizzes WHERE analysis_id = ?", Long.class, analysisId);
    }

    /** 정답은 항상 0번. */
    private long insertQuestion(long quizId, Long conceptId) {
        jdbc.update(
                "INSERT INTO quiz_questions (quiz_id, concept_id, question, choices, answer_index,"
                        + " explanation) VALUES (?, ?, '질문', '[\"a\", \"b\", \"c\", \"d\"]', 0,"
                        + " '해설')",
                quizId,
                conceptId);
        return lastId();
    }

    private long insertFile(long analysisId, String layer) {
        jdbc.update(
                "INSERT INTO analysis_files (analysis_id, path, layer, content) VALUES (?, ?, ?,"
                        + " 'code')",
                analysisId,
                "src/" + layer + ".java",
                layer);
        return lastId();
    }

    private long insertConcept(String name) {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, status, source)"
                        + " VALUES (?, 'security', 'JUNIOR', '요약', 'PUBLISHED', 'SEED')",
                "f20b-test-" + name);
        return lastId();
    }

    private void linkConcept(long analysisId, long conceptId, Long fileId) {
        jdbc.update(
                "INSERT INTO analysis_concepts (analysis_id, concept_id, file_id) VALUES (?, ?, ?)",
                analysisId,
                conceptId,
                fileId);
    }

    /** 문항별 정오로 풀이 한 건을 넣는다. score·total은 정오에서 계산한다. */
    private long insertAttempt(
            long quizId, long userId, LocalDateTime at, Map<Long, Boolean> correctByQuestion) {
        int score = (int) correctByQuestion.values().stream().filter(c -> c).count();
        jdbc.update(
                "INSERT INTO quiz_attempts (quiz_id, user_id, score, total, created_at) VALUES (?,"
                        + " ?, ?, ?, ?)",
                quizId,
                userId,
                score,
                correctByQuestion.size(),
                at);
        long attemptId = lastId();
        correctByQuestion.forEach(
                (questionId, correct) ->
                        jdbc.update(
                                "INSERT INTO quiz_answers (attempt_id, question_id,"
                                        + " selected_index, is_correct) VALUES (?, ?, ?, ?)",
                                attemptId,
                                questionId,
                                correct ? 0 : 1,
                                correct));
        return attemptId;
    }

    private void share(long teamId, long analysisId, LocalDateTime at) {
        jdbc.update(
                "INSERT INTO team_analyses (team_id, analysis_id, shared_at) VALUES (?, ?, ?)",
                teamId,
                analysisId,
                at);
    }

    private boolean isShared(long teamId, long analysisId) {
        return count(
                        "SELECT COUNT(*) FROM team_analyses WHERE team_id = ? AND analysis_id = ?",
                        teamId,
                        analysisId)
                == 1;
    }

    private int attemptCount(long userId) {
        return count("SELECT COUNT(*) FROM quiz_attempts WHERE user_id = ?", userId);
    }

    private String shareJson(Object analysisId) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("analysisId", analysisId);
        return objectMapper.writeValueAsString(body);
    }

    private org.springframework.test.web.servlet.ResultActions shareRequest(
            long teamId, long userId, Object analysisId) throws Exception {
        return perform(post(ANALYSES, teamId), userId, shareJson(analysisId));
    }

    private void expectError(
            org.springframework.test.web.servlet.ResultActions actions, int status, String code)
            throws Exception {
        actions.andExpect(status().is(status)).andExpect(jsonPath("$.code").value(code));
    }

    // ── AC1 ──

    @Test
    void AC1_팀_멤버가_내_완료_분석을_공유하면_201을_받고_다른_팀에도_공유할_수_있다() throws Exception {
        long owner = insertUser("f20bowner");
        long u = insertUser("f20bu");
        long team = insertTeam(owner, "팀", "TEAM0001", BASE);
        long team2 = insertTeam(u, "팀2", "TEAM0002", BASE);
        addMember(team, u, BASE);
        long a = analysisWithQuiz(u, "spring");

        shareRequest(team, u, a)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.analysisId").value(a))
                .andExpect(jsonPath("$.sharedAt").value(notNullValue()));
        shareRequest(team2, u, a).andExpect(status().isCreated());

        assertThat(isShared(team, a)).isTrue();
        assertThat(isShared(team2, a)).isTrue();
        assertThat(count("SELECT COUNT(*) FROM team_analyses WHERE analysis_id = ?", a))
                .isEqualTo(2);
    }

    // ── AC2 ──

    @Test
    void AC2_공유_요청이_잘못되면_정해진_순서로_에러를_주고_행이_생기지_않는다() throws Exception {
        long u = insertUser("f20bu");
        long other = insertUser("f20bother");
        long outsider = insertUser("f20bout");
        long team = insertTeam(u, "팀", "TEAM0001", BASE);
        long othersAnalysis = analysisWithQuiz(other, "others");
        long deleted = insertAnalysis(u, "DONE", true, "deleted");
        insertQuiz(deleted);
        long running = insertAnalysis(u, "EXPLAINING", false, "running");
        insertQuiz(running);
        long noQuiz = doneAnalysis(u, "noquiz");
        long mine = analysisWithQuiz(u, "mine");

        // 400 검증
        expectError(perform(post(ANALYSES, team), u, "{}"), 400, "COMMON_400");
        perform(post(ANALYSES, team), u, "{}").andExpect(jsonPath("$.details.analysisId").exists());
        expectError(shareRequest(team, u, 0), 400, "COMMON_400");
        // 404 ANALYSIS_404: 없는 분석·남의 분석·soft delete
        expectError(shareRequest(team, u, 999_999_999L), 404, "ANALYSIS_404");
        expectError(shareRequest(team, u, othersAnalysis), 404, "ANALYSIS_404");
        expectError(shareRequest(team, u, deleted), 404, "ANALYSIS_404");
        // 409 TEAM_009: DONE 아님·퀴즈 없음
        expectError(shareRequest(team, u, running), 409, "TEAM_009");
        expectError(shareRequest(team, u, noQuiz), 409, "TEAM_009");
        // 멤버가 아니면 무엇을 보내든 TEAM_404 (없는 팀 포함)
        expectError(perform(post(ANALYSES, team), outsider, "{}"), 404, "TEAM_404");
        expectError(shareRequest(team, outsider, mine), 404, "TEAM_404");
        expectError(shareRequest(999_999_999L, u, mine), 404, "TEAM_404");

        assertThat(count("SELECT COUNT(*) FROM team_analyses WHERE team_id = ?", team)).isZero();

        // 409 TEAM_007: 이미 공유
        shareRequest(team, u, mine).andExpect(status().isCreated());
        expectError(shareRequest(team, u, mine), 409, "TEAM_007");
        assertThat(count("SELECT COUNT(*) FROM team_analyses WHERE team_id = ?", team))
                .isEqualTo(1);
    }

    @Test
    void AC2_공유가_10개인_팀에는_TEAM_008이고_soft_delete된_분석은_세지_않는다() throws Exception {
        long u = insertUser("f20bu");
        long full = insertTeam(u, "가득", "TEAM0001", BASE);
        long withDeleted = insertTeam(u, "삭제포함", "TEAM0002", BASE);
        List<Long> shared = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            long a = analysisWithQuiz(u, "a" + i);
            share(full, a, BASE.plusMinutes(i));
            share(withDeleted, a, BASE.plusMinutes(i));
            shared.add(a);
        }
        long extra = analysisWithQuiz(u, "extra");
        long extra2 = analysisWithQuiz(u, "extra2");
        // 같은 트랜잭션에서 jdbc로 바꾼 값은 MyBatis 세션 캐시에 안 보일 수 있어 요청 전에 바꾼다
        jdbc.update("UPDATE analyses SET deleted_at = NOW() WHERE id = ?", shared.get(0));

        // full 팀은 삭제된 shared[0]을 빼고 a10을 더해 미삭제 10개 → TEAM_008
        share(full, analysisWithQuiz(u, "a10"), BASE.plusMinutes(10));
        expectError(shareRequest(full, u, extra), 409, "TEAM_008");
        assertThat(isShared(full, extra)).isFalse();

        // 10행 중 1개가 삭제된 분석이면 9개로 세어 공유된다
        shareRequest(withDeleted, u, extra).andExpect(status().isCreated());
        expectError(shareRequest(withDeleted, u, extra2), 409, "TEAM_008");
        // 이미 공유한 분석은 상한보다 TEAM_007이 먼저다
        expectError(shareRequest(withDeleted, u, extra), 409, "TEAM_007");
    }

    // ── AC3 ──

    @Test
    void AC3_공유_목록은_최신_공유순이고_삭제된_분석을_빼며_풀이_요약을_준다() throws Exception {
        long u = insertUser("f20bu");
        long v = insertUser("f20bv");
        long w = insertUser("f20bw");
        long gone = insertUser("f20bgone");
        long team = insertTeam(u, "팀", "TEAM0001", BASE);
        addMember(team, v, BASE.plusMinutes(1));
        addMember(team, w, BASE.plusMinutes(2));
        long a1 = analysisWithQuiz(u, "a1");
        long a2 = analysisWithQuiz(v, "a2");
        long a3 = analysisWithQuiz(w, "a3");
        share(team, a1, LocalDateTime.of(2026, 11, 24, 12, 0));
        share(team, a2, LocalDateTime.of(2026, 11, 25, 12, 0));
        share(team, a3, LocalDateTime.of(2026, 11, 26, 12, 0));
        jdbc.update("UPDATE analyses SET deleted_at = NOW() WHERE id = ?", a3);
        long q1 = quizOf(a1);
        long question =
                jdbc.queryForObject(
                        "SELECT id FROM quiz_questions WHERE quiz_id = ?", Long.class, q1);
        insertAttempt(q1, u, BASE.plusHours(1), Map.of(question, true));
        insertAttempt(q1, v, BASE.plusHours(1), Map.of(question, false));
        insertAttempt(q1, v, BASE.plusHours(2), Map.of(question, true));
        insertAttempt(q1, gone, BASE.plusHours(2), Map.of(question, true)); // 멤버 아님

        perform(get(ANALYSES, team), u)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].analysisId").value(a2))
                .andExpect(jsonPath("$[0].sharedBy.userId").value(v))
                .andExpect(jsonPath("$[0].sharedBy.nickname").value("f20bv"))
                .andExpect(jsonPath("$[0].myLastAttempt").value(nullValue()))
                .andExpect(jsonPath("$[0].solvedMemberCount").value(0))
                .andExpect(jsonPath("$[0].canUnshare").value(true)) // 팀장
                .andExpect(jsonPath("$[1].analysisId").value(a1))
                .andExpect(jsonPath("$[1].title").value("a1"))
                .andExpect(jsonPath("$[1].projectName").value("proj-a1"))
                .andExpect(jsonPath("$[1].level").value("JUNIOR"))
                .andExpect(jsonPath("$[1].sharedAt").value("2026-11-24T12:00:00+09:00"))
                .andExpect(jsonPath("$[1].questionCount").value(1))
                .andExpect(jsonPath("$[1].solvedMemberCount").value(2))
                .andExpect(jsonPath("$[1].myLastAttempt.score").value(1))
                .andExpect(jsonPath("$[1].myLastAttempt.total").value(1));

        perform(get(ANALYSES, team), w)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].canUnshare").value(false))
                .andExpect(jsonPath("$[1].canUnshare").value(false));
        perform(get(ANALYSES, team), v)
                .andExpect(jsonPath("$[0].canUnshare").value(true)) // 공유한 사람
                .andExpect(jsonPath("$[1].canUnshare").value(false))
                .andExpect(
                        jsonPath("$[1].myLastAttempt.createdAt")
                                .value("2026-11-24T12:00:00+09:00"));
        expectError(perform(get(ANALYSES, team), gone), 404, "TEAM_404");
    }

    @Test
    void AC3_공유가_없으면_빈_배열이다() throws Exception {
        long u = insertUser("f20bu");
        long team = insertTeam(u, "팀", "TEAM0001", BASE);

        perform(get(ANALYSES, team), u)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ── AC4 ──

    @Test
    void AC4_공유한_사람이나_팀장만_해제하고_풀이_기록은_남는다() throws Exception {
        long u = insertUser("f20bu");
        long v = insertUser("f20bv");
        long w = insertUser("f20bw");
        long outsider = insertUser("f20bout");
        long team = insertTeam(u, "팀", "TEAM0001", BASE);
        addMember(team, v, BASE);
        addMember(team, w, BASE);
        long a = analysisWithQuiz(v, "a");
        long b = analysisWithQuiz(v, "b");
        share(team, a, BASE);
        share(team, b, BASE);
        long question =
                jdbc.queryForObject(
                        "SELECT id FROM quiz_questions WHERE quiz_id = ?", Long.class, quizOf(a));
        insertAttempt(quizOf(a), w, BASE.plusHours(1), Map.of(question, true));
        long notShared = analysisWithQuiz(v, "c");

        expectError(perform(delete(ANALYSIS, team, a), w), 403, "TEAM_010");
        expectError(perform(delete(ANALYSIS, team, notShared), v), 404, "TEAM_006");
        expectError(perform(delete(ANALYSIS, team, a), outsider), 404, "TEAM_404");
        assertThat(isShared(team, a)).isTrue();

        perform(delete(ANALYSIS, team, a), v).andExpect(status().isNoContent());
        perform(delete(ANALYSIS, team, b), u).andExpect(status().isNoContent());

        assertThat(isShared(team, a)).isFalse();
        assertThat(isShared(team, b)).isFalse();
        assertThat(attemptCount(w)).isEqualTo(1);
        expectError(perform(delete(ANALYSIS, team, a), v), 404, "TEAM_006");
    }

    // ── AC6 ──

    /** AC6 예시: 4문항(q1·q2 → controller, q3 → service, q4 개념 없음), U(팀장)·V·W. */
    private record Example(long team, long analysis, long u, long v, long w, List<Long> q) {}

    private Example example() {
        long u = insertUser("f20bu");
        long v = insertUser("f20bv");
        long w = insertUser("f20bw");
        long team = insertTeam(u, "팀", "TEAM0001", BASE);
        addMember(team, v, BASE.plusMinutes(1));
        addMember(team, w, BASE.plusMinutes(2));
        long a = doneAnalysis(u, "spring");
        long controller = insertFile(a, "controller");
        long service = insertFile(a, "service");
        long c1 = insertConcept("c1");
        long c2 = insertConcept("c2");
        linkConcept(a, c1, controller);
        linkConcept(a, c2, service);
        long quiz = insertQuiz(a);
        List<Long> q =
                List.of(
                        insertQuestion(quiz, c1),
                        insertQuestion(quiz, c1),
                        insertQuestion(quiz, c2),
                        insertQuestion(quiz, null));
        share(team, a, BASE);
        return new Example(team, a, u, v, w, q);
    }

    private Map<Long, Boolean> answers(List<Long> q, boolean... correct) {
        Map<Long, Boolean> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < q.size(); i++) map.put(q.get(i), correct[i]);
        return map;
    }

    @Test
    void AC6_멤버별_최근_풀이로_모듈별_정답률과_멤버별_표를_준다() throws Exception {
        Example e = example();
        long quiz = quizOf(e.analysis());
        insertAttempt(quiz, e.u(), BASE.plusHours(1), answers(e.q(), false, false, false, false));
        insertAttempt(quiz, e.u(), BASE.plusHours(2), answers(e.q(), true, false, true, false));
        insertAttempt(quiz, e.v(), BASE.plusHours(1), answers(e.q(), true, true, false, true));

        perform(get(UNDERSTANDING, e.team(), e.analysis()), e.w())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisId").value(e.analysis()))
                .andExpect(jsonPath("$.title").value("spring"))
                .andExpect(jsonPath("$.sharedBy.userId").value(e.u()))
                .andExpect(jsonPath("$.questionCount").value(4))
                .andExpect(jsonPath("$.memberCount").value(3))
                .andExpect(jsonPath("$.solvedMemberCount").value(2))
                .andExpect(jsonPath("$.modules", hasSize(3)))
                .andExpect(jsonPath("$.modules[0].layer").value("controller"))
                .andExpect(jsonPath("$.modules[0].questionCount").value(2))
                .andExpect(jsonPath("$.modules[0].correctRate").value(75))
                .andExpect(jsonPath("$.modules[1].layer").value("service"))
                .andExpect(jsonPath("$.modules[1].correctRate").value(50))
                .andExpect(jsonPath("$.modules[2].layer").value("other"))
                .andExpect(jsonPath("$.modules[2].questionCount").value(1))
                .andExpect(jsonPath("$.modules[2].correctRate").value(50))
                .andExpect(jsonPath("$.members", hasSize(3)))
                .andExpect(jsonPath("$.members[0].userId").value(e.u()))
                .andExpect(jsonPath("$.members[0].role").value("OWNER"))
                .andExpect(jsonPath("$.members[0].lastAttempt.score").value(2))
                .andExpect(jsonPath("$.members[0].lastAttempt.total").value(4))
                .andExpect(jsonPath("$.members[0].modules[0].layer").value("controller"))
                .andExpect(jsonPath("$.members[0].modules[0].correct").value(1))
                .andExpect(jsonPath("$.members[0].modules[0].total").value(2))
                .andExpect(jsonPath("$.members[0].modules[1].correct").value(1))
                .andExpect(jsonPath("$.members[0].modules[1].total").value(1))
                .andExpect(jsonPath("$.members[0].modules[2].layer").value("other"))
                .andExpect(jsonPath("$.members[0].modules[2].correct").value(0))
                .andExpect(jsonPath("$.members[1].userId").value(e.v()))
                .andExpect(jsonPath("$.members[1].modules[0].correct").value(2))
                .andExpect(jsonPath("$.members[2].userId").value(e.w()))
                .andExpect(jsonPath("$.members[2].lastAttempt").value(nullValue()))
                .andExpect(jsonPath("$.members[2].modules", hasSize(0)));
    }

    // ── AC7 ──

    @Test
    void AC7_아무도_풀지_않았으면_정답률이_null이다() throws Exception {
        Example e = example();

        perform(get(UNDERSTANDING, e.team(), e.analysis()), e.u())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solvedMemberCount").value(0))
                .andExpect(jsonPath("$.modules[0].correctRate").value(nullValue()))
                .andExpect(jsonPath("$.modules[1].correctRate").value(nullValue()))
                .andExpect(jsonPath("$.modules[2].correctRate").value(nullValue()));
    }

    @Test
    void AC7_연결이_없거나_파일이_없는_개념의_문항은_기타이고_떠난_멤버의_풀이는_빠진다() throws Exception {
        long u = insertUser("f20bu");
        long v = insertUser("f20bv");
        long left = insertUser("f20bleft");
        long team = insertTeam(u, "팀", "TEAM0001", BASE);
        addMember(team, v, BASE);
        long a = doneAnalysis(v, "a");
        long unlinked = insertConcept("unlinked");
        long noFile = insertConcept("nofile");
        linkConcept(a, noFile, null);
        long quiz = insertQuiz(a);
        List<Long> q = List.of(insertQuestion(quiz, unlinked), insertQuestion(quiz, noFile));
        share(team, a, BASE);
        insertAttempt(quiz, v, BASE.plusHours(1), answers(q, true, false)); // 주인
        insertAttempt(quiz, u, BASE.plusHours(1), answers(q, true, true)); // 주인 아닌 멤버
        insertAttempt(quiz, left, BASE.plusHours(1), answers(q, false, false)); // 멤버 아님

        perform(get(UNDERSTANDING, team, a), u)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberCount").value(2))
                .andExpect(jsonPath("$.solvedMemberCount").value(2))
                .andExpect(jsonPath("$.modules", hasSize(1)))
                .andExpect(jsonPath("$.modules[0].layer").value("other"))
                .andExpect(jsonPath("$.modules[0].questionCount").value(2))
                .andExpect(jsonPath("$.modules[0].correctRate").value(75))
                .andExpect(jsonPath("$.members", hasSize(2)));
    }

    @Test
    void AC7_공유되지_않았거나_삭제된_분석은_TEAM_006이고_멤버가_아니면_TEAM_404다() throws Exception {
        Example e = example();
        long outsider = insertUser("f20bout");
        long notShared = analysisWithQuiz(e.u(), "notshared");

        expectError(perform(get(UNDERSTANDING, e.team(), notShared), e.u()), 404, "TEAM_006");
        expectError(perform(get(UNDERSTANDING, e.team(), e.analysis()), outsider), 404, "TEAM_404");
        jdbc.update("UPDATE analyses SET deleted_at = NOW() WHERE id = ?", e.analysis());
        expectError(perform(get(UNDERSTANDING, e.team(), e.analysis()), e.u()), 404, "TEAM_006");
        expectError(perform(delete(ANALYSIS, e.team(), e.analysis()), e.u()), 404, "TEAM_006");
    }

    // ── F-20c AC10 (#89) ──

    @Test
    void AC10_제목이_없는_공유_분석의_이해도에_projectName이_있다() throws Exception {
        long u = insertUser("f20bu");
        long team = insertTeam(u, "팀", "TEAM0001", BASE);
        jdbc.update(
                "INSERT INTO analyses (user_id, source_type, project_name, title, level, status,"
                        + " progress, created_at) VALUES (?, 'ZIP', 'todo-app', NULL, 'JUNIOR',"
                        + " 'DONE', 100, ?)",
                u,
                BASE);
        long a = lastId();
        insertQuestion(insertQuiz(a), null);
        share(team, a, BASE);

        perform(get(UNDERSTANDING, team, a), u)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(nullValue()))
                .andExpect(jsonPath("$.projectName").value("todo-app"))
                .andExpect(jsonPath("$.questionCount").value(1));
    }

    // ── AC8 ──

    @Test
    void AC8_공유한_사람이_팀을_떠나면_그_팀의_공유만_해제되고_풀이는_남는다() throws Exception {
        long u = insertUser("f20bu");
        long v = insertUser("f20bv");
        long x = insertUser("f20bx");
        long team = insertTeam(u, "팀", "TEAM0001", BASE);
        long team2 = insertTeam(x, "팀2", "TEAM0002", BASE);
        addMember(team, v, BASE);
        addMember(team2, v, BASE);
        long a = analysisWithQuiz(v, "a");
        long ua = analysisWithQuiz(u, "ua");
        share(team, a, BASE);
        share(team2, a, BASE);
        share(team, ua, BASE);
        long question =
                jdbc.queryForObject(
                        "SELECT id FROM quiz_questions WHERE quiz_id = ?", Long.class, quizOf(a));
        insertAttempt(quizOf(a), u, BASE.plusHours(1), Map.of(question, true));

        perform(delete("/api/teams/{id}/members/me", team), v).andExpect(status().isNoContent());

        assertThat(isShared(team, a)).isFalse();
        assertThat(isShared(team, ua)).isTrue();
        assertThat(isShared(team2, a)).isTrue();
        assertThat(attemptCount(u)).isEqualTo(1);

        // 팀장이 내보내도 같다
        addMember(team, v, BASE);
        share(team, a, BASE);
        perform(delete("/api/teams/{id}/members/{userId}", team, v), u)
                .andExpect(status().isNoContent());
        assertThat(isShared(team, a)).isFalse();
        assertThat(isShared(team2, a)).isTrue();
    }

    @Test
    void AC8_팀을_삭제하면_그_팀의_공유가_모두_지워진다() throws Exception {
        long u = insertUser("f20bu");
        long team = insertTeam(u, "팀", "TEAM0001", BASE);
        long team2 = insertTeam(u, "팀2", "TEAM0002", BASE);
        long a = analysisWithQuiz(u, "a");
        share(team, a, BASE);
        share(team2, a, BASE);

        perform(delete("/api/teams/{id}", team), u).andExpect(status().isNoContent());

        assertThat(count("SELECT COUNT(*) FROM team_analyses WHERE team_id = ?", team)).isZero();
        assertThat(isShared(team2, a)).isTrue();
    }

    @Test
    void AC8_회원_탈퇴하면_그_회원이_공유한_모든_행이_지워진다() throws Exception {
        long u = insertUser("f20bu");
        long x = insertUser("f20bx");
        long v = insertUser("f20bv");
        jdbc.update(
                "UPDATE users SET password_hash = ? WHERE id = ?",
                passwordEncoder.encode(PASSWORD),
                v);
        long team = insertTeam(u, "팀", "TEAM0001", BASE);
        long team2 = insertTeam(x, "팀2", "TEAM0002", BASE);
        addMember(team, v, BASE);
        addMember(team2, v, BASE);
        long a = analysisWithQuiz(v, "a");
        long ua = analysisWithQuiz(u, "ua");
        share(team, a, BASE);
        share(team2, a, BASE);
        share(team, ua, BASE);
        long question =
                jdbc.queryForObject(
                        "SELECT id FROM quiz_questions WHERE quiz_id = ?", Long.class, quizOf(ua));
        insertAttempt(quizOf(ua), v, BASE.plusHours(1), Map.of(question, true));

        perform(
                        delete("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"password\":\"" + PASSWORD + "\"}"),
                        v)
                .andExpect(status().isNoContent());

        assertThat(count("SELECT COUNT(*) FROM team_analyses WHERE analysis_id = ?", a)).isZero();
        assertThat(isShared(team, ua)).isTrue();
        assertThat(attemptCount(v)).isEqualTo(1);
    }

    // ── AC9 ──

    @Test
    void AC9_토큰이_없거나_만료_깨졌거나_탈퇴한_회원이면_새_API가_401이다() throws Exception {
        long u = insertUser("f20bu");
        long gone = insertUser("f20bgone");
        long team = insertTeam(u, "팀", "TEAM0001", BASE);
        addMember(team, gone, BASE);
        long a = analysisWithQuiz(gone, "a");
        long b = analysisWithQuiz(gone, "b");
        share(team, a, BASE);
        withdraw(gone);
        String body = shareJson(b);

        List<Supplier<MockHttpServletRequestBuilder>> requests =
                List.of(
                        () -> post(ANALYSES, team).contentType("application/json").content(body),
                        () -> get(ANALYSES, team),
                        () -> delete(ANALYSIS, team, a),
                        () -> get(UNDERSTANDING, team, a));
        String[] authorizations = {null, expiredBearer(u), "Bearer not-a-jwt", bearer(gone)};

        for (Supplier<MockHttpServletRequestBuilder> request : requests) {
            for (String authorization : authorizations) {
                perform(request.get(), authorization, null)
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value("AUTH_401"));
            }
        }

        assertThat(isShared(team, a)).isTrue();
        assertThat(isShared(team, b)).isFalse();
    }
}
