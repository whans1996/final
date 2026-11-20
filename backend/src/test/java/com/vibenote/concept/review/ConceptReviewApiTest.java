package com.vibenote.concept.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.security.JwtProperties;
import com.vibenote.common.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * F-11 카드 리뷰 (docs/specs/F-11-concept-review.md AC1~AC12). 시드 카드에 기대지 않도록 고유 카테고리({@code f11-*})의
 * 카드·회원·리뷰를 직접 넣고 롤백한다.
 */
@Transactional
class ConceptReviewApiTest extends IntegrationTestBase {

    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired JwtProperties jwtProperties;

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final String REVIEWS = "/api/concepts/{id}/reviews";
    private static final String ME = "/api/concepts/{id}/reviews/me";

    // ── 데이터 준비 ──

    private long insertConcept(String category, String status) {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, status, source)"
                        + " VALUES (?, ?, 'JUNIOR', '요약', ?, 'AI')",
                category + " 카드 " + System.nanoTime(),
                category,
                status);
        return lastId();
    }

    private long published(String category) {
        return insertConcept(category, "PUBLISHED");
    }

    private long insertUser(String name) {
        jdbc.update(
                "INSERT INTO users (email, password_hash, nickname) VALUES (?, 'hash', ?)",
                name + "@f11.test",
                name);
        return lastId();
    }

    private void withdraw(long userId) {
        jdbc.update(
                "UPDATE users SET status = 'WITHDRAWN', withdrawn_at = NOW() WHERE id = ?", userId);
    }

    private long insertReview(
            long conceptId, long userId, int rating, String content, LocalDateTime createdAt) {
        jdbc.update(
                "INSERT INTO concept_reviews (concept_id, user_id, rating, content, created_at)"
                        + " VALUES (?, ?, ?, ?, ?)",
                conceptId,
                userId,
                rating,
                content,
                createdAt);
        return lastId();
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

    private String body(Integer rating, String content) throws Exception {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        if (rating != null) map.put("rating", rating);
        if (content != null) map.put("content", content);
        return objectMapper.writeValueAsString(map);
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String authorization)
            throws Exception {
        if (authorization != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return mockMvc.perform(request);
    }

    private ResultActions list(long conceptId, int page, int size) throws Exception {
        return mockMvc.perform(
                get(REVIEWS, conceptId)
                        .param("page", Integer.toString(page))
                        .param("size", Integer.toString(size)));
    }

    private ResultActions create(long conceptId, String authorization, String json)
            throws Exception {
        return perform(
                post(REVIEWS, conceptId).contentType(MediaType.APPLICATION_JSON).content(json),
                authorization);
    }

    private ResultActions update(long conceptId, String authorization, String json)
            throws Exception {
        return perform(
                put(ME, conceptId).contentType(MediaType.APPLICATION_JSON).content(json),
                authorization);
    }

    private ResultActions remove(long conceptId, String authorization) throws Exception {
        return perform(delete(ME, conceptId), authorization);
    }

    private ResultActions mine(long conceptId, String authorization) throws Exception {
        return perform(get(ME, conceptId), authorization);
    }

    private BigDecimal avgRating(long conceptId) {
        return jdbc.queryForObject(
                "SELECT avg_rating FROM concepts WHERE id = ?", BigDecimal.class, conceptId);
    }

    private int reviewCount(long conceptId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM concept_reviews WHERE concept_id = ?",
                Integer.class,
                conceptId);
    }

    private int reviewActivities(long userId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM activities WHERE user_id = ? AND type = 'REVIEW'",
                Integer.class,
                userId);
    }

    private Map<String, Object> reviewRow(long reviewId) {
        return jdbc.queryForMap("SELECT * FROM concept_reviews WHERE id = ?", reviewId);
    }

    private static List<Integer> ints(List<Long> ids) {
        return ids.stream().map(Long::intValue).toList();
    }

    // ── AC1 ──

    @Test
    void AC1_공개_카드의_리뷰를_비로그인으로_최신순_페이지_조회한다() throws Exception {
        long c = published("f11-ac1");
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            long u = insertUser("f11ac1u" + i);
            // 두 개씩 같은 created_at → id DESC로 순서를 가른다
            ids.add(insertReview(c, u, 1 + i % 5, "리뷰" + i, BASE.plusMinutes(i / 2)));
        }
        List<Long> expected = new ArrayList<>(ids);
        Collections.reverse(expected);
        long newest = expected.get(0);
        long newestAuthor =
                jdbc.queryForObject(
                        "SELECT user_id FROM concept_reviews WHERE id = ?", Long.class, newest);

        list(c, 1, 10)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.total").value(12))
                .andExpect(
                        jsonPath("$.items[*].id")
                                .value(contains(ints(expected.subList(0, 10)).toArray())))
                .andExpect(jsonPath("$.items[0].rating").value(2))
                .andExpect(jsonPath("$.items[0].content").value("리뷰11"))
                .andExpect(jsonPath("$.items[0].authorId").value(newestAuthor))
                .andExpect(jsonPath("$.items[0].authorNickname").value("f11ac1u11"))
                .andExpect(jsonPath("$.items[0].createdAt").value("2026-01-01T00:05:00+09:00"));

        list(c, 2, 10)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(12))
                .andExpect(
                        jsonPath("$.items[*].id")
                                .value(contains(ints(expected.subList(10, 12)).toArray())));

        list(c, 3, 10)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(empty()))
                .andExpect(jsonPath("$.total").value(12));

        long empty = published("f11-ac1-empty");
        list(empty, 1, 10)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(empty()))
                .andExpect(jsonPath("$.total").value(0));
    }

    // ── AC2 ──

    @Test
    void AC2_탈퇴한_작성자의_리뷰는_익명으로_목록과_total에_포함된다() throws Exception {
        long c = published("f11-ac2");
        long gone = insertUser("f11ac2gone");
        long active = insertUser("f11ac2old");
        insertReview(c, gone, 3, "탈퇴 전 리뷰", BASE);
        insertReview(c, active, 5, "활성 리뷰", BASE.plusMinutes(1));
        withdraw(gone);
        jdbc.update("UPDATE users SET nickname = 'f11ac2new' WHERE id = ?", active);

        list(c, 1, 10)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].authorId").value(active))
                .andExpect(jsonPath("$.items[0].authorNickname").value("f11ac2new"))
                .andExpect(jsonPath("$.items[1].authorId").value(nullValue()))
                .andExpect(jsonPath("$.items[1].authorNickname").value("탈퇴한 사용자"))
                .andExpect(jsonPath("$.items[1].content").value("탈퇴 전 리뷰"));
    }

    // ── AC3 ──

    @Test
    void AC3_리뷰를_쓰면_201과_평점_갱신과_REVIEW_활동이_남는다() throws Exception {
        long c = published("f11-ac3");
        insertReview(c, insertUser("f11ac3a"), 5, "a", BASE);
        insertReview(c, insertUser("f11ac3b"), 5, "b", BASE);
        long me = insertUser("f11ac3me");

        String response =
                create(c, bearer(me), body(4, "  예제가 명확했어요  "))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.review.id").value(notNullValue()))
                        .andExpect(jsonPath("$.review.rating").value(4))
                        .andExpect(jsonPath("$.review.content").value("예제가 명확했어요"))
                        .andExpect(jsonPath("$.review.authorId").value(me))
                        .andExpect(jsonPath("$.review.authorNickname").value("f11ac3me"))
                        .andExpect(jsonPath("$.review.createdAt").value(notNullValue()))
                        .andExpect(jsonPath("$.avgRating").value(4.7))
                        .andExpect(jsonPath("$.reviewCount").value(3))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long reviewId = objectMapper.readTree(response).path("review").path("id").asLong();

        assertThat(avgRating(c)).isEqualByComparingTo("4.7");
        assertThat(reviewRow(reviewId).get("content")).isEqualTo("예제가 명확했어요");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM activities WHERE user_id = ? AND type ="
                                        + " 'REVIEW' AND target_id = ?",
                                Integer.class,
                                me,
                                reviewId))
                .isEqualTo(1);
    }

    // ── AC4 ──

    @Test
    void AC4_평점은_소수_1자리로_반올림되고_모두_지우면_0이다() throws Exception {
        long c = published("f11-ac4");
        long a = insertUser("f11ac4a");
        long b = insertUser("f11ac4b");
        long d = insertUser("f11ac4d");

        create(c, bearer(a), body(4, "a")).andExpect(status().isCreated());
        create(c, bearer(b), body(4, "b")).andExpect(status().isCreated());
        create(c, bearer(d), body(5, "d"))
                .andExpect(jsonPath("$.avgRating").value(4.3))
                .andExpect(jsonPath("$.reviewCount").value(3));
        assertThat(avgRating(c)).isEqualByComparingTo("4.3");

        remove(c, bearer(a))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avgRating").value(4.5))
                .andExpect(jsonPath("$.reviewCount").value(2));
        assertThat(avgRating(c)).isEqualByComparingTo("4.5");

        remove(c, bearer(b)).andExpect(status().isOk());
        remove(c, bearer(d))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avgRating").value(0.0))
                .andExpect(jsonPath("$.reviewCount").value(0));
        assertThat(avgRating(c)).isEqualByComparingTo("0.0");
    }

    // ── AC5 ──

    static List<Object[]> invalidBodies() {
        return List.of(
                new Object[] {null, "좋아요", "rating"},
                new Object[] {0, "좋아요", "rating"},
                new Object[] {6, "좋아요", "rating"},
                new Object[] {3, null, "content"},
                new Object[] {3, "   ", "content"},
                new Object[] {3, "  " + "가".repeat(201) + "  ", "content"});
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void AC5_별점이나_내용이_규칙을_어기면_400이고_아무것도_바뀌지_않는다(Integer rating, String content, String field)
            throws Exception {
        long c = published("f11-ac5");
        long me = insertUser("f11ac5me");

        create(c, bearer(me), body(rating, content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details." + field).value(notNullValue()));

        assertThat(reviewCount(c)).isZero();
        assertThat(avgRating(c)).isEqualByComparingTo("0.0");
        assertThat(reviewActivities(me)).isZero();
    }

    @Test
    void AC5_공백을_뺀_내용이_정확히_200자면_성공한다() throws Exception {
        long c = published("f11-ac5-200");
        long me = insertUser("f11ac5ok");
        String content = "가".repeat(200);

        create(c, bearer(me), body(1, " " + content + " "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.review.content").value(content));
    }

    // ── AC6 ──

    @Test
    void AC6_같은_카드에_두_번째_리뷰는_409이고_기존_리뷰는_그대로다() throws Exception {
        long c = published("f11-ac6");
        long me = insertUser("f11ac6me");
        long first = insertReview(c, me, 2, "처음 리뷰", BASE);

        create(c, bearer(me), body(5, "두 번째"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCEPT_409"))
                .andExpect(jsonPath("$.message").value("이미 이 카드에 리뷰를 남겼습니다."));

        assertThat(reviewCount(c)).isEqualTo(1);
        assertThat(reviewRow(first).get("content")).isEqualTo("처음 리뷰");
        assertThat(reviewActivities(me)).isZero();
    }

    // ── AC7 ──

    @Test
    void AC7_내_리뷰는_200이고_없으면_204다() throws Exception {
        long c = published("f11-ac7");
        long me = insertUser("f11ac7me");
        long other = insertUser("f11ac7other");
        insertReview(c, other, 1, "남의 리뷰", BASE);

        long id = insertReview(c, me, 4, "내 리뷰", BASE.plusMinutes(1));
        mine(c, bearer(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.content").value("내 리뷰"))
                .andExpect(jsonPath("$.authorId").value(me))
                .andExpect(jsonPath("$.authorNickname").value("f11ac7me"))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T00:01:00+09:00"));

        // 테스트 트랜잭션 하나에서 같은 쿼리를 다시 부르면 MyBatis 1차 캐시가 답하므로 리뷰 없는 회원은 따로 둔다
        long none = insertUser("f11ac7none");
        mine(c, bearer(none)).andExpect(status().isNoContent()).andExpect(content().string(""));
    }

    // ── AC8 ──

    @Test
    void AC8_내_리뷰를_고치면_평점이_다시_계산되고_id와_작성일은_그대로다() throws Exception {
        long c = published("f11-ac8");
        long me = insertUser("f11ac8me");
        insertReview(c, insertUser("f11ac8other"), 4, "남의 리뷰", BASE.plusMinutes(5));
        long id = insertReview(c, me, 2, "처음엔 어려웠음", BASE);

        update(c, bearer(me), body(5, " 다시 읽으니 이해됨 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.review.id").value(id))
                .andExpect(jsonPath("$.review.rating").value(5))
                .andExpect(jsonPath("$.review.content").value("다시 읽으니 이해됨"))
                .andExpect(jsonPath("$.review.createdAt").value("2026-01-01T00:00:00+09:00"))
                .andExpect(jsonPath("$.avgRating").value(4.5))
                .andExpect(jsonPath("$.reviewCount").value(2));

        assertThat(avgRating(c)).isEqualByComparingTo("4.5");
        assertThat(reviewRow(id).get("rating")).isEqualTo(5);
        assertThat(reviewActivities(me)).isZero();

        update(c, bearer(me), body(0, "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.rating").value(notNullValue()));
        update(c, bearer(me), body(3, "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.content").value(notNullValue()));
    }

    // ── AC9 ──

    @Test
    void AC9_내_리뷰를_지우면_평점이_다시_계산되고_다시_쓸_수_있다() throws Exception {
        long c = published("f11-ac9");
        long me = insertUser("f11ac9me");
        insertReview(c, insertUser("f11ac9other"), 3, "남의 리뷰", BASE);
        long id = insertReview(c, me, 5, "내 리뷰", BASE.plusMinutes(1));

        remove(c, bearer(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avgRating").value(3.0))
                .andExpect(jsonPath("$.reviewCount").value(1));

        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM concept_reviews WHERE id = ?",
                                Integer.class,
                                id))
                .isZero();
        assertThat(avgRating(c)).isEqualByComparingTo("3.0");

        create(c, bearer(me), body(4, "다시 씀")).andExpect(status().isCreated());
    }

    // ── AC10 ──

    @Test
    void AC10_내_리뷰가_없으면_수정_삭제는_404이고_남의_리뷰는_그대로다() throws Exception {
        long c = published("f11-ac10");
        long me = insertUser("f11ac10me");
        long other = insertUser("f11ac10other");
        long otherReview = insertReview(c, other, 3, "남의 리뷰", BASE);

        update(c, bearer(me), body(5, "바꿔치기"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONCEPT_001"))
                .andExpect(jsonPath("$.message").value("작성한 리뷰가 없습니다."));
        remove(c, bearer(me))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONCEPT_001"));

        Map<String, Object> row = reviewRow(otherReview);
        assertThat(row.get("rating")).isEqualTo(3);
        assertThat(row.get("content")).isEqualTo("남의 리뷰");
    }

    // ── AC11 ──

    @Test
    void AC11_비공개이거나_없는_카드면_모든_리뷰_API가_404다() throws Exception {
        long me = insertUser("f11ac11me");
        long draft = insertConcept("f11-ac11", "DRAFT");
        long hidden = insertConcept("f11-ac11", "HIDDEN");
        long hiddenReview = insertReview(hidden, me, 2, "숨기기 전 리뷰", BASE);

        for (long id : new long[] {draft, hidden, 999_999L}) {
            String auth = bearer(me);
            for (ResultActions result :
                    List.of(
                            list(id, 1, 10),
                            mine(id, auth),
                            create(id, auth, body(5, "리뷰")),
                            update(id, auth, body(5, "리뷰")),
                            remove(id, auth))) {
                result.andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("CONCEPT_404"));
            }
        }

        Map<String, Object> row = reviewRow(hiddenReview);
        assertThat(row.get("rating")).isEqualTo(2);
        assertThat(row.get("content")).isEqualTo("숨기기 전 리뷰");
    }

    // ── AC12 ──

    @Test
    void AC12_토큰이_없거나_깨지면_목록만_200이고_나머지는_401이다() throws Exception {
        long c = published("f11-ac12");
        long me = insertUser("f11ac12me");
        insertReview(c, me, 4, "내 리뷰", BASE);

        for (String auth : new String[] {null, "Bearer not-a-jwt", expiredBearer(me)}) {
            for (ResultActions result :
                    List.of(
                            mine(c, auth),
                            create(c, auth, body(5, "리뷰")),
                            update(c, auth, body(5, "리뷰")),
                            remove(c, auth))) {
                result.andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value("AUTH_401"));
            }
            perform(get(REVIEWS, c), auth)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(1));
        }
        assertThat(reviewCount(c)).isEqualTo(1);
    }
}
