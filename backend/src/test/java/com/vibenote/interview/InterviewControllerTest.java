package com.vibenote.interview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** I-1a AC1(202 응답)·AC8(권한·상태)·AC9(조회). 생성기는 가짜로 둔다. */
class InterviewControllerTest extends InterviewTestSupport {

    @MockitoBean InterviewGenerator generator;

    private String url(long analysisId) {
        return "/api/analyses/" + analysisId + "/interview";
    }

    @Test
    void AC1_DONE_분석에_면접_질문을_요청하면_202와_PENDING을_받는다() throws Exception {
        long userId = createUser("ctl1");
        long analysisId = analysis(userId, "DONE", "JUNIOR");

        mockMvc.perform(post(url(analysisId)).header("Authorization", bearer(userId)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.analysisId").value(analysisId))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.errorCode").isEmpty())
                .andExpect(jsonPath("$.errorMessage").isEmpty())
                .andExpect(jsonPath("$.regenerationsLeft").value(2))
                .andExpect(jsonPath("$.questions").value(nullValue()))
                .andExpect(jsonPath("$.generatedAt").isEmpty());
    }

    @Test
    void AC8_DONE이_아니면_409_남의_것_없는_것_삭제된_것은_404_토큰_없으면_401() throws Exception {
        long userId = createUser("ctl2");
        long other = createUser("ctl2o");
        long running = analysis(userId, "QUIZ", "JUNIOR");
        long done = analysis(userId, "DONE", "JUNIOR");
        long deleted = analysis(userId, "DONE", "JUNIOR");
        jdbc.update(
                "UPDATE analyses SET deleted_at = NOW(), is_public = TRUE WHERE id = ?", deleted);
        jdbc.update("UPDATE analyses SET is_public = TRUE WHERE id = ?", done);

        mockMvc.perform(post(url(running)).header("Authorization", bearer(userId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INTERVIEW_409"));
        mockMvc.perform(post(url(done)).header("Authorization", bearer(other)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
        mockMvc.perform(get(url(done)).header("Authorization", bearer(other)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
        mockMvc.perform(post(url(deleted)).header("Authorization", bearer(userId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(url(deleted)).header("Authorization", bearer(userId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(url(999_999_999L)).header("Authorization", bearer(userId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(url(done)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
        mockMvc.perform(get(url(done))).andExpect(status().isUnauthorized());
    }

    @Test
    void AC8_팀에_공유된_분석도_주인이_아니면_POST_GET_모두_404() throws Exception {
        long owner = createUser("ctl2t");
        long member = createUser("ctl2m");
        long shared = analysis(owner, "DONE", "JUNIOR");
        jdbc.update(
                "INSERT INTO teams (name, invite_code, owner_id) VALUES ('i1a팀', 'I1AAC8TM', ?)",
                owner);
        long teamId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update(
                "INSERT INTO team_members (team_id, user_id, role) VALUES (?, ?, 'OWNER'), (?, ?,"
                        + " 'MEMBER')",
                teamId,
                owner,
                teamId,
                member);
        jdbc.update(
                "INSERT INTO team_analyses (team_id, analysis_id) VALUES (?, ?)", teamId, shared);

        mockMvc.perform(post(url(shared)).header("Authorization", bearer(member)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
        mockMvc.perform(get(url(shared)).header("Authorization", bearer(member)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
        Integer rows =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM analysis_interviews WHERE analysis_id = ?",
                        Integer.class,
                        shared);
        assertThat(rows).isZero();
    }

    @Test
    void AC6_다시_만들기_횟수를_다_쓰면_429() throws Exception {
        long userId = createUser("ctl3");
        long analysisId = analysis(userId, "DONE", "JUNIOR");
        insertInterview(analysisId, "DONE", 2, null);

        mockMvc.perform(post(url(analysisId)).header("Authorization", bearer(userId)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("INTERVIEW_429"))
                .andExpect(jsonPath("$.message").value("이 분석의 면접 질문 다시 만들기 횟수(2회)를 모두 썼습니다."));
    }

    @Test
    void AC9_요청한_적이_없으면_진행_중_분석이어도_NONE이다() throws Exception {
        long userId = createUser("ctl4");
        long running = analysis(userId, "EXPLAINING", "JUNIOR");

        mockMvc.perform(get(url(running)).header("Authorization", bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NONE"))
                .andExpect(jsonPath("$.questions").value(nullValue()))
                .andExpect(jsonPath("$.regenerationsLeft").value(2));
    }

    @Test
    void AC9_DONE이면_질문을_no_순으로_주고_관련_카드는_숨김_삭제된_카드를_뺀다() throws Exception {
        long userId = createUser("ctl5");
        long analysisId = analysis(userId, "DONE", "JUNIOR");
        long fileId = fileIds(analysisId).get(0);
        long shown = concept("보임", "PUBLISHED");
        long draft = concept("초안", "DRAFT");
        long hidden = concept("숨김", "HIDDEN");
        long removed = concept("지움", "DRAFT");
        link(analysisId, shown, null);
        link(analysisId, draft, null);
        link(analysisId, hidden, null);
        link(analysisId, removed, null);
        long interviewId = insertInterview(analysisId, "DONE", 1, null);
        insertQuestion(interviewId, 2, fileId, "[]");
        insertQuestion(
                interviewId,
                1,
                fileId,
                "[%d, %d, %d, %d]".formatted(hidden, shown, removed, draft));
        jdbc.update("DELETE FROM concepts WHERE id = ?", removed);

        mockMvc.perform(get(url(analysisId)).header("Authorization", bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.regenerationsLeft").value(1))
                .andExpect(jsonPath("$.generatedAt").isNotEmpty())
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andExpect(jsonPath("$.questions[0].no").value(1))
                .andExpect(jsonPath("$.questions[0].category").value("FLOW"))
                .andExpect(jsonPath("$.questions[0].question").value("질문 1"))
                .andExpect(jsonPath("$.questions[0].keyPoints[0]").value("포인트"))
                .andExpect(jsonPath("$.questions[0].evidence[0].fileId").value(fileId))
                .andExpect(jsonPath("$.questions[0].evidence[0].startLine").value(1))
                .andExpect(jsonPath("$.questions[0].evidence[0].endLine").value(2))
                .andExpect(jsonPath("$.questions[0].concepts.length()").value(2))
                .andExpect(jsonPath("$.questions[0].concepts[0].id").value(shown))
                .andExpect(jsonPath("$.questions[0].concepts[0].name").value("I1A-보임"))
                .andExpect(jsonPath("$.questions[0].concepts[1].id").value(draft))
                .andExpect(jsonPath("$.questions[1].no").value(2))
                .andExpect(jsonPath("$.questions[1].concepts.length()").value(0));
    }

    @Test
    void AC9_FAILED면_오류_코드와_문구를_주고_질문은_null이다() throws Exception {
        long userId = createUser("ctl6");
        long analysisId = analysis(userId, "DONE", "JUNIOR");
        insertInterview(analysisId, "FAILED", 0, "LLM_503");

        mockMvc.perform(get(url(analysisId)).header("Authorization", bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorCode").value("LLM_503"))
                .andExpect(jsonPath("$.errorMessage").value("AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요."))
                .andExpect(jsonPath("$.questions").value(nullValue()));
    }

    @Test
    void AC4_LLM_502로_FAILED면_면접_질문_전용_문구를_준다() throws Exception {
        // 공통 LLM_502 문구는 "설명을 만들지 못했습니다"라 면접 탭에 맞지 않는다 (reviewer 권장, 결정로그 11-28)
        long userId = createUser("ctl7");
        long analysisId = analysis(userId, "DONE", "JUNIOR");
        insertInterview(analysisId, "FAILED", 0, "LLM_502");

        mockMvc.perform(get(url(analysisId)).header("Authorization", bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("LLM_502"))
                .andExpect(
                        jsonPath("$.errorMessage")
                                .value("AI 응답 형식이 올바르지 않아 면접 질문을 만들지 못했습니다. 다시 요청해 주세요."));
    }
}
