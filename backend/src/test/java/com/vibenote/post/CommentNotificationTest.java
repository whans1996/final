package com.vibenote.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** NT-1 AC10 댓글 알림. */
class CommentNotificationTest extends PostApiTestBase {

    private static final String BODY = "{\"content\": \"좋은 질문이에요\"}";

    private List<Map<String, Object>> notifications(long userId) {
        return jdbc.queryForList(
                "SELECT type, actor_id, message, link FROM notifications WHERE recipient_id = ?",
                userId);
    }

    private void comment(long postId, long userId) throws Exception {
        perform(
                        post(COMMENTS, postId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY),
                        bearer(userId))
                .andExpect(status().isCreated());
    }

    @Test
    void AC10_남의_글에_댓글을_쓰면_글쓴이에게_알린다() throws Exception {
        long author = insertUser("nt1au");
        long writer = insertUser("nt1wr");
        long p = insertPost(author, "QNA", "Spring 질문", LocalDateTime.now());

        comment(p, writer);

        assertThat(notifications(author))
                .singleElement()
                .satisfies(
                        n -> {
                            assertThat(n.get("type")).isEqualTo("COMMENT");
                            assertThat(((Number) n.get("actor_id")).longValue()).isEqualTo(writer);
                            assertThat(n.get("message")).isEqualTo("회원님의 글에 댓글을 남겼어요 · Spring 질문");
                            assertThat(n.get("link")).isEqualTo("/community/" + p);
                        });
    }

    @Test
    void AC10_제목이_40자를_넘으면_39자와_말줄임표() throws Exception {
        long author = insertUser("nt1au");
        long writer = insertUser("nt1wr");
        long p = insertPost(author, "QNA", "가".repeat(45), LocalDateTime.now());

        comment(p, writer);

        assertThat(notifications(author))
                .singleElement()
                .extracting(n -> n.get("message"))
                .isEqualTo("회원님의 글에 댓글을 남겼어요 · " + "가".repeat(39) + "…");
    }

    @Test
    void AC10_자기_글에_쓰거나_수정_삭제하면_알림이_없다() throws Exception {
        long author = insertUser("nt1au");
        long writer = insertUser("nt1wr");
        long p = insertPost(author, "QNA", "Spring 질문", LocalDateTime.now());
        comment(p, author);
        long c = insertComment(p, writer, "기존 댓글", LocalDateTime.now());

        perform(
                        put(COMMENT, p, c)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\": \"고친 댓글\"}"),
                        bearer(writer))
                .andExpect(status().isOk());
        perform(delete(COMMENT, p, c), bearer(writer)).andExpect(status().isNoContent());

        assertThat(notifications(author)).isEmpty();
    }
}
