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

/** F-17 인증 (docs/specs/F-17-community-board.md AC12). */
class PostAuthApiTest extends PostApiTestBase {

    private long me;
    private long post;

    @BeforeEach
    void setUp() {
        me = insertUser("f17인증");
        post = insertPost(me, "QNA", "내 글", "본문", null, null, 0, BASE);
    }

    /** 막아야 하는 Authorization 값(null은 헤더 없음)과 수정·삭제 대상 글. 탈퇴 회원 토큰은 자기 글을 대상으로 한다. */
    private record Attempt(String authorization, long target) {}

    @Test
    void AC12_토큰이_없거나_깨졌거나_탈퇴한_회원이면_작성_수정_삭제가_401이고_아무것도_바뀌지_않는다() throws Exception {
        long withdrawn = insertUser("f17탈퇴토큰");
        long withdrawnPost = insertPost(withdrawn, "FREE", "탈퇴자 글", BASE);
        withdraw(withdrawn);
        String json = body("FREE", "바뀐 제목", "바뀐 본문", null, null);
        int before = count("SELECT COUNT(*) FROM posts");

        List<Attempt> attempts = new ArrayList<>();
        attempts.add(new Attempt(null, post));
        attempts.add(new Attempt(expiredBearer(me), post));
        attempts.add(new Attempt("Bearer not-a-jwt", post));
        attempts.add(new Attempt(bearer(withdrawn), withdrawnPost));

        for (Attempt attempt : attempts) {
            perform(
                            post(POSTS).contentType(MediaType.APPLICATION_JSON).content(json),
                            attempt.authorization())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_401"));
            perform(
                            put(POST, attempt.target())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json),
                            attempt.authorization())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_401"));
            perform(delete(POST, attempt.target()), attempt.authorization())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_401"));
        }

        assertThat(count("SELECT COUNT(*) FROM posts")).isEqualTo(before);
        assertThat(postRow(post).get("title")).isEqualTo("내 글");
        assertThat(postRow(withdrawnPost).get("title")).isEqualTo("탈퇴자 글");
        assertThat(
                        count(
                                "SELECT COUNT(*) FROM activities WHERE type = 'POST' AND user_id IN"
                                        + " (?, ?)",
                                me,
                                withdrawn))
                .isZero();
    }

    @Test
    void AC12_목록과_상세는_토큰이_깨져_있어도_200이다() throws Exception {
        for (String authorization : List.of(expiredBearer(me), "Bearer not-a-jwt")) {
            perform(get(POSTS), authorization).andExpect(status().isOk());
            perform(get(POST, post), authorization).andExpect(status().isOk());
        }
    }
}
