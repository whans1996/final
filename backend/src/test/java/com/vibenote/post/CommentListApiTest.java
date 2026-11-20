package com.vibenote.post;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** F-18 댓글 목록 (docs/specs/F-18-comments.md AC1~AC4). */
class CommentListApiTest extends PostApiTestBase {

    private long writer;
    private long reader;
    private long post;

    @BeforeEach
    void setUp() {
        writer = insertUser("f18글쓴이");
        reader = insertUser("f18댓글");
        post = insertPost(writer, "QNA", "질문", BASE);
    }

    @Test
    void AC1_비로그인으로_댓글을_오래된순_전부_조회한다() throws Exception {
        long other = insertPost(writer, "FREE", "다른 글", BASE);
        // C2·C3은 같은 시각이라 id 순서로 정렬된다. 넣는 순서를 섞어 created_at 정렬을 확인한다
        long sameTimeFirst = insertComment(post, reader, "같은 시각 앞", BASE.plusMinutes(5));
        long c1 = insertComment(post, writer, "첫 번째", BASE.plusMinutes(1));
        long sameTimeSecond = insertComment(post, reader, "같은 시각 뒤\n줄바꿈", BASE.plusMinutes(5));
        insertComment(other, reader, "다른 글 댓글", BASE);

        perform(get(COMMENTS, post), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(
                        jsonPath("$.items[*].id")
                                .value(
                                        contains(
                                                (int) c1,
                                                (int) sameTimeFirst,
                                                (int) sameTimeSecond)))
                .andExpect(jsonPath("$.items[0].content").value("첫 번째"))
                .andExpect(jsonPath("$.items[0].authorId").value(writer))
                .andExpect(jsonPath("$.items[0].authorNickname").value("f18글쓴이"))
                .andExpect(jsonPath("$.items[0].edited").value(false))
                .andExpect(jsonPath("$.items[0].createdAt").value("2026-01-01T09:01:00+09:00"))
                .andExpect(jsonPath("$.items[0].updatedAt").value("2026-01-01T09:01:00+09:00"))
                .andExpect(jsonPath("$.items[2].content").value("같은 시각 뒤\n줄바꿈"))
                .andExpect(jsonPath("$.items[2].authorId").value(reader));
    }

    @Test
    void AC1_댓글이_없으면_빈_목록과_0을_받는다() throws Exception {
        perform(get(COMMENTS, post), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void AC2_댓글이_201개면_오래된_200개와_total_201을_받는다() throws Exception {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < 201; i++) {
            rows.add(
                    new Object[] {
                        post, reader, "댓글 " + i, BASE.plusSeconds(i), BASE.plusSeconds(i)
                    });
        }
        jdbc.batchUpdate(
                "INSERT INTO comments (post_id, user_id, content, created_at, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?)",
                rows);

        perform(get(COMMENTS, post), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(201))
                .andExpect(jsonPath("$.items", hasSize(200)))
                .andExpect(jsonPath("$.items[0].content").value("댓글 0"))
                .andExpect(jsonPath("$.items[199].content").value("댓글 199"));
    }

    @Test
    void AC3_탈퇴_작성자는_탈퇴한_사용자로_보이고_수정한_댓글만_edited다() throws Exception {
        long gone = insertUser("f18탈퇴");
        insertComment(post, gone, "탈퇴자 댓글", BASE);
        insertComment(post, reader, "고친 댓글", BASE.plusMinutes(1), BASE.plusMinutes(3));
        insertComment(post, reader, "그대로 댓글", BASE.plusMinutes(2));
        withdraw(gone);
        jdbc.update("UPDATE users SET nickname = 'f18새닉' WHERE id = ?", reader);

        perform(get(COMMENTS, post), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[0].authorId").value(nullValue()))
                .andExpect(jsonPath("$.items[0].authorNickname").value("탈퇴한 사용자"))
                .andExpect(jsonPath("$.items[0].content").value("탈퇴자 댓글"))
                .andExpect(jsonPath("$.items[1].authorId").value(reader))
                .andExpect(jsonPath("$.items[1].authorNickname").value("f18새닉"))
                .andExpect(jsonPath("$.items[1].edited").value(true))
                .andExpect(jsonPath("$.items[1].updatedAt").value("2026-01-01T09:03:00+09:00"))
                .andExpect(jsonPath("$.items[2].edited").value(false));
    }

    @Test
    void AC4_없거나_지워진_글의_댓글_목록은_404다() throws Exception {
        long removed = insertPost(writer, "FREE", "지울 글", BASE);
        insertComment(removed, reader, "댓글", BASE);
        jdbc.update("DELETE FROM posts WHERE id = ?", removed);

        for (long id : List.of(999999L, removed)) {
            perform(get(COMMENTS, id), null)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("POST_404"))
                    .andExpect(jsonPath("$.message").value("게시글을 찾을 수 없습니다."));
        }
    }
}
