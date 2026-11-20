package com.vibenote.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** F-17 게시글 상세·조회수·첨부 표시 (docs/specs/F-17-community-board.md AC3~AC5). */
class PostDetailApiTest extends PostApiTestBase {

    private long author;
    private long other;

    @BeforeEach
    void setUp() {
        author = insertUser("f17글쓴이");
        other = insertUser("f17구경꾼");
    }

    private ResultActions detail(long postId, String authorization) throws Exception {
        return perform(get(POST, postId), authorization);
    }

    private long postWithAnalysis(long analysisId) {
        return insertPost(author, "QNA", "분석 질문", "본문", analysisId, null, 0, BASE);
    }

    @Test
    void AC4_상세를_열_때마다_조회수가_1씩_오르고_그_값을_응답한다() throws Exception {
        long commenter = insertUser("f17댓글");
        long post =
                insertPost(
                        author,
                        "QNA",
                        "@Transactional이 안 먹어요",
                        "같은 클래스 안에서\n<b>호출</b>하면",
                        null,
                        null,
                        7,
                        BASE);
        insertComment(post, commenter);
        insertComment(post, commenter);

        detail(post, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) post))
                .andExpect(jsonPath("$.boardType").value("QNA"))
                .andExpect(jsonPath("$.title").value("@Transactional이 안 먹어요"))
                .andExpect(jsonPath("$.content").value("같은 클래스 안에서\n<b>호출</b>하면"))
                .andExpect(jsonPath("$.authorId").value((int) author))
                .andExpect(jsonPath("$.authorNickname").value("f17글쓴이"))
                .andExpect(jsonPath("$.viewCount").value(8))
                .andExpect(jsonPath("$.commentCount").value(2))
                .andExpect(jsonPath("$.analysis").value(nullValue()))
                .andExpect(jsonPath("$.concept").value(nullValue()))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T09:00:00+09:00"))
                .andExpect(jsonPath("$.updatedAt").value("2026-01-01T09:00:00+09:00"));

        // 작성자 본인이 열어도 오른다
        detail(post, bearer(author)).andExpect(jsonPath("$.viewCount").value(9));
        assertThat(postRow(post).get("view_count")).isEqualTo(9);
    }

    @Test
    void AC4_없는_글이면_404이고_어떤_글의_조회수도_바뀌지_않는다() throws Exception {
        long post = insertPost(author, "FREE", "있는 글", "본문", null, null, 7, BASE);
        int before = count("SELECT COALESCE(SUM(view_count), 0) FROM posts");

        detail(999_999_999L, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_404"))
                .andExpect(jsonPath("$.message").value("게시글을 찾을 수 없습니다."));

        assertThat(count("SELECT COALESCE(SUM(view_count), 0) FROM posts")).isEqualTo(before);
        assertThat(postRow(post).get("view_count")).isEqualTo(7);
    }

    @Test
    void AC3_탈퇴한_작성자의_글도_상세에_나오고_작성자를_숨긴다() throws Exception {
        long gone = insertUser("f17떠난이");
        long post = insertPost(gone, "FREE", "탈퇴자의 글", BASE);
        withdraw(gone);

        detail(post, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorId").value(nullValue()))
                .andExpect(jsonPath("$.authorNickname").value("탈퇴한 사용자"));
    }

    @Test
    void AC5_공개_분석은_누구에게나_제목과_프로젝트명이_보인다() throws Exception {
        long post = postWithAnalysis(insertAnalysis(author, "DONE", true, false));

        detail(post, null)
                .andExpect(jsonPath("$.analysis.viewable").value(true))
                .andExpect(jsonPath("$.analysis.title").value("쇼핑몰 과제"))
                .andExpect(jsonPath("$.analysis.projectName").value("shop-backend"))
                .andExpect(jsonPath("$.concept").value(nullValue()));
        detail(post, bearer(other)).andExpect(jsonPath("$.analysis.viewable").value(true));
    }

    @Test
    void AC5_비공개_분석은_주인에게만_보이고_남과_비로그인에게는_숨긴다() throws Exception {
        long analysis = insertAnalysis(author, "DONE", false, false);
        long post = postWithAnalysis(analysis);

        detail(post, bearer(author))
                .andExpect(jsonPath("$.analysis.id").value((int) analysis))
                .andExpect(jsonPath("$.analysis.viewable").value(true))
                .andExpect(jsonPath("$.analysis.title").value("쇼핑몰 과제"));

        for (String authorization : new String[] {bearer(other), null}) {
            detail(post, authorization)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.analysis.id").value((int) analysis))
                    .andExpect(jsonPath("$.analysis.viewable").value(false))
                    .andExpect(jsonPath("$.analysis.title").value(nullValue()))
                    .andExpect(jsonPath("$.analysis.projectName").value(nullValue()));
        }
    }

    @Test
    void AC5_삭제된_분석은_주인에게도_숨긴다() throws Exception {
        long analysis = insertAnalysis(author, "DONE", true, true);
        long post = postWithAnalysis(analysis);

        detail(post, bearer(author))
                .andExpect(jsonPath("$.analysis.id").value((int) analysis))
                .andExpect(jsonPath("$.analysis.viewable").value(false))
                .andExpect(jsonPath("$.analysis.title").value(nullValue()))
                .andExpect(jsonPath("$.analysis.projectName").value(nullValue()));
    }

    @Test
    void AC5_공개_카드는_요약을_보이고_공개되지_않은_카드는_숨긴다() throws Exception {
        long published = insertConcept("PUBLISHED");
        long draft = insertConcept("DRAFT");
        long withPublished = insertPost(author, "REVIEW", "카드 후기", "본문", null, published, 0, BASE);
        long withDraft = insertPost(author, "REVIEW", "초안 카드", "본문", null, draft, 0, BASE);

        detail(withPublished, null)
                .andExpect(jsonPath("$.analysis").value(nullValue()))
                .andExpect(jsonPath("$.concept.id").value((int) published))
                .andExpect(jsonPath("$.concept.viewable").value(true))
                .andExpect(jsonPath("$.concept.name").exists())
                .andExpect(jsonPath("$.concept.category").value("f17"))
                .andExpect(jsonPath("$.concept.difficulty").value("JUNIOR"))
                .andExpect(jsonPath("$.concept.summary").value("서버가 서명한 토큰"));

        detail(withDraft, bearer(author))
                .andExpect(jsonPath("$.concept.id").value((int) draft))
                .andExpect(jsonPath("$.concept.viewable").value(false))
                .andExpect(jsonPath("$.concept.name").value(nullValue()))
                .andExpect(jsonPath("$.concept.category").value(nullValue()))
                .andExpect(jsonPath("$.concept.difficulty").value(nullValue()))
                .andExpect(jsonPath("$.concept.summary").value(nullValue()));
    }
}
