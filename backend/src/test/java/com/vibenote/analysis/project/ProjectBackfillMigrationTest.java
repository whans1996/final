package com.vibenote.analysis.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.common.IntegrationTestBase;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * P-1a V15 기존 분석 묶기 (docs/specs/P-1a-project-versions.md AC13). 같은 컨테이너의 별도 스키마에 V14까지 적용하고 행을 넣은 뒤
 * V15를 적용한다. 앱 스키마(vibenote)는 건드리지 않는다.
 */
class ProjectBackfillMigrationTest extends IntegrationTestBase {

    private static final String SCHEMA = "v15_backfill";
    private static final String TODO = "https://github.com/octo/todo-app";
    private static final String SHOP = "https://gitlab.com/group/sub/shop-api";

    private final JdbcTemplate root = new JdbcTemplate(dataSource(""));

    private static DriverManagerDataSource dataSource(String schema) {
        // Testcontainers MySQL은 root 비밀번호를 앱 계정 비밀번호와 같게 둔다
        String url =
                "jdbc:mysql://%s:%d/%s"
                        .formatted(MYSQL.getHost(), MYSQL.getMappedPort(3306), schema);
        return new DriverManagerDataSource(url, "root", MYSQL.getPassword());
    }

    private static Flyway flyway(String target) {
        return Flyway.configure()
                .dataSource(dataSource(SCHEMA))
                .locations("classpath:db/migration")
                .placeholderReplacement(false)
                .target(target)
                .load();
    }

    @AfterEach
    void dropSchema() {
        root.execute("DROP DATABASE IF EXISTS " + SCHEMA);
    }

    @Test
    void AC13_V15는_저장소는_회원과_주소마다_ZIP은_분석마다_프로젝트를_만들고_미삭제_분석마다_버전을_준다() {
        root.execute("DROP DATABASE IF EXISTS " + SCHEMA);
        root.execute("CREATE DATABASE " + SCHEMA);
        flyway("14").migrate();
        JdbcTemplate db = new JdbcTemplate(dataSource(SCHEMA));
        long me = insertUser(db, "me");
        long other = insertUser(db, "other");
        // 요청 순서는 created_at, 같으면 id
        long todo1 = insertUrl(db, me, TODO, "2026-11-20 10:00:00", false);
        long todoDeleted = insertUrl(db, me, TODO, "2026-11-21 10:00:00", true);
        long todo3 = insertUrl(db, me, TODO, "2026-11-22 10:00:00", false);
        long todo2 = insertUrl(db, me, TODO, "2026-11-21 12:00:00", false);
        long shop = insertUrl(db, me, SHOP, "2026-11-20 09:00:00", false);
        long othersTodo = insertUrl(db, other, TODO, "2026-11-20 10:00:00", false);
        long zip = insertZip(db, me, "shop-front", "2026-11-23 10:00:00", false);
        long zipNoName = insertZip(db, me, null, "2026-11-23 11:00:00", true);

        flyway("15").migrate();

        long todoProject = projectOf(db, todo1);
        assertThat(List.of(projectOf(db, todo2), projectOf(db, todo3), projectOf(db, todoDeleted)))
                .containsOnly(todoProject);
        assertThat(project(db, todoProject))
                .containsEntry("user_id", me)
                .containsEntry("source_type", "URL")
                .containsEntry("source_url", TODO)
                .containsEntry("name", "todo-app");
        assertThat(project(db, projectOf(db, shop)))
                .containsEntry("source_url", SHOP)
                .containsEntry("name", "shop-api");
        assertThat(projectOf(db, othersTodo)).isNotEqualTo(todoProject);
        assertThat(project(db, projectOf(db, othersTodo))).containsEntry("user_id", other);
        assertThat(project(db, projectOf(db, zip)))
                .containsEntry("source_type", "ZIP")
                .containsEntry("name", "shop-front")
                .containsEntry("source_url", null);
        assertThat(project(db, projectOf(db, zipNoName))).containsEntry("name", "project");
        assertThat(projectOf(db, zipNoName)).isNotEqualTo(projectOf(db, zip));

        assertThat(versionNumber(db, todo1)).isEqualTo(1);
        assertThat(versionNumber(db, todo2)).isEqualTo(2);
        assertThat(versionNumber(db, todo3)).isEqualTo(3);
        assertThat(versionId(db, todoDeleted)).isNull();
        assertThat(versionId(db, zipNoName)).isNull();
        assertThat(versionNumber(db, shop)).isEqualTo(1);
        assertThat(versionNumber(db, othersTodo)).isEqualTo(1);
        assertThat(versionNumber(db, zip)).isEqualTo(1);
        assertThat(
                        db.queryForObject(
                                "SELECT COUNT(*) FROM project_versions WHERE commit_sha IS NOT NULL"
                                    + " OR commit_message IS NOT NULL OR committed_at IS NOT NULL",
                                Integer.class))
                .isZero();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM project_versions", Integer.class))
                .isEqualTo(6);

        assertThat(
                        db.queryForObject(
                                "SELECT IS_NULLABLE FROM information_schema.COLUMNS"
                                        + " WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'analyses'"
                                        + " AND COLUMN_NAME = 'project_id'",
                                String.class,
                                SCHEMA))
                .isEqualTo("NO");
        assertThat(
                        db.queryForObject(
                                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA"
                                        + " = ? AND COLUMN_NAME = 'legacy_analysis_id'",
                                Integer.class,
                                SCHEMA))
                .isZero();
    }

