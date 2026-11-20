package com.vibenote.reviewplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** F-15 복습 계획 API (docs/specs/F-15-review-plan.md AC1~AC7). 오늘 = 2026-11-20. */
class ReviewPlanApiTest extends ReviewPlanTestSupport {

    private ResultActions list(long userId, String query) throws Exception {
        return mockMvc.perform(
                get("/api/review-plans" + query).header(HttpHeaders.AUTHORIZATION, bearer(userId)));
    }

    private ResultActions add(long userId, String body) throws Exception {
        return mockMvc.perform(
                post("/api/review-plans")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    private ResultActions changeStatus(long userId, long planId, String body) throws Exception {
        return mockMvc.perform(
                patch("/api/review-plans/{id}", planId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    private ResultActions remove(long userId, long planId) throws Exception {
        return mockMvc.perform(
                delete("/api/review-plans/{id}", planId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId)));
    }

    private Map<String, Object> row(long planId) {
        return jdbc.queryForMap(
                "SELECT DATE_FORMAT(plan_date, '%Y-%m-%d') AS plan_date, status, reason,"
                        + " DATE_FORMAT(done_at, '%Y-%m-%d %H:%i:%s') AS done_at"
                        + " FROM review_plans WHERE id = ?",
                planId);
    }

    private static Map<String, Object> row(
            String planDate, String status, String reason, String doneAt) {
        Map<String, Object> expected = new HashMap<>();
        expected.put("plan_date", planDate);
        expected.put("status", status);
        expected.put("reason", reason);
        expected.put("done_at", doneAt);
        return expected;
    }

    // ── AC1 기간 조회 ──

    @Test
    void AC1_기간을_주지_않으면_오늘부터_7일을_돌려준다() throws Exception {
        long me = createUser("f15ac1");
        long other = createUser("f15ac1o");
        long a = insertConcept("f15-test-A", "PUBLISHED");
        long b = insertConcept("f15-test-B", "PUBLISHED");
        long c = insertConcept("f15-test-C", "PUBLISHED");
        long hidden = insertConcept("f15-test-숨김", "HIDDEN");
        long draft = insertConcept("f15-test-초안", "DRAFT");
        long laterSameDay = insertPlan(me, c, "2026-11-21", "TODO", "MANUAL", null);
        long done = insertPlan(me, a, "2026-11-20", "DONE", "WISH", "2026-11-20 09:12:00");
        long firstOnDay = insertTodo(me, b, "2026-11-21");
        long lastDay = insertTodo(me, a, "2026-11-26");
        insertTodo(me, b, "2026-11-27"); // 기간 밖
        insertTodo(me, hidden, "2026-11-22");
        insertTodo(me, draft, "2026-11-22");
        insertTodo(other, c, "2026-11-22");

        list(me, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today").value("2026-11-20"))
                .andExpect(jsonPath("$.from").value("2026-11-20"))
                .andExpect(jsonPath("$.to").value("2026-11-26"))
                .andExpect(jsonPath("$.items", hasSize(4)))
                .andExpect(jsonPath("$.items[0].id").value(done))
                .andExpect(jsonPath("$.items[0].planDate").value("2026-11-20"))
                .andExpect(jsonPath("$.items[0].status").value("DONE"))
                .andExpect(jsonPath("$.items[0].reason").value("WISH"))
                .andExpect(jsonPath("$.items[0].doneAt").value("2026-11-20T09:12:00+09:00"))
                .andExpect(jsonPath("$.items[0].concept.id").value(a))
                .andExpect(jsonPath("$.items[0].concept.name").value("f15-test-A"))
                .andExpect(jsonPath("$.items[0].concept.category").value("security"))
                .andExpect(jsonPath("$.items[0].concept.difficulty").value("JUNIOR"))
                .andExpect(jsonPath("$.items[0].concept.summary").value("요약"))
                .andExpect(jsonPath("$.items[0].concept.avgRating").value(4.5))
                .andExpect(jsonPath("$.items[0].concept.wishCount").value(2))
                .andExpect(jsonPath("$.items[1].id").value(laterSameDay))
                .andExpect(jsonPath("$.items[1].reason").value("MANUAL"))
                .andExpect(jsonPath("$.items[1].status").value("TODO"))
                .andExpect(jsonPath("$.items[1].doneAt").value(nullValue()))
                .andExpect(jsonPath("$.items[2].id").value(firstOnDay))
                .andExpect(jsonPath("$.items[2].reason").value("WRONG"))
                .andExpect(jsonPath("$.items[3].id").value(lastDay))
                .andExpect(jsonPath("$.items[3].planDate").value("2026-11-26"))
                .andExpect(jsonPath("$.overdue", hasSize(0)));
    }

    @Test
    void AC1_기간을_주면_그_기간의_계획만_준다() throws Exception {
        long me = createUser("f15ac1r");
        long a = insertConcept("f15-test-A", "PUBLISHED");
        long b = insertConcept("f15-test-B", "PUBLISHED");
        insertTodo(me, a, "2026-11-30");
        long first = insertPlan(me, b, "2026-12-01", "DONE", "MANUAL", "2026-11-20 08:00:00");
        long last = insertTodo(me, a, "2026-12-31");
        insertTodo(me, b, "2027-01-01");

        list(me, "?from=2026-12-01&to=2026-12-31")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today").value("2026-11-20"))
                .andExpect(jsonPath("$.from").value("2026-12-01"))
                .andExpect(jsonPath("$.to").value("2026-12-31"))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].id").value(first))
                .andExpect(jsonPath("$.items[1].id").value(last));
    }

    // ── AC2 밀린 복습 ──

    @Test
    void AC2_지난_날짜의_TODO는_기간과_상관없이_overdue에_모두_담고_DONE은_뺀다() throws Exception {
        long me = createUser("f15ac2");
        long other = createUser("f15ac2o");
        long a = insertConcept("f15-test-A", "PUBLISHED");
        long b = insertConcept("f15-test-B", "PUBLISHED");
        long hidden = insertConcept("f15-test-숨김", "HIDDEN");
        long old = insertTodo(me, a, "2026-10-01");
        long yesterdayB = insertTodo(me, b, "2026-11-19");
        insertPlan(me, a, "2026-11-19", "DONE", "WRONG", "2026-11-19 20:00:00");
        insertTodo(me, hidden, "2026-11-18");
        insertTodo(other, a, "2026-11-18");
        insertTodo(me, b, "2026-11-20"); // 오늘은 밀린 복습이 아니다

        list(me, "?from=2026-12-01&to=2026-12-07")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.overdue", hasSize(2)))
                .andExpect(jsonPath("$.overdue[0].id").value(old))
                .andExpect(jsonPath("$.overdue[0].planDate").value("2026-10-01"))
                .andExpect(jsonPath("$.overdue[0].status").value("TODO"))
                .andExpect(jsonPath("$.overdue[0].concept.id").value(a))
                .andExpect(jsonPath("$.overdue[1].id").value(yesterdayB));
    }

    // ── AC3 잘못된 조회 ──

    @Test
    void AC3_날짜_형식이_틀리거나_한쪽만_주면_400이고_details에_필드를_담는다() throws Exception {
        long me = createUser("f15ac3");

        list(me, "?from=2026/11/20&to=2026-11-26")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.from").exists());
        list(me, "?from=2026-11-20&to=2026-02-30")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.to").exists());
        list(me, "?from=2026-11-20")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.to").exists());
        list(me, "?to=2026-11-26")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.from").exists());
    }

    @Test
    void AC3_from이_to보다_뒤이거나_31일을_넘으면_400이고_31일은_된다() throws Exception {
        long me = createUser("f15ac3r");

        list(me, "?from=2026-11-27&to=2026-11-26")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.to").exists());
        list(me, "?from=2026-11-01&to=2026-12-02")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.to").exists());
        list(me, "?from=2026-11-01&to=2026-12-01").andExpect(status().isOk());
        list(me, "?from=2026-11-20&to=2026-11-20").andExpect(status().isOk());
    }

    @Test
    void AC3_토큰이_없으면_401() throws Exception {
        mockMvc.perform(get("/api/review-plans"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    // ── AC4 직접 추가 ──

    @Test
    void AC4_planDate를_생략하면_내일_MANUAL_TODO로_넣고_항목을_돌려준다() throws Exception {
        long me = createUser("f15ac4");
        long a = insertConcept("f15-test-A", "PUBLISHED");

        add(me, "{\"conceptId\": %d}".formatted(a))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.planDate").value("2026-11-21"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.reason").value("MANUAL"))
                .andExpect(jsonPath("$.doneAt").value(nullValue()))
                .andExpect(jsonPath("$.concept.id").value(a))
                .andExpect(jsonPath("$.concept.name").value("f15-test-A"));

        long id =
                jdbc.queryForObject(
                        "SELECT id FROM review_plans WHERE user_id = ?", Long.class, me);
        assertThat(row(id)).isEqualTo(row("2026-11-21", "TODO", "MANUAL", null));
    }

    @Test
    void AC4_planDate를_주면_그_날짜로_넣고_오늘과_365일_뒤도_된다() throws Exception {
        long me = createUser("f15ac4d");
        long a = insertConcept("f15-test-A", "PUBLISHED");
        long b = insertConcept("f15-test-B", "PUBLISHED");
        long c = insertConcept("f15-test-C", "PUBLISHED");

        add(me, "{\"conceptId\": %d, \"planDate\": \"2026-11-25\"}".formatted(a))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planDate").value("2026-11-25"));
        add(me, "{\"conceptId\": %d, \"planDate\": \"2026-11-20\"}".formatted(b))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planDate").value("2026-11-20"));
        add(me, "{\"conceptId\": %d, \"planDate\": \"2027-11-20\"}".formatted(c))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planDate").value("2027-11-20"));
        assertThat(countPlans(me)).isEqualTo(3);
    }

    // ── AC5 잘못된 추가 ──

    @Test
    void AC5_conceptId가_없거나_0_이하거나_planDate가_범위_밖이면_400() throws Exception {
        long me = createUser("f15ac5");
        long a = insertConcept("f15-test-A", "PUBLISHED");

        add(me, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.conceptId").exists());
        add(me, "{\"conceptId\": 0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.conceptId").exists());
        add(me, "{\"conceptId\": %d, \"planDate\": \"2026-11-19\"}".formatted(a))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.planDate").exists());
        add(me, "{\"conceptId\": %d, \"planDate\": \"2027-11-21\"}".formatted(a))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.planDate").exists());
        assertThat(countPlans(me)).isZero();
    }

    @Test
    void AC5_없는_카드와_PUBLISHED가_아닌_카드는_CONCEPT_404() throws Exception {
        long me = createUser("f15ac5n");
        long hidden = insertConcept("f15-test-숨김", "HIDDEN");
        long draft = insertConcept("f15-test-초안", "DRAFT");

        for (long id : new long[] {hidden, draft, 999_999_999L}) {
            add(me, "{\"conceptId\": %d}".formatted(id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("CONCEPT_404"));
        }
        assertThat(countPlans(me)).isZero();
    }

    @Test
    void AC5_TODO가_날짜와_상관없이_있거나_같은_날짜의_DONE이_있으면_409() throws Exception {
        long me = createUser("f15ac5c");
        long todoElsewhere = insertConcept("f15-test-다른 날 TODO", "PUBLISHED");
        long todoPast = insertConcept("f15-test-지난 TODO", "PUBLISHED");
        long doneSameDay = insertConcept("f15-test-같은 날 DONE", "PUBLISHED");
        insertTodo(me, todoElsewhere, "2026-12-01");
        insertTodo(me, todoPast, "2026-11-01");
        insertPlan(me, doneSameDay, "2026-11-21", "DONE", "WRONG", "2026-11-19 10:00:00");

        for (long id : new long[] {todoElsewhere, todoPast, doneSameDay}) {
            add(me, "{\"conceptId\": %d}".formatted(id))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("REVIEW_PLAN_409"))
                    .andExpect(jsonPath("$.message").value("이미 복습 계획에 있는 카드입니다."));
        }
        assertThat(countPlans(me)).isEqualTo(3);
    }

    @Test
    void AC5_다른_날짜의_DONE만_있으면_넣고_다른_회원의_TODO는_보지_않는다() throws Exception {
        long me = createUser("f15ac5d");
        long other = createUser("f15ac5o");
        long a = insertConcept("f15-test-A", "PUBLISHED");
        insertPlan(me, a, "2026-11-18", "DONE", "WRONG", "2026-11-18 10:00:00");
        insertTodo(other, a, "2026-11-21");

        add(me, "{\"conceptId\": %d}".formatted(a)).andExpect(status().isCreated());
        assertThat(countPlans(me)).isEqualTo(2);
    }

    @Test
    void AC5_토큰이_없으면_401() throws Exception {
        mockMvc.perform(
                        post("/api/review-plans")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"conceptId\": 1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    // ── AC6 완료·되돌림 ──

    @Test
    void AC6_TODO를_DONE으로_바꾸면_doneAt이_지금이고_되돌리면_null이다() throws Exception {
        long me = createUser("f15ac6");
        long a = insertConcept("f15-test-A", "PUBLISHED");
        long plan = insertPlan(me, a, "2026-11-19", "TODO", "WISH", null);

        changeStatus(me, plan, "{\"status\": \"DONE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(plan))
                .andExpect(jsonPath("$.planDate").value("2026-11-19"))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.reason").value("WISH"))
                .andExpect(jsonPath("$.doneAt").value("2026-11-20T10:00:00+09:00"))
                .andExpect(jsonPath("$.concept.id").value(a));
        assertThat(row(plan)).isEqualTo(row("2026-11-19", "DONE", "WISH", "2026-11-20 10:00:00"));

        changeStatus(me, plan, "{\"status\": \"TODO\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.doneAt").value(nullValue()));
        assertThat(row(plan)).isEqualTo(row("2026-11-19", "TODO", "WISH", null));
    }

    @Test
    void AC6_이미_같은_상태면_바꾸지_않고_doneAt을_유지한다() throws Exception {
        long me = createUser("f15ac6s");
        long a = insertConcept("f15-test-A", "PUBLISHED");
        long b = insertConcept("f15-test-B", "PUBLISHED");
        long done = insertPlan(me, a, "2026-11-18", "DONE", "WRONG", "2026-11-18 21:30:00");
        long todo = insertTodo(me, b, "2026-11-22");

        changeStatus(me, done, "{\"status\": \"DONE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doneAt").value("2026-11-18T21:30:00+09:00"));
        changeStatus(me, todo, "{\"status\": \"TODO\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TODO"));
        assertThat(row(done)).isEqualTo(row("2026-11-18", "DONE", "WRONG", "2026-11-18 21:30:00"));
    }

    @Test
    void AC6_DONE을_되돌릴_때_그_카드의_다른_TODO가_있으면_409() throws Exception {
        long me = createUser("f15ac6c");
        long a = insertConcept("f15-test-A", "PUBLISHED");
        long done = insertPlan(me, a, "2026-11-18", "DONE", "WRONG", "2026-11-18 21:30:00");
        insertTodo(me, a, "2026-11-25");

        changeStatus(me, done, "{\"status\": \"TODO\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REVIEW_PLAN_409"));
        assertThat(row(done)).isEqualTo(row("2026-11-18", "DONE", "WRONG", "2026-11-18 21:30:00"));
    }

    @Test
    void AC6_status가_없거나_TODO_DONE이_아니면_400() throws Exception {
        long me = createUser("f15ac6b");
        long a = insertConcept("f15-test-A", "PUBLISHED");
        long plan = insertTodo(me, a, "2026-11-21");

        changeStatus(me, plan, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        changeStatus(me, plan, "{\"status\": \"SKIPPED\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        assertThat(row(plan)).isEqualTo(row("2026-11-21", "TODO", "WRONG", null));
    }

    // ── AC7 삭제·남의 계획 ──

    @Test
    void AC7_내_계획을_지우면_204이고_다시_조회하면_없다() throws Exception {
        long me = createUser("f15ac7");
        long a = insertConcept("f15-test-A", "PUBLISHED");
        long plan = insertTodo(me, a, "2026-11-21");

        remove(me, plan).andExpect(status().isNoContent());

        assertThat(countPlans(me)).isZero();
        list(me, "").andExpect(jsonPath("$.items", hasSize(0)));
        remove(me, plan)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REVIEW_PLAN_404"));
    }

    @Test
    void AC7_없는_계획_남의_계획_비공개_카드의_계획은_REVIEW_PLAN_404이고_그대로_둔다() throws Exception {
        long me = createUser("f15ac7n");
        long other = createUser("f15ac7o");
        long a = insertConcept("f15-test-A", "PUBLISHED");
        long hidden = insertConcept("f15-test-숨김", "HIDDEN");
        long othersPlan = insertTodo(other, a, "2026-11-21");
        long hiddenPlan = insertTodo(me, hidden, "2026-11-21");

        for (long id : new long[] {othersPlan, hiddenPlan, 999_999_999L}) {
            changeStatus(me, id, "{\"status\": \"DONE\"}")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("REVIEW_PLAN_404"))
                    .andExpect(jsonPath("$.message").value("복습 계획을 찾을 수 없습니다."));
            remove(me, id)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("REVIEW_PLAN_404"));
        }
        assertThat(row(othersPlan)).isEqualTo(row("2026-11-21", "TODO", "WRONG", null));
        assertThat(row(hiddenPlan)).isEqualTo(row("2026-11-21", "TODO", "WRONG", null));
    }

    @Test
    void AC7_토큰이_없으면_401() throws Exception {
        mockMvc.perform(
                        patch("/api/review-plans/{id}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\": \"DONE\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
        mockMvc.perform(delete("/api/review-plans/{id}", 1))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }
}
