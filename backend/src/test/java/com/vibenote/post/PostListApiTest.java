package com.vibenote.post;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** F-17 게시글 목록·검색 (docs/specs/F-17-community-board.md AC1~AC3). */
class PostListApiTest extends PostApiTestBase {

    private long author;

    @BeforeEach
    void setUp() {
        clearPosts();
        author = insertUser("f17작성자");
    }

    private ResultActions list(String... params) throws Exception {
        var request = get(POSTS);
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(request);
    }

    private List<Integer> ids(List<Long> longs) {
        return longs.stream().map(Long::intValue).toList();
    }

    @Test
    void AC1_세_게시판_글을_최신순으로_20개씩_페이지_조회한다() throws Exception {
        String[] boards = {"QNA", "REVIEW", "FREE"};
        List<Long> created = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            created.add(insertPost(author, boards[i % 3], "글 " + i, BASE.plusMinutes(i)));
        }
        // 같은 시각이면 id가 큰 쪽이 먼저
        long sameTime = insertPost(author, "FREE", "같은 시각", BASE.plusMinutes(24));
        created.add(sameTime);
        List<Long> latestFirst = new ArrayList<>(created);
        Collections.reverse(latestFirst);

        list("page", "1", "size", "20")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total").value(26))
                .andExpect(jsonPath("$.items", hasSize(20)))
                .andExpect(
                        jsonPath("$.items[*].id")
                                .value(contains(ids(latestFirst.subList(0, 20)).toArray())))
                .andExpect(jsonPath("$.items[0]", not(hasKey("content"))));

