package com.vibenote.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 시드 개념 카드(DS-05, {@code R__seed_concepts.sql})가 데이터셋 규칙대로 들어갔는지 확인한다.
 *
 * <p>출처는 DS-01(split {@code content}) 도메인만 허용한다. 카테고리는 F-09 필터 값과 같은 소문자 6종이다.
 */
class SeedConceptsTest extends IntegrationTestBase {

    private static final Set<String> DS01_HOSTS =
            Set.of(
                    "docs.spring.io",
                    "mybatis.org",
                    "ko.vuejs.org",
                    "pinia.vuejs.org",
                    "router.vuejs.org",
                    "developer.mozilla.org",
                    "springdoc.org",
                    "docs.oracle.com");

    private static final Set<String> CATEGORIES =
            Set.of("java", "spring", "security", "mybatis", "web", "vue");

    @Autowired JdbcTemplate jdbc;

    @Test
    void 시드_카드가_30개_이상_50개_이하다() {
        Integer count =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM concepts WHERE source = 'SEED'", Integer.class);

        assertThat(count).isBetween(30, 50);
    }

    @Test
    void 시드_카드는_모두_PUBLISHED이고_출처가_DS01_도메인이다() {
        List<String> statuses =
                jdbc.queryForList(
                        "SELECT DISTINCT status FROM concepts WHERE source = 'SEED'", String.class);
        List<String> urls =
                jdbc.queryForList(
                        "SELECT reference_url FROM concepts WHERE source = 'SEED'", String.class);

        assertThat(statuses).containsExactly("PUBLISHED");
        assertThat(urls)
                .isNotEmpty()
                .allSatisfy(
                        url -> {
                            assertThat(url).startsWith("https://");
                            assertThat(DS01_HOSTS).contains(URI.create(url).getHost());
                        });
    }

    @Test
    void 카테고리는_6종_난이도는_3단계가_모두_있다() {
        List<String> categories =
                jdbc.queryForList(
                        "SELECT DISTINCT category FROM concepts WHERE source = 'SEED'",
                        String.class);
        List<String> difficulties =
                jdbc.queryForList(
                        "SELECT DISTINCT difficulty FROM concepts WHERE source = 'SEED'",
                        String.class);

        assertThat(categories).containsExactlyInAnyOrderElementsOf(CATEGORIES);
        assertThat(difficulties).containsExactlyInAnyOrder("BEGINNER", "JUNIOR", "SENIOR");
    }

    @Test
    void summary와_body와_예제_코드가_비어있지_않다() {
        Integer blank =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM concepts WHERE source = 'SEED' AND (TRIM(summary) ="
                            + " '' OR body IS NULL OR TRIM(body) = '' OR example_code IS NULL OR"
                            + " TRIM(example_code) = '')",
                        Integer.class);

        assertThat(blank).isZero();
    }
}
