package com.vibenote.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** F-17 게시글 작성·수정·삭제 (docs/specs/F-17-community-board.md AC6~AC11). */
class PostWriteApiTest extends PostApiTestBase {

    private long me;
    private long other;

    @BeforeEach
    void setUp() {
        me = insertUser("f17나");
        other = insertUser("f17남");
    }

    private ResultActions create(long userId, String json) throws Exception {
        return perform(
                post(POSTS).contentType(MediaType.APPLICATION_JSON).content(json), bearer(userId));
    }

    private ResultActions update(long userId, long postId, String json) throws Exception {
        return perform(
                put(POST, postId).contentType(MediaType.APPLICATION_JSON).content(json),
                bearer(userId));
    }

    private ResultActions remove(long userId, long postId) throws Exception {
        return perform(delete(POST, postId), bearer(userId));
    }

    private long createdId(ResultActions result) throws Exception {
        String json = result.andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private int postCount() {
        return count("SELECT COUNT(*) FROM posts");
    }

    private int postActivities(long userId) {
        return count("SELECT COUNT(*) FROM activities WHERE user_id = ? AND type = 'POST'", userId);
    }

    // ── AC6 작성 ──

    @Test
    void AC6_분석을_첨부해_글을_쓰면_201과_id를_받고_다듬은_값과_활동이_저장된다() throws Exception {
        long analysis = doneAnalysis(me);

        ResultActions result =
                create(
                                me,
                                body(
                                        "QNA",
                                        "  @Transactional이 안 먹어요  ",
                                        "\n같은 클래스 안에서 ...\n  ",
                                        analysis,
                                        null))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id").isNumber());
        long id = createdId(result);

        Map<String, Object> row = postRow(id);
        assertThat(row.get("user_id")).isEqualTo(me);
        assertThat(row.get("board_type")).isEqualTo("QNA");
        assertThat(row.get("title")).isEqualTo("@Transactional이 안 먹어요");
        assertThat(row.get("content")).isEqualTo("같은 클래스 안에서 ...");
        assertThat(row.get("analysis_id")).isEqualTo(analysis);
        assertThat(row.get("concept_id")).isNull();
        assertThat(row.get("view_count")).isEqualTo(0);
        assertThat(
                        count(
                                "SELECT COUNT(*) FROM activities WHERE user_id = ? AND type ="
                                        + " 'POST' AND target_id = ?",
                                me,
                                id))
                .isEqualTo(1);
    }

    @Test
    void AC6_공개_카드를_첨부하거나_첨부_없이_써도_201이다() throws Exception {
        long concept = insertConcept("PUBLISHED");

        long withConcept =
                createdId(
                        create(me, body("REVIEW", "카드 후기", "좋았다", null, concept))
                                .andExpect(status().isCreated()));
        long plain =
                createdId(
                        create(me, body("FREE", "잡담", "안녕하세요", null, null))
                                .andExpect(status().isCreated()));

        assertThat(postRow(withConcept).get("concept_id")).isEqualTo(concept);
        assertThat(postRow(plain).get("analysis_id")).isNull();
        assertThat(postActivities(me)).isEqualTo(2);
    }

    // ── AC7 검증 ──

    static Stream<Arguments> invalidBodies() {
        return Stream.of(
                Arguments.of(null, "제목", "본문", false, "boardType"),
                Arguments.of("NOTICE", "제목", "본문", false, "boardType"),
                Arguments.of("QNA", null, "본문", false, "title"),
                Arguments.of("QNA", "   ", "본문", false, "title"),
                Arguments.of("QNA", "가".repeat(101), "본문", false, "title"),
                Arguments.of("QNA", "제목", null, false, "content"),
                Arguments.of("QNA", "제목", " \n ", false, "content"),
                Arguments.of("QNA", "제목", "가".repeat(5001), false, "content"),
                Arguments.of("QNA", "제목", "본문", true, "conceptId"));
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void AC7_검증에_실패하면_400과_해당_필드를_받고_글과_활동이_생기지_않는다(
            String boardType, String title, String content, boolean both, String field)
            throws Exception {
        Long analysis = both ? doneAnalysis(me) : null;
        Long concept = both ? insertConcept("PUBLISHED") : null;
        int before = postCount();

        create(me, body(boardType, title, content, analysis, concept))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details." + field).isString());

        assertThat(postCount()).isEqualTo(before);
        assertThat(postActivities(me)).isZero();
    }

    @Test
    void AC7_제목_100자와_본문_5000자는_앞뒤_공백을_빼고_세어_성공한다() throws Exception {
        long id =
                createdId(
                        create(
                                        me,
                                        body(
                                                "FREE",
                                                "  " + "가".repeat(100) + "  ",
                                                " " + "나".repeat(5000) + " ",
                                                null,
                                                null))
                                .andExpect(status().isCreated()));

        assertThat((String) postRow(id).get("title")).hasSize(100);
        assertThat((String) postRow(id).get("content")).hasSize(5000);
    }

    // ── AC8 첨부 검증 ──

    @Test
    void AC8_첨부할_수_없는_분석이면_POST_001이고_글이_생기지_않는다() throws Exception {
        long othersDone = doneAnalysis(other);
        long myFailed = insertAnalysis(me, "FAILED", true, false);
        long myRunning = insertAnalysis(me, "EXPLAINING", true, false);
        long myDeleted = insertAnalysis(me, "DONE", true, true);
        long myPrivate = insertAnalysis(me, "DONE", false, false); // 비공개는 독자가 볼 수 없다 (#206)
        int before = postCount();

        for (long analysis :
                new long[] {othersDone, myFailed, myRunning, myDeleted, myPrivate, 999_999_999L}) {
            create(me, body("QNA", "질문", "본문", analysis, null))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("POST_001"))
                    .andExpect(jsonPath("$.message").value("첨부할 수 없는 분석입니다."));
        }
        assertThat(postCount()).isEqualTo(before);
        assertThat(postActivities(me)).isZero();
    }

    @Test
    void AC8_공개되지_않았거나_없는_카드면_POST_002이고_글이_생기지_않는다() throws Exception {
        long draft = insertConcept("DRAFT");
        long hidden = insertConcept("HIDDEN");
        int before = postCount();

        for (long concept : new long[] {draft, hidden, 999_999_999L}) {
            create(me, body("REVIEW", "후기", "본문", null, concept))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("POST_002"))
                    .andExpect(jsonPath("$.message").value("첨부할 수 없는 개념 카드입니다."));
        }
        assertThat(postCount()).isEqualTo(before);
    }

