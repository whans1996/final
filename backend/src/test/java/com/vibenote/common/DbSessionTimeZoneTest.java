package com.vibenote.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.user.UserService;
import com.vibenote.user.dto.SignupRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB 기본값(CURRENT_TIMESTAMP)·NOW()가 MySQL 서버 시간대와 상관없이 Asia/Seoul 시각이 되는지 (#17). 테스트 컨테이너는 UTC로 뜬다.
 */
@Transactional
class DbSessionTimeZoneTest extends IntegrationTestBase {

    private static final Duration TOLERANCE = Duration.ofSeconds(60);

    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;
    @Autowired UserService userService;

    @Test
    void 세션_시간대가_서버_기본값과_무관하게_KST() {
        String zone = jdbc.queryForObject("SELECT @@session.time_zone", String.class);

        assertThat(zone).isEqualTo("+09:00");
    }

    @Test
    void DB_NOW가_서비스_Clock과_같은_시각() {
        LocalDateTime dbNow = jdbc.queryForObject("SELECT NOW()", LocalDateTime.class);

        assertCloseToClockNow(dbNow);
    }

    @Test
    void 기본값_created_at이_KST로_들어간다() {
        long userId =
                userService.signup(
                        new SignupRequest("tz@vibenote.dev", "vibe1234", "시간대", "BEGINNER"));

        LocalDateTime createdAt =
                jdbc.queryForObject(
                        "SELECT created_at FROM users WHERE id = ?", LocalDateTime.class, userId);

        assertCloseToClockNow(createdAt);
    }

    private void assertCloseToClockNow(LocalDateTime actual) {
        Duration diff = Duration.between(LocalDateTime.now(clock), actual).abs();
        assertThat(diff).isLessThan(TOLERANCE);
    }
}
