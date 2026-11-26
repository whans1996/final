package com.vibenote.post;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.security.JwtProperties;
import com.vibenote.common.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * F-17 게시판·F-18 댓글 통합 테스트 공통 데이터 준비. 회원·분석·카드·글을 직접 넣고 테스트마다 롤백한다. 시드 카드와 다른 테스트의 데이터에 기대지 않도록 카드
 * 카테고리는 {@code f17}, 이메일은 {@code @f17.test}를 쓴다.
 */
@Transactional
abstract class PostApiTestBase extends IntegrationTestBase {

    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected JwtTokenProvider tokenProvider;
    @Autowired protected JwtProperties jwtProperties;

    protected static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 1, 9, 0);
    protected static final String POSTS = "/api/posts";
    protected static final String POST = "/api/posts/{id}";
    protected static final String COMMENTS = "/api/posts/{postId}/comments";
    protected static final String COMMENT = "/api/posts/{postId}/comments/{commentId}";

    // ── 회원 ──

    protected long insertUser(String name) {
        jdbc.update(
                "INSERT INTO users (email, password_hash, nickname) VALUES (?, 'hash', ?)",
                name + "@f17.test",
                name);
        return lastId();
    }

    protected void withdraw(long userId) {
        jdbc.update(
                "UPDATE users SET status = 'WITHDRAWN', withdrawn_at = NOW() WHERE id = ?", userId);
    }

    // ── 첨부 대상 ──

    protected long insertAnalysis(long userId, String status, boolean isPublic, boolean deleted) {
        jdbc.update(
                "INSERT INTO analyses (user_id, source_type, project_name, title, level, status,"
                        + " is_public, deleted_at) VALUES (?, 'ZIP', 'shop-backend', '쇼핑몰 과제',"
                        + " 'JUNIOR', ?, ?, ?)",
                userId,
                status,
                isPublic,
                deleted ? BASE : null);
        return lastId();
    }

    /** 첨부할 수 있는 분석: 공개·DONE·삭제 안 됨 (비공개는 첨부할 수 없다, #206). */
    protected long doneAnalysis(long userId) {
        return insertAnalysis(userId, "DONE", true, false);
    }

    protected long insertConcept(String status) {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, status, source)"
                        + " VALUES (?, 'f17', 'JUNIOR', '서버가 서명한 토큰', ?, 'AI')",
                "F17 카드 " + System.nanoTime(),
                status);
        return lastId();
    }

    // ── 글·댓글 ──

    protected long insertPost(long userId, String boardType, String title, LocalDateTime at) {
        return insertPost(userId, boardType, title, "본문", null, null, 0, at);
    }

    protected long insertPost(
            long userId,
            String boardType,
            String title,
            String content,
            Long analysisId,
            Long conceptId,
            int viewCount,
            LocalDateTime at) {
        jdbc.update(
                "INSERT INTO posts (user_id, board_type, title, content, analysis_id, concept_id,"
                        + " view_count, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                userId,
                boardType,
                title,
                content,
                analysisId,
                conceptId,
                viewCount,
                at,
                at);
        return lastId();
    }

    protected void insertComment(long postId, long userId) {
        jdbc.update(
                "INSERT INTO comments (post_id, user_id, content) VALUES (?, ?, '댓글')",
                postId,
                userId);
    }

    /** F-18. 작성·수정 시각을 직접 넣는다 (updatedAt이 createdAt보다 뒤면 edited). */
    protected long insertComment(
            long postId,
            long userId,
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        jdbc.update(
                "INSERT INTO comments (post_id, user_id, content, created_at, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?)",
                postId,
                userId,
                content,
                createdAt,
                updatedAt);
        return lastId();
    }

    protected long insertComment(long postId, long userId, String content, LocalDateTime at) {
        return insertComment(postId, userId, content, at, at);
    }

    protected Map<String, Object> commentRow(long commentId) {
        return jdbc.queryForMap("SELECT * FROM comments WHERE id = ?", commentId);
    }

    /** 목록 테스트가 다른 데이터와 섞이지 않게 글을 비운다 (롤백된다). 댓글은 FK CASCADE로 함께 지워진다. */
    protected void clearPosts() {
        jdbc.update("DELETE FROM posts");
    }

    protected Map<String, Object> postRow(long postId) {
        return jdbc.queryForMap("SELECT * FROM posts WHERE id = ?", postId);
    }

    protected int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    protected long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    // ── 요청 ──

    protected String bearer(long userId) {
        return "Bearer " + tokenProvider.createAccessToken(userId, "USER");
    }

    protected String expiredBearer(long userId) {
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

    protected ResultActions perform(MockHttpServletRequestBuilder request, String authorization)
            throws Exception {
        if (authorization != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return mockMvc.perform(request);
    }

    /** 요청 본문. null인 값은 키를 빼지 않고 JSON null로 보낸다 (첨부 해제 표현). */
    protected String body(
            String boardType, String title, String content, Long analysisId, Long conceptId)
            throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("boardType", boardType);
        map.put("title", title);
        map.put("content", content);
        map.put("analysisId", analysisId);
        map.put("conceptId", conceptId);
        return objectMapper.writeValueAsString(map);
    }

    /** 댓글 요청 본문. null이면 {@code {"content": null}}로 보낸다. */
    protected String commentBody(String content) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("content", content);
        return objectMapper.writeValueAsString(map);
    }
}
