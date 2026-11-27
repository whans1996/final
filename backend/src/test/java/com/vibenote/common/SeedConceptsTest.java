package com.vibenote.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.analysis.concept.ConceptCategories;
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
 * <p>출처는 DS-01(split {@code content}) 도메인만 허용한다. 카테고리는 12종({@code ConceptCategories.ALL}, K-1) 안에
 * 있다.
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
    void K1_AC1_카테고리는_12종_안에_있고_난이도는_3단계가_모두_있다() {
        List<String> categories =
                jdbc.queryForList(
                        "SELECT DISTINCT category FROM concepts WHERE source = 'SEED'",
                        String.class);
        List<String> difficulties =
                jdbc.queryForList(
                        "SELECT DISTINCT difficulty FROM concepts WHERE source = 'SEED'",
                        String.class);

        assertThat(categories).isNotEmpty().allMatch(ConceptCategories.ALL::contains);
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
     * K-3a AC1: 생성기(seed-build.mjs)가 작은따옴표와 역슬래시를 이스케이프해서 DB 값이 카드 원본과 같다. 픽스처 SQL은 {@code
     * seed/cards}의 카드로 만든 생성본이고, 하네스 테스트(seed-build.test.mjs)가 생성기 출력과 같은지 확인한다.
     */
    @Test
    @Transactional
    void AC1_역슬래시와_작은따옴표가_원본_그대로_들어간다() throws IOException {
        String sql =
                new ClassPathResource("seed/escape-seed.sql")
                        .getContentAsString(StandardCharsets.UTF_8)
                        .strip();
        jdbc.execute(sql.endsWith(";") ? sql.substring(0, sql.length() - 1) : sql);

        assertThat(
                        jdbc.queryForMap(
                                "SELECT summary, body, example_code FROM concepts WHERE name = ?",
                                "이스케이프 검사 카드 (테스트 전용)"))
                .containsEntry("summary", "It's 테스트 전용 카드다. 정규식 '\\d+'와 경로 C:\\temp가 그대로 들어가야 한다.")
                .containsEntry("body", "- 작은따옴표: It's, 'quoted'\n- 역슬래시: \\d+ \\\\ \\n \\' 끝\\")
                .containsEntry(
                        "example_code",
                        "Pattern digits = Pattern.compile(\"\\\\d+\");\n"
                                + "String path = \"C:\\\\temp\\\\It's\";");
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

    /**
     * F-13 AC12: AI가 먼저 DRAFT 초안으로 만든 이름이 나중에 시드 파일에 들어오면, 시드 카드로서 파일의 PUBLISHED가 된다. 관리자가 정한
     * PUBLISHED·HIDDEN만 유지하고 검수 전 초안은 승격한다 (리뷰 권장 반영).
     */
    @Test
    @Transactional
    void AC12_시드에_새로_들어온_AI_초안_이름은_PUBLISHED_SEED가_된다() throws IOException {
        String name = "예외 처리와 커스텀 예외";
        jdbc.update("UPDATE concepts SET status = 'DRAFT', source = 'AI' WHERE name = ?", name);

        String sql =
                new ClassPathResource("db/migration/R__seed_concepts.sql")
                        .getContentAsString(StandardCharsets.UTF_8)
                        .strip();
        jdbc.execute(sql.endsWith(";") ? sql.substring(0, sql.length() - 1) : sql);

        assertThat(jdbc.queryForMap("SELECT status, source FROM concepts WHERE name = ?", name))
                .containsEntry("status", "PUBLISHED")
                .containsEntry("source", "SEED");
    }

    /** K-1 AC1: V16의 수준별 칸 6개. 기존 시드 카드는 수준별 절이 없어 모두 NULL이다 (내용은 K-3c). */
    @Test
    void K1_AC1_수준별_요약_본문_칸이_있고_기존_시드_카드는_모두_NULL이다() {
        List<java.util.Map<String, Object>> columns =
                jdbc.queryForList(
                        "SELECT column_name AS name, data_type AS type, character_maximum_length AS"
                                + " len, is_nullable AS nullable FROM information_schema.columns"
                                + " WHERE table_schema = DATABASE() AND table_name = 'concepts' AND"
                                + " (column_name LIKE 'summary\\_%' OR column_name LIKE 'body\\_%')"
                                + " ORDER BY ordinal_position");

        assertThat(columns)
                .extracting(c -> c.get("name") + " " + c.get("type") + " " + c.get("nullable"))
                .containsExactly(
                        "summary_beginner varchar YES",
                        "summary_junior varchar YES",
                        "summary_senior varchar YES",
                        "body_beginner text YES",
                        "body_junior text YES",
                        "body_senior text YES");
        assertThat(columns.subList(0, 3))
                .allSatisfy(c -> assertThat(((Number) c.get("len")).intValue()).isEqualTo(300));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM concepts WHERE source = 'SEED' AND"
                                    + " (summary_beginner IS NOT NULL OR summary_junior IS NOT NULL"
                                    + " OR summary_senior IS NOT NULL OR body_beginner IS NOT NULL"
                                    + " OR body_junior IS NOT NULL OR body_senior IS NOT NULL)",
                                Integer.class))
                .isZero();
    }
}