    // ── AC9 수정 ──

    @Test
    void AC9_내_글을_통째로_바꾸고_조회수와_작성_시각은_그대로다() throws Exception {
        long analysis = doneAnalysis(me);
        long concept = insertConcept("PUBLISHED");
        long post = insertPost(me, "QNA", "처음 제목", "처음 본문", analysis, null, 7, BASE);

        update(me, post, body("FREE", " 바뀐 제목 ", "바뀐 본문", null, concept))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) post))
                .andExpect(jsonPath("$.boardType").value("FREE"))
                .andExpect(jsonPath("$.title").value("바뀐 제목"))
                .andExpect(jsonPath("$.content").value("바뀐 본문"))
                .andExpect(jsonPath("$.analysis").value(nullValue()))
                .andExpect(jsonPath("$.concept.id").value((int) concept))
                .andExpect(jsonPath("$.concept.viewable").value(true))
                .andExpect(jsonPath("$.viewCount").value(7))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T09:00:00+09:00"))
                .andExpect(jsonPath("$.updatedAt").value(not("2026-01-01T09:00:00+09:00")));

        Map<String, Object> row = postRow(post);
        assertThat(row.get("analysis_id")).isNull();
        assertThat(row.get("concept_id")).isEqualTo(concept);
        assertThat(row.get("view_count")).isEqualTo(7);
        assertThat(postActivities(me)).isZero();

        // 첨부 없애기
        update(me, post, body("FREE", "바뀐 제목", "바뀐 본문", null, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysis").value(nullValue()))
                .andExpect(jsonPath("$.concept").value(nullValue()));
        assertThat(postRow(post).get("concept_id")).isNull();
    }

    @Test
    void AC9_수정도_작성과_같은_검증과_첨부_규칙을_따른다() throws Exception {
        long post = insertPost(me, "QNA", "제목", "본문", null, null, 0, BASE);

        update(me, post, body("QNA", "가".repeat(101), "본문", null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.title").isString());
        update(me, post, body("QNA", "제목", "본문", doneAnalysis(me), insertConcept("PUBLISHED")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.conceptId").isString());
        update(me, post, body("QNA", "제목", "본문", doneAnalysis(other), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("POST_001"));
        update(me, post, body("QNA", "제목", "본문", insertAnalysis(me, "DONE", false, false), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("POST_001"));
        update(me, post, body("QNA", "제목", "본문", null, insertConcept("DRAFT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("POST_002"));

        assertThat(postRow(post).get("title")).isEqualTo("제목");
    }

    // ── AC10 삭제 ──

    @Test
    void AC10_내_글을_지우면_204이고_댓글도_지워지며_활동은_남는다() throws Exception {
        long post = insertPost(me, "QNA", "지울 글", BASE);
        insertComment(post, other);
        insertComment(post, me);
        jdbc.update(
                "INSERT INTO activities (user_id, type, target_id) VALUES (?, 'POST', ?)",
                me,
                post);

        remove(me, post).andExpect(status().isNoContent());

        assertThat(count("SELECT COUNT(*) FROM posts WHERE id = ?", post)).isZero();
        assertThat(count("SELECT COUNT(*) FROM comments WHERE post_id = ?", post)).isZero();
        assertThat(postActivities(me)).isEqualTo(1);
        mockMvc.perform(get(POST, post))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_404"));
    }

    // ── AC11 권한·없는 글 ──

    @Test
    void AC11_남의_글을_수정_삭제하면_403이고_글은_그대로다() throws Exception {
        long post = insertPost(other, "QNA", "남의 글", "남의 본문", null, null, 3, BASE);

        update(me, post, body("FREE", "가로채기", "본문", null, null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_403"))
                .andExpect(jsonPath("$.message").value("본인이 쓴 글만 수정·삭제할 수 있습니다."));
        // 잘못된 본문을 보내도 403이 먼저다
        update(me, post, body(null, "", "", 1L, 1L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_403"));
        remove(me, post)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_403"));

        Map<String, Object> row = postRow(post);
        assertThat(row.get("title")).isEqualTo("남의 글");
        assertThat(row.get("board_type")).isEqualTo("QNA");
    }

    @Test
    void AC11_없는_글을_수정_삭제하면_404다() throws Exception {
        update(me, 999_999_999L, body("FREE", "제목", "본문", null, null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_404"));
        update(me, 999_999_999L, body(null, "", "", null, null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_404"));
        remove(me, 999_999_999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_404"));
    }
}
