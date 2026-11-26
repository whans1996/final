package com.vibenote.concept.wish;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.security.JwtProperties;
import com.vibenote.common.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * F-12 카드 찜 (docs/specs/F-12-concept-wish.md AC1~AC11). 시드 카드에 기대지 않도록 고유 카테고리({@code f12-test})의
 * 카드·회원을 직접 넣고 롤백한다. "내일"은 앱의 {@code Clock}(Asia/Seoul) 기준이다.
 */
@Transactional
class ConceptWishApiTest extends IntegrationTestBase {

    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired JwtProperties jwtProperties;
    @Autowired Clock clock;

    private static final String WISH = "/api/concepts/{id}/wish";
    private static final String WISHES = "/api/users/{id}/wishes";
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 1, 0, 0);

    // ── 데이터 준비 ──

    private long insertConcept(String status, int wishCount) {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, status, source,"
                        + " wish_count) VALUES (?, 'f12-test', 'JUNIOR', '요약', ?, 'AI', ?)",
                "f12-test 카드 " + System.nanoTime(),
                status,
                wishCount);
        return lastId();
    }

    private long published() {
        return insertConcept("PUBLISHED", 0);
    }

    private long insertUser(String name) {
        jdbc.update(
                "INSERT INTO users (email, password_hash, nickname) VALUES (?, 'hash', ?)",
                name + "@f12.test",
                name);
        return lastId();
    }

    /** 찜 행만 넣고 wish_count는 실제 행 수로 맞춘다. */
    private void insertWish(long userId, long conceptId, LocalDateTime createdAt) {
        jdbc.update(
                "INSERT INTO concept_wishes (user_id, concept_id, created_at) VALUES (?, ?, ?)",
                userId,
                conceptId,
                createdAt);
        jdbc.update(
                "UPDATE concepts SET wish_count = (SELECT COUNT(*) FROM concept_wishes WHERE"
                        + " concept_id = ?) WHERE id = ?",
                conceptId,
                conceptId);
    }

    /** 다른 회원 n명이 찜한 카드 (wish_count = n). */
    private long conceptWishedBy(int n) {
        long c = published();
        for (int i = 0; i < n; i++) {
            insertWish(insertUser("f12o" + i + "x" + System.nanoTime() % 100000), c, BASE);
        }
        return c;
    }

    private long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private String bearer(long userId) {
        return "Bearer " + tokenProvider.createAccessToken(userId, "USER");
    }

    private String expiredBearer(long userId) {
        Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
        return "Bearer "
                + Jwts.builder()
                        .subject(Long.toString(userId))
                        .claim("role", "USER")
                        .issuedAt(Date.from(past))
                        .expiration(Date.from(past.plus(1, ChronoUnit.HOURS)))
                        .signWith(
                                Keys.hmacShaKeyFor(
                                        jwtProperties.secret().getBytes(StandardCharsets.UTF_8)))
                        .compact();
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String authorization)
            throws Exception {
        if (authorization != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return mockMvc.perform(request);
    }

    private ResultActions wish(long conceptId, long userId) throws Exception {
        return perform(post(WISH, conceptId), bearer(userId));
    }

    private ResultActions unwish(long conceptId, long userId) throws Exception {
        return perform(delete(WISH, conceptId), bearer(userId));
    }

    private ResultActions wishes(long userId, int page, int size) throws Exception {
        return mockMvc.perform(
                get(WISHES, userId)
                        .param("page", Integer.toString(page))
                        .param("size", Integer.toString(size)));
    }

    private int wishRows(long userId, long conceptId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM concept_wishes WHERE user_id = ? AND concept_id = ?",
                Integer.class,
                userId,
                conceptId);
    }

    private int wishCount(long conceptId) {
        return jdbc.queryForObject(
                "SELECT wish_count FROM concepts WHERE id = ?", Integer.class, conceptId);
    }

    private int wishActivities(long userId, long conceptId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM activities WHERE user_id = ? AND type = 'WISH' AND"
                        + " target_id = ?",
                Integer.class,
                userId,
                conceptId);
    }

    private List<String> plans(long userId, long conceptId) {
        return jdbc.queryForList(
                "SELECT CONCAT(plan_date, ' ', status, ' ', reason) FROM review_plans WHERE"
                        + " user_id = ? AND concept_id = ? ORDER BY id",
                String.class,
                userId,
                conceptId);
    }

    private LocalDate tomorrow() {
        return LocalDate.now(clock).plusDays(1);
    }

    // ── AC1 ──

    @Test
    void AC1_찜하면_찜_수가_늘고_내일_복습_계획에_들어간다() throws Exception {
        long me = insertUser("f12ac1");
        long c = conceptWishedBy(2);

        wish(c, me)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wished").value(true))
                .andExpect(jsonPath("$.wishCount").value(3))
                .andExpect(jsonPath("$.reviewPlanDate").value(tomorrow().toString()));

        assertThat(wishRows(me, c)).isEqualTo(1);
        assertThat(wishCount(c)).isEqualTo(3);
        assertThat(wishActivities(me, c)).isEqualTo(1);
        assertThat(plans(me, c)).containsExactly(tomorrow() + " TODO WISH");
    }

    // ── AC2 ──

    @Test
    void AC2_이미_찜한_카드를_다시_찜하면_아무것도_바뀌지_않는다() throws Exception {
        long me = insertUser("f12ac2");
        long c = conceptWishedBy(2);
        wish(c, me).andExpect(status().isOk());

        wish(c, me)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wished").value(true))
                .andExpect(jsonPath("$.wishCount").value(3))
                .andExpect(jsonPath("$.reviewPlanDate").value(nullValue()));

        assertThat(wishRows(me, c)).isEqualTo(1);
        assertThat(wishCount(c)).isEqualTo(3);
        assertThat(wishActivities(me, c)).isEqualTo(1);
        assertThat(plans(me, c)).hasSize(1);
    }

    // ── AC3 ──

    @Test
    void AC3_TODO_복습_계획이_있으면_찜은_되고_계획은_그대로다() throws Exception {
        long me = insertUser("f12ac3a");
        long c = published();
        jdbc.update(
                "INSERT INTO review_plans (user_id, concept_id, plan_date, status, reason)"
                        + " VALUES (?, ?, ?, 'TODO', 'WRONG')",
                me,
                c,
                LocalDate.now(clock).plusDays(3));

        wish(c, me)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wished").value(true))
                .andExpect(jsonPath("$.wishCount").value(1))
                .andExpect(jsonPath("$.reviewPlanDate").value(nullValue()));

        assertThat(plans(me, c)).containsExactly(LocalDate.now(clock).plusDays(3) + " TODO WRONG");
        assertThat(wishActivities(me, c)).isEqualTo(1);
    }

    @Test
    void AC3_내일_날짜_계획이_이미_있으면_찜은_되고_계획은_그대로다() throws Exception {
        long me = insertUser("f12ac3b");
        long c = published();
        jdbc.update(
                "INSERT INTO review_plans (user_id, concept_id, plan_date, status, reason,"
                        + " done_at) VALUES (?, ?, ?, 'DONE', 'MANUAL', NOW())",
                me,
                c,
                tomorrow());

        wish(c, me)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wished").value(true))
                .andExpect(jsonPath("$.reviewPlanDate").value(nullValue()));

        assertThat(plans(me, c)).containsExactly(tomorrow() + " DONE MANUAL");
    }

    // ── AC4 ──

    @Test
    void AC4_찜을_해제하면_찜_수가_줄고_계획과_활동은_남는다() throws Exception {
        long me = insertUser("f12ac4a");
        long c = conceptWishedBy(2);
        wish(c, me).andExpect(jsonPath("$.wishCount").value(3));

        unwish(c, me)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wished").value(false))
                .andExpect(jsonPath("$.wishCount").value(2))
                .andExpect(jsonPath("$.reviewPlanDate").value(nullValue()));

        assertThat(wishRows(me, c)).isZero();
        assertThat(wishCount(c)).isEqualTo(2);
        assertThat(wishActivities(me, c)).isEqualTo(1);
        assertThat(plans(me, c)).containsExactly(tomorrow() + " TODO WISH");
    }

    @Test
    void AC4_찜하지_않은_카드를_해제해도_200이고_찜_수는_그대로다() throws Exception {
        long me = insertUser("f12ac4b");
        long c = conceptWishedBy(2);
        long zero = published();

        unwish(c, me)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wished").value(false))
                .andExpect(jsonPath("$.wishCount").value(2));
        unwish(zero, me)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wished").value(false))
                .andExpect(jsonPath("$.wishCount").value(0));

        assertThat(wishCount(c)).isEqualTo(2);
        assertThat(wishCount(zero)).isZero();
    }

    // ── AC5 ──

    @Test
    void AC5_해제_후_다시_찜하면_찜_수는_늘고_활동은_새로_생기지_않는다() throws Exception {
        long me = insertUser("f12ac5");
        long c = published();
        wish(c, me).andExpect(status().isOk());
        unwish(c, me).andExpect(status().isOk());
        // 내일 계획은 남아 있으므로 다시 찜해도 addWish가 넣지 않는다 (AC3 규칙)
        wish(c, me)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wished").value(true))
                .andExpect(jsonPath("$.wishCount").value(1))
                .andExpect(jsonPath("$.reviewPlanDate").value(nullValue()));

        assertThat(wishRows(me, c)).isEqualTo(1);
        assertThat(wishActivities(me, c)).isEqualTo(1);
        assertThat(plans(me, c)).hasSize(1);
    }

    @Test
    void AC5_해제_후_계획이_끝났으면_다시_찜할_때_계획을_다시_만든다() throws Exception {
        long me = insertUser("f12ac5b");
        long c = published();
        wish(c, me).andExpect(status().isOk());
        unwish(c, me).andExpect(status().isOk());
        // 남은 WISH 계획을 다른 날짜의 DONE으로 바꾸면 TODO도 내일 계획도 없다
        jdbc.update(
                "UPDATE review_plans SET status = 'DONE', done_at = NOW(), plan_date = ? WHERE"
                        + " user_id = ? AND concept_id = ?",
                LocalDate.now(clock),
                me,
                c);

        wish(c, me).andExpect(jsonPath("$.reviewPlanDate").value(tomorrow().toString()));

        assertThat(wishActivities(me, c)).isEqualTo(1);
        assertThat(plans(me, c)).hasSize(2);
    }

    // ── AC6 ──

    @Test
    void AC6_찜_수는_실제_찜_행_수와_같고_상세에도_그대로_보인다() throws Exception {
        long c = published();
        long u1 = insertUser("f12ac6a");
        long u2 = insertUser("f12ac6b");
        long u3 = insertUser("f12ac6c");
        wish(c, u1).andExpect(status().isOk());
        wish(c, u2).andExpect(status().isOk());
        wish(c, u3).andExpect(status().isOk());
        unwish(c, u2).andExpect(jsonPath("$.wishCount").value(2));

        int rows =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM concept_wishes WHERE concept_id = ?",
                        Integer.class,
                        c);
        assertThat(rows).isEqualTo(2);
        assertThat(wishCount(c)).isEqualTo(2);
        mockMvc.perform(get("/api/concepts/{id}", c))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wishCount").value(2));
    }

    // ── AC7 ──

    @Test
    void AC7_상세의_wished는_내가_찜했는지를_보여_준다() throws Exception {
        long me = insertUser("f12ac7");
        long c = published();
        long d = published();
        wish(c, me).andExpect(status().isOk());

        perform(get("/api/concepts/{id}", c), bearer(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wished").value(true));
        perform(get("/api/concepts/{id}", d), bearer(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wished").value(false));
        for (String auth : new String[] {null, "Bearer not-a-jwt", expiredBearer(me)}) {
            perform(get("/api/concepts/{id}", c), auth)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.wished").value(false));
        }
    }

    // ── AC8 ──

    @Test
    void AC8_찜_목록은_공개_카드만_찜한_시각_최신순으로_페이지로_받는다() throws Exception {
        long u = insertUser("f12ac8");
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            long c = published();
            ids.add(c);
            insertWish(u, c, BASE.plusMinutes(i));
        }
        jdbc.update("UPDATE concepts SET status = 'HIDDEN' WHERE id = ?", ids.get(14));
        jdbc.update("UPDATE concepts SET status = 'DRAFT' WHERE id = ?", ids.get(13));
        // 최신순 공개: ids[12], ids[11], ..., ids[0]
        List<Integer> expectedFirst = new ArrayList<>();
        for (int i = 12; i >= 1; i--) expectedFirst.add(ids.get(i).intValue());

        wishes(u, 1, 12)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.total").value(13))
                .andExpect(jsonPath("$.items", hasSize(12)))
                .andExpect(jsonPath("$.items[*].id", contains(expectedFirst.toArray())))
                .andExpect(jsonPath("$.items[0].name").exists())
                .andExpect(jsonPath("$.items[0].category").value("f12-test"))
                .andExpect(jsonPath("$.items[0].difficulty").value("JUNIOR"))
                .andExpect(jsonPath("$.items[0].summary").value("요약"))
                .andExpect(jsonPath("$.items[0].avgRating").value(0.0))
                .andExpect(jsonPath("$.items[0].wishCount").value(1));
        wishes(u, 2, 12)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(13))
                .andExpect(jsonPath("$.items[*].id", contains(ids.get(0).intValue())));
        wishes(u, 3, 12)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(13))
                .andExpect(jsonPath("$.items", empty()));
    }

    @Test
    void AC8_같은_시각에_찜했으면_카드_id_내림차순이다() throws Exception {
        long u = insertUser("f12ac8b");
        long a = published();
        long b = published();
        insertWish(u, a, BASE);
        insertWish(u, b, BASE);

        wishes(u, 1, 12).andExpect(jsonPath("$.items[*].id", contains((int) b, (int) a)));
    }

    @Test
    void AC8_찜이_없으면_빈_목록과_0건이다() throws Exception {
        long u = insertUser("f12ac8c");

        wishes(u, 1, 12)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", empty()))
                .andExpect(jsonPath("$.total").value(0));
    }

    // ── AC9 ──

    @Test
    void AC9_없거나_탈퇴한_회원의_찜_목록은_404() throws Exception {
        long gone = insertUser("f12ac9");
        jdbc.update(
                "UPDATE users SET status = 'WITHDRAWN', withdrawn_at = NOW() WHERE id = ?", gone);

        for (long id : new long[] {999999L, gone}) {
            wishes(id, 1, 12)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("USER_404"));
        }
    }

    // ── AC10 ──

    @Test
    void AC10_비공개_없는_카드에_찜_해제하면_404이고_남은_찜은_그대로다() throws Exception {
        long me = insertUser("f12ac10");
        long other = insertUser("f12ac10o");
        long draft = insertConcept("DRAFT", 0);
        long hidden = published();
        insertWish(other, hidden, BASE);
        jdbc.update("UPDATE concepts SET status = 'HIDDEN' WHERE id = ?", hidden);

        for (long id : new long[] {draft, hidden, 999999L}) {
            wish(id, me)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("CONCEPT_404"));
            perform(delete(WISH, id), bearer(other))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("CONCEPT_404"));
        }
        assertThat(wishRows(other, hidden)).isEqualTo(1);
        assertThat(wishCount(hidden)).isEqualTo(1);
        assertThat(wishRows(me, draft)).isZero();
    }

    // ── AC11 ──

    @Test
    void AC11_토큰이_없거나_깨지면_찜_해제는_401이고_공개_경로는_200() throws Exception {
        long me = insertUser("f12ac11");
        long c = published();

        for (String auth : new String[] {null, "Bearer not-a-jwt", expiredBearer(me)}) {
            perform(post(WISH, c), auth)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_401"));
            perform(delete(WISH, c), auth)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_401"));
            perform(get(WISHES, me), auth).andExpect(status().isOk());
            perform(get("/api/concepts/{id}", c), auth).andExpect(status().isOk());
        }
        assertThat(wishRows(me, c)).isZero();
    }

    @Test
    void AC11_탈퇴한_회원의_남은_토큰이면_찜_해제는_401이고_찜은_그대로다() throws Exception {
        long me = insertUser("f12ac11w");
        long wished = conceptWishedBy(0);
        insertWish(me, wished, BASE);
        long notWished = published();
        jdbc.update("UPDATE users SET status = 'WITHDRAWN', withdrawn_at = NOW() WHERE id = ?", me);

        wish(notWished, me)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
        unwish(wished, me)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));

        assertThat(wishRows(me, notWished)).isZero();
        assertThat(wishRows(me, wished)).isOne();
        assertThat(wishCount(notWished)).isZero();
        assertThat(wishActivities(me, notWished)).isZero();
    }

    @Test
    void AC11_찜_목록_공개_경로가_users_me를_열지_않는다() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
        mockMvc.perform(get("/api/users/me/wishes")).andExpect(status().isUnauthorized());
    }

    // ── #169 회귀: 찜 수 반정규화가 카드 수정일을 바꾸지 않는다 ──

    private LocalDateTime updatedAt(long conceptId) {
        return jdbc.queryForObject(
                "SELECT updated_at FROM concepts WHERE id = ?", LocalDateTime.class, conceptId);
    }

    @Test
    void 찜과_찜_해제는_카드의_updated_at을_바꾸지_않는다() throws Exception {
        long me = insertUser("f12i169");
        long c = published();
        jdbc.update("UPDATE concepts SET updated_at = ? WHERE id = ?", BASE, c);

        wish(c, me).andExpect(status().isOk());
        assertThat(wishCount(c)).isOne();
        assertThat(updatedAt(c)).isEqualTo(BASE);

        unwish(c, me).andExpect(status().isOk());
        assertThat(wishCount(c)).isZero();
        assertThat(updatedAt(c)).isEqualTo(BASE);
    }
}
