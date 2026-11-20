package com.vibenote.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.Level;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmRequest;
import com.vibenote.llm.LlmResponse;
import com.vibenote.llm.LlmStage;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;

/**
 * F-16 추천 API (docs/specs/F-16-recommendation.md AC1~AC6). 오늘 = 2026-11-20. LLM은 실제 빈(사용량 기록 + Mock
 * fixture)을 감싼 spy이고, 필요한 테스트만 응답을 바꾼다.
 */
class RecommendationApiTest extends RecommendationTestSupport {

    /** main {@code llm-fixtures/recommend.json}의 첫 이유 (Mock은 프롬프트 카드 순서대로 붙인다). */
    static final String FIXTURE_FIRST = "지금 공부해 두면 AI가 쓴 코드에서 이 개념을 만났을 때 바로 알아볼 수 있어요.";

    static final String FIXTURE_SECOND = "핵심만 짧게 정리된 카드라 다음 분석 전에 가볍게 읽어 두기 좋아요.";

    @MockitoSpyBean LlmClient llmClient;

    private ResultActions recommend(long userId, String query) throws Exception {
        return mockMvc.perform(
                get("/api/recommendations" + query)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId)));
    }

    private List<LlmRequest> recommendRequests() {
        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmClient, atLeast(0)).complete(captor.capture());
        return captor.getAllValues().stream().filter(r -> r.stage() == LlmStage.RECOMMEND).toList();
    }

    private void answer(String content) {
        doReturn(new LlmResponse(content, "fake", 0, 0))
                .when(llmClient)
                .complete(argThat(r -> r != null && r.stage() == LlmStage.RECOMMEND));
    }

    private Map<String, Object> reasonRow(long userId, long conceptId) {
        return jdbc.queryForMap(
                "SELECT reason, signal_key, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS"
                        + " created_at FROM recommendation_reasons WHERE user_id = ? AND concept_id"
                        + " = ?",
                userId, conceptId);
    }

    // ── AC1 점수·정렬 ──

    @Test
    void AC1_신호별_점수를_합산해_높은_순으로_5개를_돌려주고_다른_회원의_활동은_넣지_않는다() throws Exception {
        long me = createUser("f16ac1", "JUNIOR");
        long other = createUser("f16ac1o", "JUNIOR");
        long a = insertConcept("A", "security", "JUNIOR", "PUBLISHED", 0, 0.0);
        long b = insertConcept("B", "spring", "JUNIOR", "PUBLISHED", 0, 0.0);
        long c = insertConcept("C", "vue", "SENIOR", "PUBLISHED", 0, 0.0);
        long d = insertConcept("D", "java", "JUNIOR", "PUBLISHED", 5, 0.0);
        long e = insertConcept("E", "java", "BEGINNER", "PUBLISHED", 0, 4.0);
        long f = insertConcept("F", "java", "SENIOR", "PUBLISHED", 0, 3.0);
        long wished = insertConcept("W", "vue", "JUNIOR");

        long myAnalysis = insertAnalysis(me, false);
        linkConcept(myAnalysis, b);
        long myQuiz = insertQuiz(myAnalysis);
        long qa1 = insertQuestion(myQuiz, a);
        long qa2 = insertQuestion(myQuiz, a);
        long qb = insertQuestion(myQuiz, b);
        long qf = insertQuestion(myQuiz, f);
        long qNone = insertQuestion(myQuiz, null);
        insertAttempt(myQuiz, me, List.of(qa1, qa2, qb, qNone), List.of(qf));
        insertAttempt(myQuiz, me, List.of(qa1, qa2), List.of(qb, qf));
        insertWish(me, wished);

        long otherAnalysis = insertAnalysis(other, false);
        linkConcept(otherAnalysis, f);
        long otherQuiz = insertQuiz(otherAnalysis);
        long qOther = insertQuestion(otherQuiz, f);
        insertAttempt(otherQuiz, other, List.of(qOther), List.of());
        insertAttempt(myQuiz, other, List.of(qf), List.of());
        insertWish(other, d);
        insertWish(other, f);

        recommend(me, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(
                        jsonPath("$.items[*].concept.id")
                                .value(contains((int) a, (int) b, (int) d, (int) c, (int) e)))
                .andExpect(jsonPath("$.items[*].score").value(contains(10, 6, 1, 1, 0)))
                .andExpect(jsonPath("$.items[0].concept.name").value("f16-test-A"))
                .andExpect(jsonPath("$.items[0].concept.category").value("security"))
                .andExpect(jsonPath("$.items[0].concept.difficulty").value("JUNIOR"))
                .andExpect(jsonPath("$.items[0].concept.summary").value("A 요약"))
                .andExpect(jsonPath("$.items[0].concept.avgRating").value(0.0))
                .andExpect(jsonPath("$.items[0].concept.wishCount").value(0))
                .andExpect(jsonPath("$.items[0].signals.wrongCount").value(4))
                .andExpect(jsonPath("$.items[0].signals.inMyCode").value(false))
                .andExpect(jsonPath("$.items[0].signals.wishedCategory").value(false))
                .andExpect(jsonPath("$.items[0].signals.levelFit").value(1))
                .andExpect(jsonPath("$.items[0].reason").value(FIXTURE_FIRST))
                .andExpect(jsonPath("$.items[0].reasonSource").value("AI"))
                .andExpect(jsonPath("$.items[1].signals.wrongCount").value(1))
                .andExpect(jsonPath("$.items[1].signals.inMyCode").value(true))
                .andExpect(jsonPath("$.items[2].signals.wishedCategory").value(false))
                .andExpect(jsonPath("$.items[3].signals.wishedCategory").value(true))
                .andExpect(jsonPath("$.items[3].signals.levelFit").value(0))
                .andExpect(jsonPath("$.items[4].signals.wrongCount").value(0))
                .andExpect(jsonPath("$.items[4].signals.inMyCode").value(false));
    }

    // ── AC2 제외 규칙 ──

    @Test
    void AC2_복습을_끝낸_카드_오늘_복습할_카드_찜한_카드_비공개_카드를_빼고_삭제된_분석의_신호는_넣지_않는다() throws Exception {
        long me = createUser("f16ac2", "JUNIOR");
        long doneOnly = insertConcept("완료만", "java", "JUNIOR");
        long dueToday = insertConcept("오늘", "java", "JUNIOR");
        long overdue = insertConcept("밀림", "java", "JUNIOR");
        long wished = insertConcept("찜", "vue", "JUNIOR");
        insertConcept("숨김", "java", "JUNIOR", "HIDDEN", 0, 0.0);
        insertConcept("초안", "java", "JUNIOR", "DRAFT", 0, 0.0);
        long doneThenTodo = insertConcept("다시틀림", "java", "JUNIOR");
        long tomorrow = insertConcept("나중", "java", "JUNIOR");
        long deletedOnly = insertConcept("삭제분석", "java", "JUNIOR");

        insertPlan(me, doneOnly, "2026-11-18", "DONE");
        insertPlan(me, dueToday, "2026-11-20", "TODO");
        insertPlan(me, overdue, "2026-11-19", "TODO");
        insertWish(me, wished);
        insertPlan(me, doneThenTodo, "2026-11-18", "DONE");
        insertPlan(me, doneThenTodo, "2026-11-21", "TODO");
        insertPlan(me, tomorrow, "2026-11-25", "TODO");

        long deletedAnalysis = insertAnalysis(me, true);
        linkConcept(deletedAnalysis, deletedOnly);
        long quiz = insertQuiz(deletedAnalysis);
        long q = insertQuestion(quiz, deletedOnly);
        insertAttempt(quiz, me, List.of(q), List.of());

        recommend(me, "?size=10")
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.items[*].concept.id")
                                .value(
                                        containsInAnyOrder(
                                                (int) doneThenTodo,
                                                (int) tomorrow,
                                                (int) deletedOnly)))
                .andExpect(
                        jsonPath(
                                        "$.items[?(@.concept.id == "
                                                + deletedOnly
                                                + ")].signals.wrongCount")
                                .value(contains(0)))
                .andExpect(
                        jsonPath("$.items[?(@.concept.id == " + deletedOnly + ")].signals.inMyCode")
                                .value(contains(false)))
                .andExpect(
                        jsonPath(
                                        "$.items[?(@.concept.id == "
                                                + deletedOnly
                                                + ")].signals.wishedCategory")
                                .value(contains(false)));
    }

    // ── AC3 새 회원 ──

    @Test
    void AC3_활동이_없는_BEGINNER는_수준에_맞는_인기_카드를_size만큼_받는다() throws Exception {
        long me = createUser("f16ac3", "BEGINNER");
        long b1 = insertConcept("B1", "java", "BEGINNER", "PUBLISHED", 1, 0.0);
        long b2 = insertConcept("B2", "java", "BEGINNER", "PUBLISHED", 1, 4.0);
        long b3 = insertConcept("B3", "web", "BEGINNER", "PUBLISHED", 3, 0.0);
        insertConcept("J1", "java", "JUNIOR", "PUBLISHED", 99, 5.0);
        insertConcept("S1", "java", "SENIOR", "PUBLISHED", 99, 5.0);

        recommend(me, "?size=3")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(
                        jsonPath("$.items[*].concept.id")
                                .value(contains((int) b3, (int) b2, (int) b1)))
                .andExpect(jsonPath("$.items[*].score").value(contains(1, 1, 1)));
    }

    @Test
    void AC3_공개_카드가_없으면_빈_목록이고_LLM을_부르지_않는다() throws Exception {
        long me = createUser("f16ac3e", "BEGINNER");
        insertConcept("숨김", "java", "BEGINNER", "HIDDEN", 0, 0.0);

        recommend(me, "").andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)));
        verify(llmClient, never()).complete(any());
    }

    // ── AC4 요청 오류 ──

    @Test
    void AC4_size가_정수가_아니거나_범위_밖이면_400() throws Exception {
        long me = createUser("f16ac4", "JUNIOR");
        for (String size : List.of("abc", "0", "11", "1.5")) {
            recommend(me, "?size=" + size)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_400"))
                    .andExpect(jsonPath("$.details.size").exists());
        }
        recommend(me, "?size=10").andExpect(status().isOk());
    }

    @Test
    void AC4_토큰이_없으면_401() throws Exception {
        mockMvc.perform(get("/api/recommendations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    // ── AC5 이유 캐시 ──

    @Test
    void AC5_캐시가_없거나_신호_키가_바뀐_카드만_모아_LLM을_1회_부르고_저장한다() throws Exception {
        long me = createUser("f16ac5", "SENIOR");
        long p = insertConcept("P", "java", "SENIOR", "PUBLISHED", 2, 0.0);
        long q = insertConcept("Q", "java", "SENIOR", "PUBLISHED", 1, 0.0);

        // 1) 둘 다 캐시 없음 → LLM 1회, 둘 다 저장
        recommend(me, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].concept.id").value(contains((int) p, (int) q)))
                .andExpect(jsonPath("$.items[0].reason").value(FIXTURE_FIRST))
                .andExpect(jsonPath("$.items[1].reason").value(FIXTURE_SECOND))
                .andExpect(jsonPath("$.items[*].reasonSource").value(contains("AI", "AI")));
        List<LlmRequest> first = recommendRequests();
        assertThat(first).hasSize(1);
        assertThat(first.get(0).level()).isEqualTo(Level.SENIOR);
        assertThat(first.get(0).userId()).isEqualTo(me);
        assertThat(first.get(0).analysisId()).isNull();
        assertThat(first.get(0).prompt()).contains(p + " | f16-test-P |", q + " | f16-test-Q |");
        assertThat(reasonRow(me, p))
                .containsEntry("reason", FIXTURE_FIRST)
                .containsEntry("signal_key", "w0c0k0l1")
                .containsEntry("created_at", "2026-11-20 10:00:00");
        assertThat(reasonRow(me, q)).containsEntry("signal_key", "w0c0k0l1");
        assertThat(count("llm_usage_logs", "user_id = ? AND stage = 'recommend'", me)).isEqualTo(1);

        // 2) 모두 캐시 적중 → LLM 없이 저장된 이유
        jdbc.update(
                "UPDATE recommendation_reasons SET reason = '저장된 이유', created_at = '2026-11-01'"
                        + " WHERE user_id = ?",
                me);
        recommend(me, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].reason").value(contains("저장된 이유", "저장된 이유")))
                .andExpect(jsonPath("$.items[*].reasonSource").value(contains("AI", "AI")));
        assertThat(recommendRequests()).hasSize(1);

        // 3) P의 신호가 바뀜(내 코드에 나옴) → P만 다시 만든다
        linkConcept(insertAnalysis(me, false), p);
        recommend(me, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].concept.id").value(contains((int) p, (int) q)))
                .andExpect(jsonPath("$.items[0].reason").value(FIXTURE_FIRST))
                .andExpect(jsonPath("$.items[1].reason").value("저장된 이유"));
        List<LlmRequest> all = recommendRequests();
        assertThat(all).hasSize(2);
        assertThat(all.get(1).prompt()).contains(p + " | f16-test-P |");
        assertThat(all.get(1).prompt()).doesNotContain(q + " | f16-test-Q |");
        assertThat(reasonRow(me, p))
                .containsEntry("reason", FIXTURE_FIRST)
                .containsEntry("signal_key", "w0c1k0l1")
                .containsEntry("created_at", "2026-11-20 10:00:00");
        assertThat(reasonRow(me, q))
                .containsEntry("signal_key", "w0c0k0l1")
                .containsEntry("created_at", "2026-11-01 00:00:00");
    }

    // ── AC6 LLM 실패 ──

    @Test
    void AC6_LLM이_실패하면_규칙_문장을_RULE로_주고_저장하지_않는다() throws Exception {
        long me = createUser("f16ac6", "JUNIOR");
        long p = insertConcept("P", "java", "JUNIOR", "PUBLISHED", 0, 0.0);
        long q = insertConcept("Q", "java", "SENIOR", "PUBLISHED", 1, 0.0);
        doThrow(new BusinessException(ErrorCode.LLM_503))
                .when(llmClient)
                .complete(argThat(r -> r != null && r.stage() == LlmStage.RECOMMEND));

        recommend(me, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].concept.id").value(contains((int) p, (int) q)))
                .andExpect(jsonPath("$.items[0].reason").value("내 수준(주니어)에 맞는 카드예요."))
                .andExpect(jsonPath("$.items[1].reason").value("많은 회원이 찜한 카드예요."))
                .andExpect(jsonPath("$.items[*].reasonSource").value(contains("RULE", "RULE")));
        assertThat(count("recommendation_reasons", "user_id = ?", me)).isZero();
    }

    @Test
    void AC6_형식_오류가_두_번이면_규칙_문장이고_다음_조회에서_다시_시도한다() throws Exception {
        long me = createUser("f16ac6f", "JUNIOR");
        insertConcept("P", "java", "JUNIOR");
        answer("not json");

        recommend(me, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].reasonSource").value("RULE"));
        assertThat(recommendRequests()).hasSize(2);
        assertThat(count("recommendation_reasons", "user_id = ?", me)).isZero();

        recommend(me, "").andExpect(jsonPath("$.items[0].reasonSource").value("RULE"));
        assertThat(recommendRequests()).hasSize(4);
    }

    @Test
    void AC6_일부_카드의_이유가_빠지면_그_카드만_RULE이고_받은_카드는_저장한다() throws Exception {
        long me = createUser("f16ac6p", "JUNIOR");
        long p = insertConcept("P", "java", "JUNIOR", "PUBLISHED", 1, 0.0);
        long q = insertConcept("Q", "java", "JUNIOR");
        answer(
                "{\"reasons\": [{\"conceptId\": %d, \"reason\": \"  P를 볼\\n이유  \"}, {\"conceptId\": 999999999, \"reason\": \"모름\"}]}"
                        .formatted(p));

        recommend(me, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].concept.id").value(contains((int) p, (int) q)))
                .andExpect(jsonPath("$.items[0].reason").value("P를 볼 이유"))
                .andExpect(jsonPath("$.items[0].reasonSource").value("AI"))
                .andExpect(jsonPath("$.items[1].reason").value("내 수준(주니어)에 맞는 카드예요."))
                .andExpect(jsonPath("$.items[1].reasonSource").value("RULE"));
        assertThat(reasonRow(me, p))
                .containsEntry("reason", "P를 볼 이유")
                .containsEntry("signal_key", "w0c0k0l1");
        assertThat(count("recommendation_reasons", "user_id = ?", me)).isEqualTo(1);
    }
}
