package com.vibenote.team;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * F-20a 팀 통합 테스트 공통 데이터 준비. 회원·팀을 테스트마다 직접 넣고 롤백한다. 이메일은 {@code @f20.test}, 직접 넣는 초대 코드는 생성기가 만들 수
 * 없는 {@code 0}·{@code 1}을 섞어 겹치지 않게 한다.
 */
@Transactional
abstract class TeamTestSupport extends IntegrationTestBase {

    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected JwtTokenProvider tokenProvider;
    @Autowired protected JwtProperties jwtProperties;

    protected static final LocalDateTime BASE = LocalDateTime.of(2026, 11, 24, 10, 0);

    protected long insertUser(String name) {
        return insertUser(name, "BEGINNER");
    }

    protected long insertUser(String name, String level) {
        jdbc.update(
                "INSERT INTO users (email, password_hash, nickname, level) VALUES (?, 'hash', ?,"
                        + " ?)",
                name + "@f20.test",
                name,
                level);
        return lastId();
    }

    protected void withdraw(long userId) {
        jdbc.update(
                "UPDATE users SET status = 'WITHDRAWN', withdrawn_at = NOW() WHERE id = ?", userId);
    }

    /** 팀과 팀장 멤버 행을 넣는다. */
    protected long insertTeam(long ownerId, String name, String code, LocalDateTime at) {
        jdbc.update(
                "INSERT INTO teams (name, description, owner_id, invite_code, created_at,"
                        + " updated_at) VALUES (?, NULL, ?, ?, ?, ?)",
                name,
                ownerId,
                code,
                at,
                at);
        long teamId = lastId();
        addMember(teamId, ownerId, "OWNER", at);
        return teamId;
    }

    protected void addMember(long teamId, long userId, LocalDateTime at) {
        addMember(teamId, userId, "MEMBER", at);
    }

    protected void addMember(long teamId, long userId, String role, LocalDateTime at) {
        jdbc.update(
                "INSERT INTO team_members (team_id, user_id, role, joined_at) VALUES (?, ?, ?, ?)",
                teamId,
                userId,
                role,
                at);
    }

    protected int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    protected int memberCount(long teamId) {
        return count("SELECT COUNT(*) FROM team_members WHERE team_id = ?", teamId);
    }

    protected boolean isMember(long teamId, long userId) {
        return count(
                        "SELECT COUNT(*) FROM team_members WHERE team_id = ? AND user_id = ?",
                        teamId,
                        userId)
                == 1;
    }

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

    protected ResultActions perform(MockHttpServletRequestBuilder request, Long userId)
            throws Exception {
        return perform(request, userId == null ? null : bearer(userId), null);
    }

    protected ResultActions perform(MockHttpServletRequestBuilder request, long userId, String json)
            throws Exception {
        return perform(request, bearer(userId), json);
    }

    protected ResultActions perform(
            MockHttpServletRequestBuilder request, String authorization, String json)
            throws Exception {
        if (authorization != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(request);
    }

    protected long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
