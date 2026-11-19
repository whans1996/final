package com.vibenote.concept;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.concept.dto.ConceptSummaryResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** 다른 도메인이 카드를 읽는 공통 조회 (F-00b AC2·AC3). 카드는 테스트 안에서 넣고 롤백한다. */
@Transactional
class ConceptQueryServiceTest extends IntegrationTestBase {

    @Autowired ConceptQueryService conceptQueryService;
    @Autowired JdbcTemplate jdbc;

    @Test
    void AC2_PUBLISHED_카드만_돌려주고_DRAFT_HIDDEN_없는_id는_CONCEPT_404() {
        long published = insertConcept("테스트 공개 카드", "PUBLISHED");
        long draft = insertConcept("테스트 초안 카드", "DRAFT");
        long hidden = insertConcept("테스트 숨김 카드", "HIDDEN");

        assertThat(conceptQueryService.getPublished(published).getName()).isEqualTo("테스트 공개 카드");
        for (long id : List.of(draft, hidden, Long.MAX_VALUE)) {
            assertThatThrownBy(() -> conceptQueryService.getPublished(id))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CONCEPT_404);
        }
    }

    @Test
    void AC3_PUBLISHED만_입력_순서대로_돌려주고_없는_id는_무시한다() {
        long a = insertConcept("테스트 카드 a", "PUBLISHED");
        long b = insertConcept("테스트 카드 b", "PUBLISHED");
        long c = insertConcept("테스트 카드 c", "DRAFT");

        List<ConceptSummaryResponse> result =
                conceptQueryService.findPublishedSummaries(List.of(b, c, Long.MAX_VALUE, a));

        assertThat(result).extracting(ConceptSummaryResponse::id).containsExactly(b, a);
        assertThat(result.get(0).name()).isEqualTo("테스트 카드 b");
    }

    @Test
    void AC3_빈_목록이면_쿼리_없이_빈_목록() {
        ConceptMapper mapper = mock(ConceptMapper.class);

        assertThat(new ConceptQueryService(mapper).findPublishedSummaries(List.of())).isEmpty();
        verifyNoInteractions(mapper);
    }

    private long insertConcept(String name, String status) {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, status, source)"
                        + " VALUES (?, 'java', 'JUNIOR', '요약', ?, 'AI')",
                name,
                status);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
