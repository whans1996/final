package com.vibenote.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.common.IntegrationTestBase;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * L-1a V14 기존 행 채우기 (docs/specs/L-1a-rerun-quiz-regenerate.md AC4). 같은 컨테이너의 별도 스키마에 V13까지 적용하고 행을
 * 넣은 뒤 V14를 적용한다. 앱 스키마(vibenote)는 건드리지 않는다.
 */
class V14QuizStatusBackfillTest extends IntegrationTestBase {

    private static final String SCHEMA = "v14_backfill";

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
    void AC4_V14는_퀴즈가_있으면_READY_DONE이면_FAILED_나머지는_NULL로_채운다() {
        root.execute("DROP DATABASE IF EXISTS " + SCHEMA);
        root.execute("CREATE DATABASE " + SCHEMA);
        flyway("13").migrate();
        JdbcTemplate db = new JdbcTemplate(dataSource(SCHEMA));
        db.update(
                "INSERT INTO users (email, password_hash, nickname, level)"
                        + " VALUES ('v14@test', 'x', 'v14', 'JUNIOR')");
        long userId =
                db.queryForObject("SELECT id FROM users WHERE email = 'v14@test'", Long.class);
        long withQuiz = insertAnalysis(db, userId, "DONE");
        db.update("INSERT INTO quizzes (analysis_id, created_at) VALUES (?, NOW())", withQuiz);
        long doneNoQuiz = insertAnalysis(db, userId, "DONE");
        long failed = insertAnalysis(db, userId, "FAILED");
        long running = insertAnalysis(db, userId, "EXPLAINING");

        flyway("14").migrate();

        assertThat(quizStatus(db, withQuiz)).isEqualTo("READY");
        assertThat(quizStatus(db, doneNoQuiz)).isEqualTo("FAILED");
        assertThat(quizStatus(db, failed)).isNull();
        assertThat(quizStatus(db, running)).isNull();
        assertThat(
                        db.queryForObject(
                                "SELECT COUNT(*) FROM analyses WHERE quiz_regenerations <> 0",
                                Integer.class))
                .isZero();
    }

    private static long insertAnalysis(JdbcTemplate db, long userId, String status) {
        db.update(
                "INSERT INTO analyses (user_id, source_type, project_name, title, level, status)"
                        + " VALUES (?, 'ZIP', 'p', 'p', 'JUNIOR', ?)",
                userId,
                status);
        // DriverManagerDataSource는 문장마다 새 연결이라 LAST_INSERT_ID()를 쓸 수 없다
        return db.queryForObject("SELECT MAX(id) FROM analyses", Long.class);
    }

    private static String quizStatus(JdbcTemplate db, long id) {
        return db.queryForObject("SELECT quiz_status FROM analyses WHERE id = ?", String.class, id);
    }
}