        list("page", "2", "size", "20")
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.items[*].id")
                                .value(contains(ids(latestFirst.subList(20, 26)).toArray())));

        list("page", "3", "size", "20")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", empty()))
                .andExpect(jsonPath("$.total").value(26));

        // size를 주지 않으면 20
        list().andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.items", hasSize(20)));
    }

    @Test
    void AC1_항목은_요약_필드와_댓글_수와_첨부_종류를_담는다() throws Exception {
        long commenter = insertUser("f17댓글러");
        long analysis = doneAnalysis(author);
        long concept = insertConcept("PUBLISHED");
        long withAnalysis = insertPost(author, "QNA", "분석 첨부", "본문", analysis, null, 42, BASE);
        long withConcept =
                insertPost(author, "REVIEW", "카드 첨부", "본문", null, concept, 0, BASE.plusMinutes(1));
        long plain = insertPost(author, "FREE", "첨부 없음", BASE.plusMinutes(2));
        insertComment(withAnalysis, commenter);
        insertComment(withAnalysis, commenter);
        insertComment(withAnalysis, author);
        // 첨부한 분석이 지워져도(viewable과 상관없이) 첨부 종류는 그대로 알린다
        jdbc.update("UPDATE analyses SET deleted_at = NOW() WHERE id = ?", analysis);

        list().andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.items[*].id")
                                .value(
                                        contains(
                                                (int) plain,
                                                (int) withConcept,
                                                (int) withAnalysis)))
                .andExpect(jsonPath("$.items[0].attachmentType").value(nullValue()))
                .andExpect(jsonPath("$.items[0].commentCount").value(0))
                .andExpect(jsonPath("$.items[1].attachmentType").value("CONCEPT"))
                .andExpect(jsonPath("$.items[1].boardType").value("REVIEW"))
                .andExpect(jsonPath("$.items[2].attachmentType").value("ANALYSIS"))
                .andExpect(jsonPath("$.items[2].boardType").value("QNA"))
                .andExpect(jsonPath("$.items[2].title").value("분석 첨부"))
                .andExpect(jsonPath("$.items[2].commentCount").value(3))
                .andExpect(jsonPath("$.items[2].viewCount").value(42))
                .andExpect(jsonPath("$.items[2].authorId").value((int) author))
                .andExpect(jsonPath("$.items[2].authorNickname").value("f17작성자"))
                .andExpect(jsonPath("$.items[2].createdAt").value("2026-01-01T09:00:00+09:00"));
    }

    @Test
    void AC2_게시판과_검색어로_거르고_둘을_함께_주면_둘_다_만족하는_글만_받는다() throws Exception {
        long qnaTitle = insertPost(author, "QNA", "트랜잭션이 안 걸려요", BASE);
        long qnaContent =
                insertPost(
                        author,
                        "QNA",
                        "질문",
                        "@Transactional 트랜잭션 전파",
                        null,
                        null,
                        0,
                        BASE.plusMinutes(1));
        long qnaOther = insertPost(author, "QNA", "JWT 질문", BASE.plusMinutes(2));
        long freeMatch = insertPost(author, "FREE", "트랜잭션 스터디", BASE.plusMinutes(3));
        long reviewOther = insertPost(author, "REVIEW", "후기", BASE.plusMinutes(4));

        list("boardType", "QNA")
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(
                        jsonPath("$.items[*].id")
                                .value(contains((int) qnaOther, (int) qnaContent, (int) qnaTitle)));

        list("keyword", "트랜잭션")
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(
                        jsonPath("$.items[*].id")
                                .value(
                                        contains(
                                                (int) freeMatch,
                                                (int) qnaContent,
                                                (int) qnaTitle)));

        list("boardType", "QNA", "keyword", "  트랜잭션  ")
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(
                        jsonPath("$.items[*].id")
                                .value(contains((int) qnaContent, (int) qnaTitle)));

        // 빈 검색어는 조건 없음
        list("keyword", "   ").andExpect(jsonPath("$.total").value(5));
        // 대소문자 무시
        list("keyword", "jwt").andExpect(jsonPath("$.items[*].id").value(contains((int) qnaOther)));
        list("boardType", "REVIEW")
                .andExpect(jsonPath("$.items[*].id").value(contains((int) reviewOther)));
    }

    @Test
    void AC2_검색어의_퍼센트와_밑줄은_글자_그대로_찾는다() throws Exception {
        long percent = insertPost(author, "FREE", "이해도 100% 달성", BASE);
        insertPost(author, "FREE", "이해도 1000점", BASE.plusMinutes(1));
        long underscore = insertPost(author, "FREE", "snake_case 질문", BASE.plusMinutes(2));
        insertPost(author, "FREE", "snakeXcase 질문", BASE.plusMinutes(3));
        long bang = insertPost(author, "FREE", "느낌표!", BASE.plusMinutes(4));

        list("keyword", "100%").andExpect(jsonPath("$.items[*].id").value(contains((int) percent)));
        list("keyword", "e_c")
                .andExpect(jsonPath("$.items[*].id").value(contains((int) underscore)));
        list("keyword", "표!").andExpect(jsonPath("$.items[*].id").value(contains((int) bang)));
    }

    @Test
    void AC2_잘못된_게시판이나_51자_검색어는_400과_해당_필드를_받는다() throws Exception {
        list("boardType", "NOTICE")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.boardType").exists());

        list("keyword", "가".repeat(51))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.keyword").exists());

        // 앞뒤 공백을 뺀 50자는 성공
        list("keyword", "  " + "가".repeat(50) + "  ").andExpect(status().isOk());
    }

    @Test
    void AC3_탈퇴한_작성자의_글도_목록과_total에_나오고_작성자를_숨긴다() throws Exception {
        long gone = insertUser("f17탈퇴자");
        long goneSPost = insertPost(gone, "FREE", "탈퇴자의 글", BASE);
        long activePost = insertPost(author, "FREE", "활성 회원 글", BASE.plusMinutes(1));
        withdraw(gone);

        list().andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(
                        jsonPath("$.items[*].id")
                                .value(contains((int) activePost, (int) goneSPost)))
                .andExpect(jsonPath("$.items[0].authorId").value((int) author))
                .andExpect(jsonPath("$.items[1].authorId").value(nullValue()))
                .andExpect(jsonPath("$.items[1].authorNickname").value("탈퇴한 사용자"));
    }
}
