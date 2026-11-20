package com.vibenote.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** F-18 댓글 작성·수정·삭제 (docs/specs/F-18-comments.md AC4~AC9). */
class CommentWriteApiTest extends PostApiTestBase {

    private long me;
    private long other;
    private long post;

    @BeforeEach
    void setUp() {
        me = insertUser("f18나");
        other = insertUser("f18남");
        post = insertPost(other, "QNA", "남의 질문", BASE);
    }

    private ResultActions create(long userId, long postId, String json) throws Exception {
        return perform(
                post(COMMENTS, postId).contentType(MediaType.APPLICATION_JSON).content(json),
                bearer(userId));
    }

    private ResultActions update(long userId, long postId, long commentId, String json)
            throws Exception {
        return perform(
                put(COMMENT, postId, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json),
                bearer(userId));
    }

    private ResultActions remove(long userId, long postId, long commentId) throws Exception {
        return perform(delete(COMMENT, postId, commentId), bearer(userId));
    }

    private long createdId(ResultActions result) throws Exception {
        String json = result.andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private int commentCount() {
        return count("SELECT COUNT(*) FROM comments");
    }

    // ── AC4 없는 글 ──

    @Test
    void AC4_없거나_지워진_글에_작성_수정_삭제하면_404이고_아무것도_바뀌지_않는다() throws Exception {
        long mine = insertComment(post, me, "남는 댓글", BASE);
        long removed = insertPost(me, "FREE", "지울 글", BASE);
        jdbc.update("DELETE FROM posts WHERE id = ?", removed);
        int before = commentCount();

        for (long postId : List.of(999999L, removed)) {
            create(me, postId, commentBody("댓글"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("POST_404"));
            update(me, postId, mine, commentBody("고침"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("POST_404"));
            remove(me, postId, mine)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("POST_404"))
                    .andExpect(jsonPath("$.message").value("게시글을 찾을 수 없습니다."));
        }

        assertThat(commentCount()).isEqualTo(before);
        assertThat(commentRow(mine).get("content")).isEqualTo("남는 댓글");
    }

    // ── AC5 작성 ──

    @Test
    void AC5_댓글을_쓰면_201과_다듬은_내용을_받고_활동은_남지_않으며_commentCount가_오른다() throws Exception {
        ResultActions result =
                create(me, post, commentBody("  같은 클래스 안 호출은 프록시를 거치지 않아서예요.  "))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id").isNumber())
                        .andExpect(jsonPath("$.content").value("같은 클래스 안 호출은 프록시를 거치지 않아서예요."))
                        .andExpect(jsonPath("$.authorId").value(me))
                        .andExpect(jsonPath("$.authorNickname").value("f18나"))
                        .andExpect(jsonPath("$.edited").value(false))
                        .andExpect(jsonPath("$.createdAt").isString())
                        .andExpect(jsonPath("$.updatedAt").isString());
        long id = createdId(result);

        Map<String, Object> row = commentRow(id);
        assertThat(row.get("post_id")).isEqualTo(post);
        assertThat(row.get("user_id")).isEqualTo(me);
        assertThat(row.get("content")).isEqualTo("같은 클래스 안 호출은 프록시를 거치지 않아서예요.");
        assertThat(count("SELECT COUNT(*) FROM activities WHERE user_id = ?", me)).isZero();

        perform(get(POST, post), null).andExpect(jsonPath("$.commentCount").value(1));
        perform(get(POSTS).param("boardType", "QNA"), null)
                .andExpect(jsonPath("$.items[?(@.id == " + post + ")].commentCount").value(1));
    }

    @Test
    void AC5_자기_글에도_댓글을_쓸_수_있다() throws Exception {
        long myPost = insertPost(me, "FREE", "내 글", BASE);

        create(me, myPost, commentBody("내가 단 댓글"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorId").value(me));
    }

    // ── AC6 검증 ──

    static Stream<Arguments> invalidContents() {
        return Stream.of(
                Arguments.of("{}"),
                Arguments.of("{\"content\":null}"),
                Arguments.of("{\"content\":\"  \\n  \"}"),
                Arguments.of("{\"content\":\" " + "가".repeat(501) + " \"}"));
    }

    @ParameterizedTest
    @MethodSource("invalidContents")
    void AC6_내용이_없거나_공백이거나_501자면_작성_수정이_400이다(String json) throws Exception {
        long mine = insertComment(post, me, "원래 내용", BASE);
        int before = commentCount();

        create(me, post, json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.content").isString());
        update(me, post, mine, json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.content").isString());

        assertThat(commentCount()).isEqualTo(before);
        assertThat(commentRow(mine).get("content")).isEqualTo("원래 내용");
    }

    @Test
    void AC6_앞뒤_공백을_빼고_정확히_500자면_작성_수정이_성공한다() throws Exception {
        String exact = "가".repeat(500);
        long mine = insertComment(post, me, "원래 내용", BASE);

        long id =
                createdId(
                        create(me, post, commentBody("   " + exact + "\n"))
                                .andExpect(status().isCreated()));
        update(me, post, mine, commentBody("  " + exact + "  ")).andExpect(status().isOk());

        assertThat(commentRow(id).get("content")).isEqualTo(exact);
        assertThat(commentRow(mine).get("content")).isEqualTo(exact);
    }

    // ── AC7 수정 ──

    @Test
    void AC7_내_댓글을_고치면_200과_바뀐_내용을_받고_edited가_true다() throws Exception {
        LocalDateTime past = BASE;
        long mine = insertComment(post, me, "처음 내용", past);

        update(me, post, mine, commentBody("  고친 내용 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mine))
                .andExpect(jsonPath("$.content").value("고친 내용"))
                .andExpect(jsonPath("$.authorId").value(me))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T09:00:00+09:00"))
                .andExpect(jsonPath("$.edited").value(true));

        Map<String, Object> row = commentRow(mine);
        assertThat(row.get("content")).isEqualTo("고친 내용");
        assertThat((LocalDateTime) row.get("created_at")).isEqualTo(past);
        assertThat((LocalDateTime) row.get("updated_at")).isAfter(past);
    }

    @Test
    void AC7_같은_내용으로_보내면_200이고_edited가_바뀌지_않는다() throws Exception {
        long mine = insertComment(post, me, "그대로", BASE);

        update(me, post, mine, commentBody(" 그대로 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("그대로"))
                .andExpect(jsonPath("$.edited").value(false))
                .andExpect(jsonPath("$.updatedAt").value("2026-01-01T09:00:00+09:00"));

        assertThat((LocalDateTime) commentRow(mine).get("updated_at")).isEqualTo(BASE);
    }

    // ── AC8 삭제 ──

    @Test
    void AC8_내_댓글을_지우면_204이고_목록과_commentCount에서_빠진다() throws Exception {
        long mine = insertComment(post, me, "지울 댓글", BASE);
        long kept = insertComment(post, other, "남는 댓글", BASE.plusMinutes(1));

        remove(me, post, mine).andExpect(status().isNoContent());

        assertThat(count("SELECT COUNT(*) FROM comments WHERE id = ?", mine)).isZero();
        assertThat(commentRow(kept).get("content")).isEqualTo("남는 댓글");
        assertThat(postRow(post).get("title")).isEqualTo("남의 질문");
        perform(get(COMMENTS, post), null)
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(kept));
        perform(get(POST, post), null).andExpect(jsonPath("$.commentCount").value(1));
    }

    // ── AC9 권한·소속 ──

    @Test
    void AC9_남의_댓글은_수정_삭제가_403이고_빈_내용이어도_403이_먼저다() throws Exception {
        long theirs = insertComment(post, other, "남의 댓글", BASE);

        for (String json : List.of(commentBody("고침"), commentBody("   "), "{}")) {
            update(me, post, theirs, json)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("POST_004"))
                    .andExpect(jsonPath("$.message").value("본인이 쓴 댓글만 수정·삭제할 수 있습니다."));
        }
        remove(me, post, theirs)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_004"));

        assertThat(commentRow(theirs).get("content")).isEqualTo("남의 댓글");
    }

    @Test
    void AC9_글_작성자라도_남의_댓글은_수정_삭제할_수_없다() throws Exception {
        long mine = insertComment(post, me, "내 댓글", BASE);

        update(other, post, mine, commentBody("글쓴이가 고침"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_004"));
        remove(other, post, mine)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_004"));

        assertThat(commentRow(mine).get("content")).isEqualTo("내 댓글");
    }

    @Test
    void AC9_없는_댓글이나_다른_글의_댓글을_이_글_경로로_보내면_404_POST_003이다() throws Exception {
        long otherPost = insertPost(other, "FREE", "다른 글", BASE);
        long elsewhere = insertComment(otherPost, me, "다른 글의 내 댓글", BASE);

        for (long commentId : List.of(999999L, elsewhere)) {
            update(me, post, commentId, commentBody("고침"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("POST_003"))
                    .andExpect(jsonPath("$.message").value("댓글을 찾을 수 없습니다."));
            remove(me, post, commentId)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("POST_003"));
        }
        // 없는 댓글에 빈 내용을 보내도 404가 400보다 먼저다
        update(me, post, 999999L, commentBody(" "))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_003"));

        assertThat(commentRow(elsewhere).get("content")).isEqualTo("다른 글의 내 댓글");
    }
}
