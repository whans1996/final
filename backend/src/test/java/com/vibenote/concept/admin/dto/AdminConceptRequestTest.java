package com.vibenote.concept.admin.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.analysis.concept.ConceptCategories;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 관리자 카드 검증 regex가 AI 초안 카테고리(A 소유 {@link ConceptCategories})와 어긋나지 않게 고정한다 (F-13 AC5). */
class AdminConceptRequestTest {

    @Test
    void AC5_카테고리_regex는_ConceptCategories_ALL과_같다() {
        assertThat(Set.of(AdminConceptRequest.CATEGORY_REGEX.split("\\|")))
                .isEqualTo(ConceptCategories.ALL);
    }
}
