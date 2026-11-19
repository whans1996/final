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
}
