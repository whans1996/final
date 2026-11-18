package com.vibenote.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 마이그레이션(V1, V2)이 실제 MySQL 8에 적용되어 ERD의 테이블과 데이터 정책 제약이 동작하는지 확인한다.
 *
 * <p>각 테스트는 트랜잭션 롤백으로 데이터를 남기지 않는다.
 */
@Transactional
class FlywayMigrationTest extends IntegrationTestBase {

    @Autowired JdbcTemplate jdbc;

    @Test
    void ERD의_P0_테이블이_모두_생성된다() {
        List<String> tables =
                jdbc.queryForList(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema ="
                                + " DATABASE() AND table_name <> 'flyway_schema_history'",
                        String.class);

        assertThat(tables)
                .containsExactlyInAnyOrder(
                        "users",
                        "refresh_tokens",
                        "follows",
                        "activities",
                        "concepts",
                        "concept_reviews",
                        "concept_wishes",
                        "analyses",
                        "analysis_files",
                        "analysis_flows",
                        "analysis_concepts",
                        "quizzes",
                        "quiz_questions",
                        "quiz_attempts",
                        "quiz_answers",
                        "review_plans",
                        "recommendation_reasons",
                        "posts",
                        "comments",
                        "challenges",
                        "challenge_participants",
                        "llm_usage_logs");
    }

    @Test
    void 개념_카드에_공식_문서_출처_컬럼이_있다() {
        Integer count =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema ="
                                + " DATABASE() AND table_name = 'concepts' AND column_name ="
                                + " 'reference_url'",
                        Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void 분석을_soft_delete해도_퀴즈_풀이_기록은_남는다() {
        long userId = insertUser("solver");
        long conceptId = insertConcept("test-트랜잭션");
        jdbc.update(
                "INSERT INTO analyses (user_id, source_type, level) VALUES (?, 'ZIP', 'BEGINNER')",
                userId);
        long analysisId = lastId();
        jdbc.update("INSERT INTO quizzes (analysis_id) VALUES (?)", analysisId);
        long quizId = lastId();
        jdbc.update(
                "INSERT INTO quiz_questions (quiz_id, concept_id, question, choices, answer_index)"
                        + " VALUES (?, ?, '질문', JSON_ARRAY('a', 'b'), 0)",
                quizId,
                conceptId);
        long questionId = lastId();
        jdbc.update(
                "INSERT INTO quiz_attempts (quiz_id, user_id, score, total) VALUES (?, ?, 0, 1)",
                quizId,
                userId);
        long attemptId = lastId();
        jdbc.update(
                "INSERT INTO quiz_answers (attempt_id, question_id, selected_index, is_correct)"
                        + " VALUES (?, ?, 1, FALSE)",
                attemptId,
                questionId);

        jdbc.update("UPDATE analyses SET deleted_at = NOW() WHERE id = ?", analysisId);

        assertThat(count("SELECT COUNT(*) FROM quiz_attempts WHERE user_id = ?", userId))
                .isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM quiz_answers WHERE attempt_id = ?", attemptId))
                .isEqualTo(1);
    }

    @Test
    void 같은_날_같은_카드의_복습_계획은_하나만_만들_수_있다() {
        long userId = insertUser("planner");
        long conceptId = insertConcept("test-인덱스");
        String sql =
                "INSERT INTO review_plans (user_id, concept_id, plan_date, reason)"
                        + " VALUES (?, ?, '2026-11-20', ?)";
        jdbc.update(sql, userId, conceptId, "WRONG");

        assertThatThrownBy(() -> jdbc.update(sql, userId, conceptId, "WISH"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void 카드_리뷰는_사용자당_하나만_쓸_수_있다() {
        long userId = insertUser("reviewer");
        long conceptId = insertConcept("test-JWT");
        String sql =
                "INSERT INTO concept_reviews (concept_id, user_id, rating, content)"
                        + " VALUES (?, ?, 5, '좋아요')";
        jdbc.update(sql, conceptId, userId);

        assertThatThrownBy(() -> jdbc.update(sql, conceptId, userId))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void 글을_쓴_회원의_행은_실제로_삭제되지_않는다() {
        long userId = insertUser("writer");
        jdbc.update(
                "INSERT INTO posts (user_id, board_type, title, content)"
                        + " VALUES (?, 'FREE', '제목', '내용')",
                userId);

        assertThatThrownBy(() -> jdbc.update("DELETE FROM users WHERE id = ?", userId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 챌린지_활동은_참여와_달성을_구분한다() {
        long userId = insertUser("challenger");
        String sql = "INSERT INTO activities (user_id, type, target_id) VALUES (?, ?, 1)";
        jdbc.update(sql, userId, "CHALLENGE_JOIN");
        jdbc.update(sql, userId, "CHALLENGE_COMPLETE");

        assertThatThrownBy(() -> jdbc.update(sql, userId, "CHALLENGE"))
                .isInstanceOf(DataAccessException.class);
    }

    private long insertUser(String nickname) {
        jdbc.update(
                "INSERT INTO users (email, password_hash, nickname) VALUES (?, 'hash', ?)",
                nickname + "@test.vibenote",
                nickname);
        return lastId();
    }

    private long insertConcept(String name) {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, source)"
                        + " VALUES (?, 'spring', 'BEGINNER', '요약', 'SEED')",
                name);
        return lastId();
    }

    private long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }
}
