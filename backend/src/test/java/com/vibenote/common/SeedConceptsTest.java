package com.vibenote.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

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
    void 시드_카드가_80개_이상이고_파일의_행이_모두_들어갔다() throws IOException {
        String sql =
                new ClassPathResource("db/migration/R__seed_concepts.sql")
                        .getContentAsString(StandardCharsets.UTF_8);
        int rowsInFile = sql.split(Pattern.quote("'PUBLISHED', 'SEED', 'https"), -1).length - 1;
        Integer count =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM concepts WHERE source = 'SEED'", Integer.class);

        // 이름이 겹치면(콜레이션상 대소문자·악센트만 달라도) 한 INSERT 안에서 앞 행이 조용히 덮어써진다
        assertThat(count).isGreaterThanOrEqualTo(80).isEqualTo(rowsInFile);
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

    /**
     * F-13 AC12: 관리자가 숨긴 시드 카드는 시드 파일이 다시 적용돼도 HIDDEN 그대로이고, 내용은 파일 기준으로 덮어써진다. Flyway처럼 파일 전체를 한
     * 문장으로 실행한다 (끝의 세미콜론만 뗀다).
     */
    @Test
    @Transactional
    void AC12_시드_재적용해도_숨긴_카드는_HIDDEN() throws IOException {
        String name = "예외 처리와 커스텀 예외";
        String fileSummary =
                jdbc.queryForObject(
                        "SELECT summary FROM concepts WHERE name = ?", String.class, name);
        jdbc.update(
                "UPDATE concepts SET status = 'HIDDEN', summary = '관리자가 고친 요약' WHERE name = ?",
                name);

        String sql =
                new ClassPathResource("db/migration/R__seed_concepts.sql")
                        .getContentAsString(StandardCharsets.UTF_8)
                        .strip();
        jdbc.execute(sql.endsWith(";") ? sql.substring(0, sql.length() - 1) : sql);

        assertThat(jdbc.queryForMap("SELECT status, summary FROM concepts WHERE name = ?", name))
                .containsEntry("status", "HIDDEN")
                .containsEntry("summary", fileSummary);
    }
}
