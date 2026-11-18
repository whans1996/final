package com.vibenote.user;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.security.JwtTokenProvider;
import com.vibenote.user.dto.SignupRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** F-03 통합 테스트 공통: 회원 만들기, Access 토큰 헤더, 행 삽입 후 id 얻기. */
@Transactional
abstract class UserTestSupport extends IntegrationTestBase {

    static final String PASSWORD = "vibe1234";

    @Autowired UserService userService;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired JdbcTemplate jdbc;

    long createUser(String email, String nickname) {
        return userService.signup(new SignupRequest(email, PASSWORD, nickname, "BEGINNER"));
    }

    String bearer(long userId) {
        return "Bearer " + tokenProvider.createAccessToken(userId, "USER");
    }

    /** INSERT를 실행하고 생성된 id를 돌려준다 (테스트 트랜잭션이 한 커넥션을 쓰므로 LAST_INSERT_ID가 안전하다). */
    long insert(String sql, Object... args) {
        jdbc.update(sql, args);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    String column(String column, long userId) {
        return jdbc.queryForObject(
                "SELECT " + column + " FROM users WHERE id = ?", String.class, userId);
    }
}
