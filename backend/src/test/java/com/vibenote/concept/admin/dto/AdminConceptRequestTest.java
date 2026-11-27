package com.vibenote.concept.admin.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.analysis.concept.ConceptCategories;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 관리자 카드 검증 regex가 AI 초안 카테고리(A 소유 {@link ConceptCategories})와 어긋나지 않게 고정한다 (F-13 AC5). */
class AdminConceptRequestTest {

    @Test
    void K1_AC5_카테고리_regex는_ConceptCategories_ORDERED와_같은_12종이다() {
        assertThat(List.of(AdminConceptRequest.CATEGORY_REGEX.split("\\|")))
                .containsExactlyElementsOf(ConceptCategories.ORDERED);
        assertThat(Set.of(AdminConceptRequest.CATEGORY_REGEX.split("\\|")))
                .isEqualTo(ConceptCategories.ALL);
    }
}
