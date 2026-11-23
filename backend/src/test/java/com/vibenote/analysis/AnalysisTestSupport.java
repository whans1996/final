package com.vibenote.analysis;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.security.JwtTokenProvider;
import com.vibenote.user.UserService;
import com.vibenote.user.dto.SignupRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * F-05a 통합 테스트 공통. 파이프라인은 커밋 뒤에 시작하므로 테스트 트랜잭션 롤백을 쓰지 않고, {@code @f05a.test} 회원을 지워 정리한다
 * (analyses·analysis_files·activities는 users FK CASCADE로 함께 지워진다).
 *
 * <p>테스트 ZIP은 코드로 만든다 (eval 데이터 금지, CLAUDE.md 규칙 11).
 */
abstract class AnalysisTestSupport extends IntegrationTestBase {

    static final ZoneId KST = ZoneId.of("Asia/Seoul");
    static final String PASSWORD = "vibe1234";

    @Autowired UserService userService;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired JdbcTemplate jdbc;

    @Value("${app.upload-dir}")
    String uploadDir;

    @AfterEach
    void cleanUpUsers() {
        jdbc.update("DELETE FROM users WHERE email LIKE '%@f05a.test'");
    }

    long createUser(String name, String level) {
        return userService.signup(new SignupRequest(name + "@f05a.test", PASSWORD, name, level));
    }

    String bearer(long userId) {
        return "Bearer " + tokenProvider.createAccessToken(userId, "USER");
    }

    Path tempZip(long analysisId) {
        return Path.of(uploadDir, "analysis-tmp", analysisId + ".zip");
    }

    /** analyses 행을 직접 넣는다. createdAt이 null이면 지금(KST). */
    long insertAnalysis(
            long userId, String status, LocalDateTime createdAt, boolean deleted, String title) {
        jdbc.update(
                "INSERT INTO analyses (user_id, source_type, project_name, title, level, status,"
                        + " created_at, deleted_at) VALUES (?, 'ZIP', ?, ?, 'BEGINNER', ?, ?,"
                        + " IF(?, NOW(), NULL))",
                userId,
                title,
                title,
                status,
                createdAt == null ? LocalDateTime.now(KST).withNano(0) : createdAt,
                deleted);
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM analyses WHERE user_id = ?", Long.class, userId);
    }

    /** 막 접수된 저장소 URL 분석 행 (F-05b). sourceUrl은 정규화한 주소로 넣는다. */
    long insertUrlAnalysis(long userId, String sourceUrl) {
        jdbc.update(
                "INSERT INTO analyses (user_id, source_type, source_url, project_name, title,"
                    + " level, status, created_at) VALUES (?, 'URL', ?, 'repo', 'repo', 'BEGINNER',"
                    + " 'FETCHING', ?)",
                userId,
                sourceUrl,
                LocalDateTime.now(KST).withNano(0));
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM analyses WHERE user_id = ?", Long.class, userId);
    }

    Map<String, Object> analysisRow(long id) {
        return jdbc.queryForMap("SELECT * FROM analyses WHERE id = ?", id);
    }

    static byte[] zip(Map<String, String> entries) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                ZipEntry entry = new ZipEntry(e.getKey());
                // 시각을 고정한다. 기본값(현재 시각, 2초 단위)이면 두 번 만든 ZIP이 2초 경계를 넘을 때 바이트가 달라진다
                entry.setTime(1_700_000_000_000L);
                out.putNextEntry(entry);
                out.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return bytes.toByteArray();
    }

    static byte[] sampleZip() {
        return zip(
                Map.of(
                        "todo-app/src/main/java/com/example/TodoController.java",
                        "public class TodoController {}"));
    }
}
