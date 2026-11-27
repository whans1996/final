package com.vibenote.common;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 분석 행을 SQL로 직접 넣는 테스트가 붙일 프로젝트를 만든다. P-1a(V15)부터 {@code analyses.project_id}가 NOT NULL이다
 * (docs/specs/P-1a-project-versions.md 4절). 회원을 지우면 프로젝트도 FK CASCADE로 함께 지워진다.
 */
public final class TestProjects {

    private TestProjects() {}

    /** 새 ZIP 프로젝트("p")의 id. 분석 INSERT의 인자로 넘기면 INSERT보다 먼저 실행되므로 뒤의 LAST_INSERT_ID()는 분석 id다. */
    public static long zip(JdbcTemplate jdbc, long userId) {
        jdbc.update(
                "INSERT INTO projects (user_id, source_type, name) VALUES (?, 'ZIP', 'p')", userId);
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM projects WHERE user_id = ?", Long.class, userId);
    }

    /** 그 회원의 저장소 프로젝트 id. 없으면 만든다 (이름은 주소의 마지막 조각). */
    public static long url(JdbcTemplate jdbc, long userId, String sourceUrl) {
        jdbc.update(
                "INSERT IGNORE INTO projects (user_id, source_type, source_url, name)"
                        + " VALUES (?, 'URL', ?, SUBSTRING_INDEX(?, '/', -1))",
                userId,
                sourceUrl,
                sourceUrl);
        return jdbc.queryForObject(
                "SELECT id FROM projects WHERE user_id = ? AND source_url = ?",
                Long.class,
                userId,
                sourceUrl);
    }
}
