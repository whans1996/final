package com.vibenote.concept.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.common.Level;
import com.vibenote.concept.ConceptStatus;
import com.vibenote.concept.domain.Concept;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 카드 요약 응답의 JSON 모양 (F-00b AC4). F-09·F-12·F-15·F-16이 같은 모양을 쓴다. */
class ConceptSummaryResponseTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void AC4_요약_필드만_나오고_avgRating은_숫자다() {
        Concept concept = new Concept();
        concept.setId(12L);
        concept.setName("JWT 인증");
        concept.setCategory("security");
        concept.setDifficulty(Level.JUNIOR);
        concept.setSummary("토큰으로 사람을 확인한다.");
        concept.setBody("본문");
        concept.setExampleCode("String token = jwt.sign(claims);");
        concept.setStatus(ConceptStatus.PUBLISHED);
        concept.setAvgRating(new BigDecimal("4.5"));
        concept.setWishCount(12);

        JsonNode json =
                mapper.readTree(mapper.writeValueAsString(ConceptSummaryResponse.from(concept)));

        assertThat(json.propertyNames())
                .containsExactlyInAnyOrder(
                        "id",
                        "name",
                        "category",
                        "difficulty",
                        "summary",
                        "avgRating",
                        "wishCount");
        assertThat(json.get("avgRating").isNumber()).isTrue();
        assertThat(json.get("avgRating").asDouble()).isEqualTo(4.5);
        assertThat(json.get("wishCount").asInt()).isEqualTo(12);
        assertThat(json.get("difficulty").asString()).isEqualTo("JUNIOR");
    }
}
