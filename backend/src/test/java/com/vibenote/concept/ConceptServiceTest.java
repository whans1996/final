package com.vibenote.concept;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** F-09 AC3: LIKE 와일드카드 이스케이프 (SQL은 ESCAPE '!'). */
class ConceptServiceTest {

    @Test
    void AC3_keyword_이스케이프는_느낌표_퍼센트_밑줄을_느낌표로_감싼다() {
        assertThat(ConceptService.escapeLike("jwt")).isEqualTo("jwt");
        assertThat(ConceptService.escapeLike("100%")).isEqualTo("100!%");
        assertThat(ConceptService.escapeLike("a_b")).isEqualTo("a!_b");
        assertThat(ConceptService.escapeLike("c!d")).isEqualTo("c!!d");
        assertThat(ConceptService.escapeLike("!%_")).isEqualTo("!!!%!_");
    }
}
