package com.vibenote.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * F-20b AC5: 팀에 공유된 분석의 퀴즈는 그 팀 멤버도 조회·제출할 수 있다. 퀴즈 외 분석 API는 여전히 주인만 본다
 * (docs/specs/F-20b-team-understanding.md).
 */
class QuizTeamAccessApiTest extends QuizTestSupport {

    private ResultActions getQuiz(long userId, long analysisId) throws Exception {
        return mockMvc.perform(
                get("/api/analyses/{id}/quiz", analysisId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId)));
    }

    private ResultActions submit(long userId, long quizId, long questionId, int selected)
            throws Exception {
        return mockMvc.perform(
                post("/api/quizzes/{quizId}/attempts", quizId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"answers\": [{\"questionId\": %d, \"selectedIndex\": %d}]}"
                                        .formatted(questionId, selected)));
    }

    private long insertTeam(long ownerId) {
        jdbc.update(
                "INSERT INTO teams (name, owner_id, invite_code) VALUES ('팀', ?, ?)",
                ownerId,
                "T0" + ownerId % 1_000_000);
        long teamId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        addMember(teamId, ownerId, "OWNER");
        return teamId;
    }

    private void addMember(long teamId, long userId, String role) {
        jdbc.update(
                "INSERT INTO team_members (team_id, user_id, role) VALUES (?, ?, ?)",
                teamId,
                userId,
                role);
    }

    private void share(long teamId, long analysisId) {
        jdbc.update(
                "INSERT INTO team_analyses (team_id, analysis_id) VALUES (?, ?)",
                teamId,
                analysisId);
    }

    /** 주인 U, 팀원 W, 팀 밖 X. U의 분석 A(퀴즈 1문항, 정답 0, 공개 개념)를 팀에 공유한다. */
    private record Setup(
            long u, long w, long x, long team, long analysis, long quiz, long q, long concept) {}

    private Setup setup(String prefix) {
        long u = createUser(prefix + "u");
        long w = createUser(prefix + "w");
        long x = createUser(prefix + "x");
        long team = insertTeam(u);
        addMember(team, w, "MEMBER");
        long analysis = insertAnalysis(u, false);
        long quiz = insertQuiz(analysis);
        long concept = insertConcept("f14-test-" + prefix, "PUBLISHED");
        List<Long> q = insertQuestions(quiz, new int[] {0}, concept);
        share(team, analysis);
        return new Setup(u, w, x, team, analysis, quiz, q.get(0), concept);
    }

    @Test
    void AC5_팀원은_공유된_분석의_퀴즈를_조회하고_제출하며_오답은_자기_복습_계획에_들어간다() throws Exception {
        Setup s = setup("f20b5a");
        jdbc.update(
                "INSERT INTO quiz_attempts (quiz_id, user_id, score, total, created_at) VALUES (?,"
                        + " ?, 1, 1, '2026-11-19 10:00:00')",
                s.quiz(),
                s.u());

        getQuiz(s.w(), s.analysis())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizId").value(s.quiz()))
                .andExpect(jsonPath("$.lastAttempt").value(nullValue()));

        submit(s.w(), s.quiz(), s.q(), 1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(0))
                .andExpect(jsonPath("$.reviewAdded[0].conceptId").value(s.concept()));

        assertThat(count("quiz_attempts", "quiz_id = ? AND user_id = ?", s.quiz(), s.w()))
                .isEqualTo(1);
        assertThat(
                        count(
                                "review_plans",
                                "user_id = ? AND concept_id = ? AND reason = 'WRONG'",
                                s.w(),
                                s.concept()))
                .isEqualTo(1);
        assertThat(count("review_plans", "user_id = ?", s.u())).isZero();
        getQuiz(s.w(), s.analysis()).andExpect(jsonPath("$.lastAttempt.score").value(0));
        // 주인은 그대로 쓸 수 있다
        getQuiz(s.u(), s.analysis()).andExpect(jsonPath("$.lastAttempt.score").value(1));
    }

    @Test
    void AC5_팀_밖_회원과_해제_탈퇴_삭제_뒤에는_조회_제출이_404다() throws Exception {
        Setup s = setup("f20b5b");

        getQuiz(s.x(), s.analysis())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
        submit(s.x(), s.quiz(), s.q(), 0)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUIZ_404"));

        // 해제
        jdbc.update("DELETE FROM team_analyses WHERE team_id = ?", s.team());
        getQuiz(s.w(), s.analysis()).andExpect(jsonPath("$.code").value("ANALYSIS_404"));
        submit(s.w(), s.quiz(), s.q(), 0).andExpect(jsonPath("$.code").value("QUIZ_404"));

        // 다시 공유했지만 W가 팀을 떠남
        share(s.team(), s.analysis());
        jdbc.update("DELETE FROM team_members WHERE user_id = ?", s.w());
        getQuiz(s.w(), s.analysis()).andExpect(jsonPath("$.code").value("ANALYSIS_404"));
        submit(s.w(), s.quiz(), s.q(), 0).andExpect(jsonPath("$.code").value("QUIZ_404"));

        // 다시 들어왔지만 분석이 soft delete됨
        addMember(s.team(), s.w(), "MEMBER");
        getQuiz(s.w(), s.analysis()).andExpect(status().isOk());
        jdbc.update("UPDATE analyses SET deleted_at = NOW() WHERE id = ?", s.analysis());
        getQuiz(s.w(), s.analysis()).andExpect(jsonPath("$.code").value("ANALYSIS_404"));
        submit(s.w(), s.quiz(), s.q(), 0).andExpect(jsonPath("$.code").value("QUIZ_404"));

        assertThat(count("quiz_attempts", "user_id IN (?, ?)", s.w(), s.x())).isZero();
    }

    @Test
    void AC5_퀴즈_외_분석_API는_팀원에게_계속_404다() throws Exception {
        Setup s = setup("f20b5c");
        jdbc.update(
                "INSERT INTO analysis_files (analysis_id, path, layer, content) VALUES (?,"
                        + " 'A.java', 'controller', 'code')",
                s.analysis());
        long fileId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        for (String path :
                List.of(
                        "/api/analyses/" + s.analysis(),
                        "/api/analyses/" + s.analysis() + "/status",
                        "/api/analyses/" + s.analysis() + "/files",
                        "/api/analyses/" + s.analysis() + "/files/" + fileId,
                        "/api/analyses/" + s.analysis() + "/concepts",
                        "/api/analyses/" + s.analysis() + "/flows")) {
            mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer(s.w())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
        }
    }
}
