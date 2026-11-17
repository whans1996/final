package com.vibenote.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** V1__init.sql이 실제 MySQL 8에 적용되어 ERD의 P0 테이블이 모두 생기는지 확인한다. */
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
}
