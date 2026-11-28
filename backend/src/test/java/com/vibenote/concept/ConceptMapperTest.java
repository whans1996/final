package com.vibenote.concept;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.Level;
import com.vibenote.concept.domain.Concept;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** concepts 행 매핑 (F-00b AC1). 시드 카드(R__seed_concepts.sql)를 그대로 읽는다. */
class ConceptMapperTest extends IntegrationTestBase {

    @Autowired ConceptMapper conceptMapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void AC1_시드_카드의_모든_컬럼을_매핑한다() {
        Map<String, Object> row =
                jdbc.queryForMap(
                        "SELECT * FROM concepts WHERE source = 'SEED' AND body IS NOT NULL"
                                + " AND example_code IS NOT NULL ORDER BY id LIMIT 1");
        long id = ((Number) row.get("id")).longValue();

        Concept concept = conceptMapper.findById(id).orElseThrow();

        assertThat(concept.getId()).isEqualTo(id);
        assertThat(concept.getName()).isEqualTo(row.get("name"));
        assertThat(concept.getCategory()).isEqualTo(row.get("category"));
        assertThat(concept.getDifficulty())
                .isEqualTo(Level.valueOf((String) row.get("difficulty")));
        assertThat(concept.getSummary()).isEqualTo(row.get("summary"));
        assertThat(concept.getBody()).isEqualTo(row.get("body"));
        assertThat(concept.getExampleCode()).isEqualTo(row.get("example_code"));
        assertThat(concept.getStatus()).isEqualTo(ConceptStatus.PUBLISHED);
        assertThat(concept.getSource()).isEqualTo(ConceptSource.SEED);
        assertThat(concept.getReferenceUrl()).isEqualTo(row.get("reference_url"));
        assertThat(concept.getWishCount()).isEqualTo(((Number) row.get("wish_count")).intValue());
        assertThat(concept.getAvgRating()).isEqualByComparingTo((BigDecimal) row.get("avg_rating"));
        assertThat(concept.getCreatedAt()).isEqualTo((LocalDateTime) row.get("created_at"));
        assertThat(concept.getUpdatedAt()).isEqualTo((LocalDateTime) row.get("updated_at"));
    }

    @Test
    void AC1_없는_id면_빈_Optional() {
        assertThat(conceptMapper.findById(Long.MAX_VALUE)).isEmpty();
    }

    /** K-1 AC1: 수준별 요약·본문 6칸을 읽고 관리자 저장으로 쓴다. */
    @Test
    @org.springframework.transaction.annotation.Transactional
    void K1_AC1_수준별_요약_본문_6칸을_읽고_쓴다() {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, summary_junior,"
                        + " body_senior, status, source) VALUES ('k1-mapper', 'cs', 'JUNIOR', '요약',"
                        + " '주니어 요약', '시니어 본문', 'DRAFT', 'AI')");
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        Concept concept = conceptMapper.findById(id).orElseThrow();
        assertThat(concept.summaryFor(Level.JUNIOR)).isEqualTo("주니어 요약");
        assertThat(concept.bodyFor(Level.SENIOR)).isEqualTo("시니어 본문");
        assertThat(concept.summaryFor(Level.BEGINNER)).isNull();

        concept.setLevelText(Level.BEGINNER, "입문 요약", "입문 본문");
        concept.setLevelText(Level.SENIOR, null, null);
        conceptMapper.updateByAdmin(concept);

        assertThat(
                        jdbc.queryForMap(
                                "SELECT summary_beginner, body_beginner, summary_junior,"
                                        + " body_senior FROM concepts WHERE id = ?",
                                id))
                .containsEntry("summary_beginner", "입문 요약")
                .containsEntry("body_beginner", "입문 본문")
                .containsEntry("summary_junior", "주니어 요약")
                .containsEntry("body_senior", null);
    }
}