    private static long insertUser(JdbcTemplate db, String name) {
        db.update(
                "INSERT INTO users (email, password_hash, nickname, level)"
                        + " VALUES (?, 'x', ?, 'JUNIOR')",
                name + "@v15.test",
                name);
        return db.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, name + "@v15.test");
    }

    private static long insertUrl(
            JdbcTemplate db, long userId, String url, String createdAt, boolean deleted) {
        db.update(
                "INSERT INTO analyses (user_id, source_type, source_url, project_name, title,"
                        + " level, status, created_at, deleted_at) VALUES (?, 'URL', ?, 'p', 'p',"
                        + " 'JUNIOR', 'DONE', ?, IF(?, NOW(), NULL))",
                userId,
                url,
                createdAt,
                deleted);
        // DriverManagerDataSource는 문장마다 새 연결이라 LAST_INSERT_ID()를 쓸 수 없다
        return db.queryForObject("SELECT MAX(id) FROM analyses", Long.class);
    }

    private static long insertZip(
            JdbcTemplate db, long userId, String projectName, String createdAt, boolean deleted) {
        db.update(
                "INSERT INTO analyses (user_id, source_type, project_name, title, level, status,"
                        + " created_at, deleted_at)"
                        + " VALUES (?, 'ZIP', ?, 'p', 'JUNIOR', 'FAILED', ?, IF(?, NOW(), NULL))",
                userId,
                projectName,
                createdAt,
                deleted);
        return db.queryForObject("SELECT MAX(id) FROM analyses", Long.class);
    }

    private static long projectOf(JdbcTemplate db, long analysisId) {
        return db.queryForObject(
                "SELECT project_id FROM analyses WHERE id = ?", Long.class, analysisId);
    }

    private static Map<String, Object> project(JdbcTemplate db, long projectId) {
        return db.queryForMap(
                "SELECT user_id, source_type, source_url, name FROM projects WHERE id = ?",
                projectId);
    }

    private static Long versionId(JdbcTemplate db, long analysisId) {
        return db.queryForObject(
                "SELECT project_version_id FROM analyses WHERE id = ?", Long.class, analysisId);
    }

    private static Integer versionNumber(JdbcTemplate db, long analysisId) {
        return db.queryForObject(
                "SELECT v.number FROM analyses a JOIN project_versions v"
                        + " ON v.id = a.project_version_id AND v.project_id = a.project_id"
                        + " WHERE a.id = ?",
                Integer.class,
                analysisId);
    }
}
