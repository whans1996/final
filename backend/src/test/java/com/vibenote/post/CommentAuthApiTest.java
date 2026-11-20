package com.vibenote.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** F-18 댓글 인증 (docs/specs/F-18-comments.md AC10). */
class CommentAuthApiTest extends PostApiTestBase {

    private long me;
    private long post;
    private long comment;

    @BeforeEach
    void setUp() {
        me = insertUser("f18인증");
        post = insertPost(me, "QNA", "내 글", BASE);
        comment = insertComment(post, me, "내 댓글", BASE);
    }

    /** 막아야 하는 Authorization 값(null은 헤더 없음)과 수정·삭제 대상 댓글. 탈퇴 회원 토큰은 자기 댓글을 대상으로 한다. */
    private record Attempt(String authorization, long target) {}

    @Test
    void AC10_토큰이_없거나_깨졌거나_탈퇴한_회원이면_작성_수정_삭제가_401이고_아무것도_바뀌지_않는다() throws Exception {
        long withdrawn = insertUser("f18탈퇴토큰");
        long withdrawnComment = insertComment(post, withdrawn, "탈퇴자 댓글", BASE);
        withdraw(withdrawn);
        String json = commentBody("바뀐 내용");
        int before = count("SELECT COUNT(*) FROM comments");

        List<Attempt> attempts = new ArrayList<>();
        attempts.add(new Attempt(null, comment));
        attempts.add(new Attempt(expiredBearer(me), comment));
        attempts.add(new Attempt("Bearer not-a-jwt", comment));
        attempts.add(new Attempt(bearer(withdrawn), withdrawnComment));

        for (Attempt attempt : attempts) {
            perform(
                            post(COMMENTS, post)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json),
                            attempt.authorization())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_401"));
            perform(
                            put(COMMENT, post, attempt.target())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json),
                            attempt.authorization())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_401"));
            perform(delete(COMMENT, post, attempt.target()), attempt.authorization())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_401"));
        }

        assertThat(count("SELECT COUNT(*) FROM comments")).isEqualTo(before);
        assertThat(commentRow(comment).get("content")).isEqualTo("내 댓글");
        assertThat(commentRow(withdrawnComment).get("content")).isEqualTo("탈퇴자 댓글");
    }

    @Test
    void AC10_목록은_토큰이_깨져_있어도_200이다() throws Exception {
        for (String authorization : List.of(expiredBearer(me), "Bearer not-a-jwt")) {
            perform(get(COMMENTS, post), authorization)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(1));
        }
    }
}
