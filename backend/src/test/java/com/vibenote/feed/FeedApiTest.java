package com.vibenote.feed;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.security.JwtTokenProvider;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * F-04 활동 피드 (docs/specs/F-04-follow-feed.md AC6~AC8). 활동 행과 대상(분석·리뷰·찜·글·챌린지)을 직접 넣고 롤백한다. 활동 시각은
 * 직접 정해 정렬을 확인한다.
 */
@Transactional
class FeedApiTest extends IntegrationTestBase {

    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired SqlSessionTemplate sqlSession;

    private static final String FEED = "/api/feed";
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 1, 9, 0);

    // ── 데이터 준비 ──

    private long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long user(String prefix) {
        String name = prefix + System.nanoTime() % 1_000_000_000L;
        jdbc.update(
                "INSERT INTO users (email, password_hash, nickname) VALUES (?, 'hash', ?)",
                name + "@f04f.test",
                name);
        return lastId();
    }

    private String nickname(long userId) {
        return jdbc.queryForObject("SELECT nickname FROM users WHERE id = ?", String.class, userId);
    }

    private void follow(long follower, long following) {
        jdbc.update(
                "INSERT INTO follows (follower_id, following_id) VALUES (?, ?)",
                follower,
                following);
    }

    private long activity(long userId, String type, long targetId, LocalDateTime at) {
        jdbc.update(
                "INSERT INTO activities (user_id, type, target_id, created_at) VALUES (?, ?, ?, ?)",
                userId,
                type,
                targetId,
                at);
        return lastId();
    }

    private long analysis(
            long userId,
            String title,
            String projectName,
            boolean isPublic,
            String status,
            LocalDateTime deletedAt) {
        jdbc.update(
                "INSERT INTO analyses (user_id, source_type, project_name, title, level, status,"
                        + " is_public, deleted_at) VALUES (?, 'ZIP', ?, ?, 'JUNIOR', ?, ?, ?)",
                userId,
                projectName,
                title,
                status,
                isPublic,
                deletedAt);
        return lastId();
    }

    private long concept(String name, String status) {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, status, source)"
                        + " VALUES (?, 'f04-test', 'JUNIOR', '요약', ?, 'AI')",
                name + " " + System.nanoTime(),
                status);
        return lastId();
    }

    private String conceptName(long conceptId) {
        return jdbc.queryForObject(
                "SELECT name FROM concepts WHERE id = ?", String.class, conceptId);
    }

    private long review(long userId, long conceptId, int rating) {
        jdbc.update(
                "INSERT INTO concept_reviews (concept_id, user_id, rating, content) VALUES (?, ?,"
                        + " ?, '좋아요')",
                conceptId,
                userId,
                rating);
        return lastId();
    }

    private void wish(long userId, long conceptId) {
        jdbc.update(
                "INSERT INTO concept_wishes (user_id, concept_id) VALUES (?, ?)",
                userId,
                conceptId);
    }

    private void unwish(long userId, long conceptId) {
        jdbc.update(
                "DELETE FROM concept_wishes WHERE user_id = ? AND concept_id = ?",
                userId,
                conceptId);
    }

    private long post(long userId, String title) {
        jdbc.update(
                "INSERT INTO posts (user_id, board_type, title, content) VALUES (?, 'FREE', ?,"
                        + " '본문')",
                userId,
                title);
        return lastId();
    }

    private long challenge(long createdBy, String title) {
        jdbc.update(
                "INSERT INTO challenges (title, start_date, end_date, goal_type, goal_value,"
                    + " created_by) VALUES (?, '2026-01-01', '2026-01-31', 'REVIEW_COUNT', 3, ?)",
                title,
                createdBy);
        return lastId();
    }

    private String bearer(long userId) {
        return "Bearer " + tokenProvider.createAccessToken(userId, "USER");
    }

    private ResultActions feed(long me) throws Exception {
        return mockMvc.perform(get(FEED).header(HttpHeaders.AUTHORIZATION, bearer(me)));
    }

    // ── AC6 ──

    @Test
    void AC6_팔로우한_사람의_활동만_최신순으로_보인다() throws Exception {
        long me = user("f04fa");
        long b = user("f04fb");
        long c = user("f04fc");
        long d = user("f04fd");
        follow(me, b);
        follow(me, c);
        follow(d, me); // 나를 팔로우하는 사람의 활동은 보이지 않는다

        long card = concept("f04 리뷰 카드", "PUBLISHED");
        long bPost = post(b, "B의 글");
        long cReview = review(c, card, 4);
        long dPost = post(d, "D의 글");
        long myPost = post(me, "내 글");

        long a1 = activity(b, "POST", bPost, BASE);
        long a2 = activity(c, "REVIEW", cReview, BASE.plusHours(1));
        long a3 = activity(b, "WISH", card, BASE.plusHours(1)); // 같은 시각이면 활동 id 내림차순
        wish(b, card);
        activity(d, "POST", dPost, BASE.plusHours(2));
        activity(me, "POST", myPost, BASE.plusHours(3));

        feed(me).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(contains((int) a3, (int) a2, (int) a1)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[1].type").value("REVIEW"))
                .andExpect(jsonPath("$.items[1].createdAt").value("2026-01-01T10:00:00+09:00"))
                .andExpect(jsonPath("$.items[1].actor.id").value(c))
                .andExpect(jsonPath("$.items[1].actor.nickname").value(nickname(c)))
                .andExpect(jsonPath("$.items[1].target.id").value(card)) // 리뷰 id가 아니라 카드 id
                .andExpect(jsonPath("$.items[1].target.title").value(conceptName(card)))
                .andExpect(jsonPath("$.items[1].target.rating").value(4))
                .andExpect(jsonPath("$.items[0].type").value("WISH"))
                .andExpect(jsonPath("$.items[0].target.id").value(card))
                .andExpect(jsonPath("$.items[0].target.rating").value(nullValue()))
                .andExpect(jsonPath("$.items[2].type").value("POST"))
                .andExpect(jsonPath("$.items[2].target.id").value(bPost))
                .andExpect(jsonPath("$.items[2].target.title").value("B의 글"));
    }

    @Test
    void AC6_페이지_크기만큼_나눠서_준다() throws Exception {
        long me = user("f04fa");
        long b = user("f04fb");
        follow(me, b);
        long[] ids = new long[3];
        for (int i = 0; i < 3; i++) {
            ids[i] = activity(b, "POST", post(b, "글" + i), BASE.plusMinutes(i));
        }

        mockMvc.perform(
                        get(FEED)
                                .param("page", "2")
                                .param("size", "2")
                                .header(HttpHeaders.AUTHORIZATION, bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(contains((int) ids[0])))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void AC6_토큰이_없으면_401_AUTH_401() throws Exception {
        mockMvc.perform(get(FEED))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    @Test
    void AC6_page_size가_범위_밖이면_400_COMMON_400() throws Exception {
        long me = user("f04fa");
        mockMvc.perform(get(FEED).param("size", "0").header(HttpHeaders.AUTHORIZATION, bearer(me)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    // ── AC7 ──

    @Test
    void AC7_분석은_공개이고_삭제되지_않은_DONE만_보이고_제목이_없으면_프로젝트명이다() throws Exception {
        long me = user("f04fa");
        long b = user("f04fb");
        follow(me, b);

        long titled = analysis(b, "내 첫 분석", "proj-a", true, "DONE", null);
        long untitled = analysis(b, null, "proj-b", true, "DONE", null);
        long nameless = analysis(b, null, null, true, "DONE", null);
        long a1 = activity(b, "ANALYSIS", titled, BASE.plusMinutes(3));
        long a2 = activity(b, "ANALYSIS", untitled, BASE.plusMinutes(2));
        long a3 = activity(b, "ANALYSIS", nameless, BASE.plusMinutes(1));
        activity(b, "ANALYSIS", analysis(b, "비공개", null, false, "DONE", null), BASE);
        activity(b, "ANALYSIS", analysis(b, "삭제", null, true, "DONE", BASE), BASE);
        activity(b, "ANALYSIS", analysis(b, "진행 중", null, true, "EXPLAINING", null), BASE);
        activity(b, "ANALYSIS", titled + 100_000, BASE); // 없는 분석

        feed(me).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(contains((int) a1, (int) a2, (int) a3)))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[0].target.id").value(titled))
                .andExpect(jsonPath("$.items[0].target.title").value("내 첫 분석"))
                .andExpect(jsonPath("$.items[1].target.title").value("proj-b"))
                .andExpect(jsonPath("$.items[2].target.title").value("제목 없는 분석"));
    }

    @Test
    void AC7_리뷰는_지워졌거나_카드가_공개가_아니면_빠진다() throws Exception {
        long me = user("f04fa");
        long b = user("f04fb");
        follow(me, b);

        long shown = activity(b, "REVIEW", review(b, concept("공개", "PUBLISHED"), 5), BASE);
        activity(b, "REVIEW", review(b, concept("숨김", "HIDDEN"), 3), BASE);
        long gone = review(b, concept("삭제 리뷰", "PUBLISHED"), 2);
        activity(b, "REVIEW", gone, BASE);
        jdbc.update("DELETE FROM concept_reviews WHERE id = ?", gone);

        feed(me).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(contains((int) shown)))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void AC7_찜은_해제했거나_카드가_공개가_아니면_빠지고_다시_찜하면_원래_시각으로_보인다() throws Exception {
        long me = user("f04fa");
        long b = user("f04fb");
        long c = user("f04fc");
        follow(me, b);
        follow(me, c);

        long card = concept("찜 카드", "PUBLISHED");
        long hidden = concept("숨김 찜 카드", "DRAFT");
        long bWish = activity(b, "WISH", card, BASE);
        long cWish = activity(c, "WISH", card, BASE.plusMinutes(1));
        activity(b, "WISH", hidden, BASE);
        wish(b, card);
        wish(c, card);
        wish(b, hidden);
        unwish(c, card); // C의 찜만 풀렸다. B의 찜 행은 남아 있어도 C의 활동은 빠진다

        feed(me).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(contains((int) bWish)))
                .andExpect(jsonPath("$.total").value(1));

        wish(c, card);
        // 테스트 트랜잭션 하나에서 같은 SELECT를 다시 부르므로 MyBatis 세션 캐시를 비운다 (실제 요청은 요청마다 새 세션)
        sqlSession.clearCache();

        feed(me).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(contains((int) cWish, (int) bWish)))
                .andExpect(jsonPath("$.items[0].createdAt").value("2026-01-01T09:01:00+09:00"))
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void AC7_글이_지워지면_빠진다() throws Exception {
        long me = user("f04fa");
        long b = user("f04fb");
        follow(me, b);

        long shown = activity(b, "POST", post(b, "남은 글"), BASE);
        long gone = post(b, "지운 글");
        activity(b, "POST", gone, BASE);
        jdbc.update("DELETE FROM posts WHERE id = ?", gone);

        feed(me).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(contains((int) shown)))
                .andExpect(jsonPath("$.items[0].target.title").value("남은 글"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void AC7_챌린지가_지워지면_빠진다() throws Exception {
        long me = user("f04fa");
        long b = user("f04fb");
        follow(me, b);

        long ch = challenge(b, "리뷰 3개 쓰기");
        long join = activity(b, "CHALLENGE_JOIN", ch, BASE);
        long done = activity(b, "CHALLENGE_COMPLETE", ch, BASE.plusMinutes(1));
        long gone = challenge(b, "지운 챌린지");
        activity(b, "CHALLENGE_JOIN", gone, BASE);
        jdbc.update("DELETE FROM challenges WHERE id = ?", gone);

        feed(me).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id").value(contains((int) done, (int) join)))
                .andExpect(jsonPath("$.items[0].type").value("CHALLENGE_COMPLETE"))
                .andExpect(jsonPath("$.items[0].target.id").value(ch))
                .andExpect(jsonPath("$.items[0].target.title").value("리뷰 3개 쓰기"))
                .andExpect(jsonPath("$.total").value(2));
    }

    // ── AC8 ──

    @Test
    void AC8_팔로우한_사람이_없으면_빈_피드다() throws Exception {
        long me = user("f04fa");
        long b = user("f04fb");
        activity(b, "POST", post(b, "남의 글"), BASE);

        feed(me).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(empty()))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void AC8_팔로우해도_보일_활동이_없으면_빈_피드다() throws Exception {
        long me = user("f04fa");
        long b = user("f04fb");
        follow(me, b);
        long gone = post(b, "지운 글");
        activity(b, "POST", gone, BASE);
        jdbc.update("DELETE FROM posts WHERE id = ?", gone);

        feed(me).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(empty()))
                .andExpect(jsonPath("$.total").value(0));
    }
}
