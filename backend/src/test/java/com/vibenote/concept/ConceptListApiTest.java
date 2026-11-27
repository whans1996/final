package com.vibenote.concept;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.security.JwtTokenProvider;
import java.time.LocalDateTime;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * F-09 개념 카드 목록·검색 (docs/specs/F-09-concept-list.md AC1~AC8). 시드 개수에 기대지 않도록 테스트가 고유 토큰을 넣은 카드를 직접
 * 만들고, 그 토큰을 keyword로 걸어 확인한다.
 */
@Transactional
class ConceptListApiTest extends IntegrationTestBase {

    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenProvider tokenProvider;

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 1, 0, 0);

    private record Card(
            String name,
            String category,
            String difficulty,
            String summary,
            String body,
            String status,
            int wishCount,
            String avgRating,
            LocalDateTime createdAt) {

        static Card published(String name) {
            return new Card(name, "java", "BEGINNER", "요약", null, "PUBLISHED", 0, "0.0", BASE);
        }

        Card category(String v) {
            return new Card(
                    name, v, difficulty, summary, body, status, wishCount, avgRating, createdAt);
        }

        Card difficulty(String v) {
            return new Card(
                    name, category, v, summary, body, status, wishCount, avgRating, createdAt);
        }

        Card summary(String v) {
            return new Card(
                    name, category, difficulty, v, body, status, wishCount, avgRating, createdAt);
        }

        Card body(String v) {
            return new Card(
                    name,
                    category,
                    difficulty,
                    summary,
                    v,
                    status,
                    wishCount,
                    avgRating,
                    createdAt);
        }

        Card status(String v) {
            return new Card(
                    name, category, difficulty, summary, body, v, wishCount, avgRating, createdAt);
        }

        Card popularity(int wish, String rating) {
            return new Card(
                    name, category, difficulty, summary, body, status, wish, rating, createdAt);
        }

        Card createdAt(LocalDateTime v) {
            return new Card(
                    name, category, difficulty, summary, body, status, wishCount, avgRating, v);
        }
    }

    private long insert(Card c) {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, body, status, source,"
                    + " wish_count, avg_rating, created_at) VALUES (?, ?, ?, ?, ?, ?, 'AI', ?, ?,"
                    + " ?)",
                c.name(),
                c.category(),
                c.difficulty(),
                c.summary(),
                c.body(),
                c.status(),
                c.wishCount(),
                c.avgRating(),
                c.createdAt());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /** {@code "a=b&c=d"}를 디코딩하지 않고 그대로 파라미터로 보낸다 (%·공백 리터럴 확인용). */
    private ResultActions list(String query) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/concepts");
        if (!query.isEmpty()) {
            for (String pair : query.split("&")) {
                int eq = pair.indexOf('=');
                request.param(pair.substring(0, eq), pair.substring(eq + 1));
            }
        }
        return mockMvc.perform(request);
    }

    private long publishedCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM concepts WHERE status = 'PUBLISHED'", Long.class);
    }

    // ── AC1 ──

    @Test
    void AC1_파라미터_없이_요청하면_공개_카드를_인기순으로_받는다() throws Exception {
        long id =
                insert(
                        Card.published("zqa1 인기 카드")
                                .category("security")
                                .difficulty("SENIOR")
                                .summary("zqa1 요약")
                                .body("본문은 목록에 없다")
                                .popularity(999_999, "4.5"));

        list("").andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total").value(publishedCount()))
                .andExpect(jsonPath("$.items", hasSize(20)))
                .andExpect(jsonPath("$.items[0].id").value(id))
                .andExpect(jsonPath("$.items[0].name").value("zqa1 인기 카드"))
                .andExpect(jsonPath("$.items[0].category").value("security"))
                .andExpect(jsonPath("$.items[0].difficulty").value("SENIOR"))
                .andExpect(jsonPath("$.items[0].summary").value("zqa1 요약"))
                .andExpect(jsonPath("$.items[0].wishCount").value(999_999))
                .andExpect(jsonPath("$.items[0].avgRating").value(4.5))
                .andExpect(jsonPath("$.items[0].body").doesNotExist())
                .andExpect(jsonPath("$.items[0].exampleCode").doesNotExist())
                .andExpect(jsonPath("$.items[0].status").doesNotExist());
    }

    @Test
    void AC1_로그인_토큰이_있어도_결과가_같다() throws Exception {
        long id = insert(Card.published("zqa1b 카드").popularity(999_999, "0.0"));

        mockMvc.perform(
                        get("/api/concepts")
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenProvider.createAccessToken(1L, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(publishedCount()))
                .andExpect(jsonPath("$.items[0].id").value(id))
                .andExpect(jsonPath("$.items[0].avgRating").value(0.0));
    }

    @Test
    void AC1_공개는_GET_목록뿐이고_POST는_토큰_없으면_401() throws Exception {
        // SecurityConfig의 공개 목록이 "/api/concepts/**"나 메서드 무관으로 넓어지면 여기서 잡힌다.
        mockMvc.perform(post("/api/concepts").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    // ── AC2 ──

    @Test
    void AC2_DRAFT와_HIDDEN은_어떤_조건에서도_items와_total에_없다() throws Exception {
        insert(Card.published("zqa2 공개").category("web"));
        insert(Card.published("zqa2 초안").category("web").status("DRAFT").popularity(9, "5.0"));
        insert(Card.published("zqa2 숨김").category("web").status("HIDDEN").popularity(9, "5.0"));

        for (String q :
                new String[] {
                    "keyword=zqa2",
                    "keyword=zqa2&sort=LATEST",
                    "keyword=zqa2&category=web&difficulty=BEGINNER"
                }) {
            list(q).andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(1))
                    .andExpect(jsonPath("$.items[*].name", contains("zqa2 공개")));
        }
        list("").andExpect(jsonPath("$.total").value(publishedCount()));
    }

    // ── AC3 ──

    @Test
    void AC3_이름이나_요약에_부분_일치하는_카드만_받고_본문만_일치하면_제외한다() throws Exception {
        insert(Card.published("이름 zqa3tok 일치").popularity(3, "0.0"));
        insert(Card.published("요약 일치").summary("요약에 zqa3tok가 있다").popularity(2, "0.0"));
        insert(Card.published("본문만 일치").body("본문에만 zqa3tok").popularity(1, "0.0"));

        list("keyword=zqa3tok")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[*].name", contains("이름 zqa3tok 일치", "요약 일치")));
    }

    @Test
    void AC3_대소문자를_무시하고_앞뒤_공백은_trim한다() throws Exception {
        insert(Card.published("ZQA3CASE 카드"));

        list("keyword=  zqa3case  ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("ZQA3CASE 카드"));
    }

    @Test
    void AC3_trim_후_빈_문자열이면_키워드_조건이_없다() throws Exception {
        list("keyword=   ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(publishedCount()));
    }

    @Test
    void AC3_퍼센트_밑줄_느낌표는_글자_그대로_찾는다() throws Exception {
        insert(Card.published("zqa3 100% 할인"));
        insert(Card.published("zqa3 1000 할인"));
        insert(Card.published("zqa3 a_b"));
        insert(Card.published("zqa3 axb"));
        insert(Card.published("zqa3 c!d"));
        insert(Card.published("zqa3 c!!d"));

        list("keyword=zqa3 100%").andExpect(jsonPath("$.items[*].name", contains("zqa3 100% 할인")));
        list("keyword=zqa3 a_b").andExpect(jsonPath("$.items[*].name", contains("zqa3 a_b")));
        list("keyword=zqa3 c!d").andExpect(jsonPath("$.items[*].name", contains("zqa3 c!d")));
    }

    // ── AC4 ──

    @Test
    void AC4_카테고리와_난이도와_키워드를_모두_만족하는_카드만_받는다() throws Exception {
        insert(Card.published("zqa4 보안 주니어").category("security").difficulty("JUNIOR"));
        insert(Card.published("zqa4 보안 시니어").category("security").difficulty("SENIOR"));
        insert(Card.published("zqa4 웹 주니어").category("web").difficulty("JUNIOR"));

        list("keyword=zqa4&category=security&difficulty=JUNIOR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[*].name", contains("zqa4 보안 주니어")));
        list("keyword=zqa4&category=security").andExpect(jsonPath("$.total").value(2));
        list("keyword=zqa4&difficulty=JUNIOR").andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void AC4_목록에_없는_카테고리는_400이_아니라_빈_목록이다() throws Exception {
        list("category=python")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void K1_AC5_새_카테고리로_거르면_그_카테고리_카드만_받는다() throws Exception {
        insert(Card.published("zqk1 훅").category("react"));
        insert(Card.published("zqk1 제네릭").category("typescript"));

        list("keyword=zqk1&category=react")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[*].name", contains("zqk1 훅")))
                .andExpect(jsonPath("$.items[0].category").value("react"));
    }

    // ── AC5 ──

    @Test
    void AC5_POPULAR는_찜수_평점_id_순이고_생략하면_POPULAR다() throws Exception {
        insert(Card.published("zqa5 a").popularity(1, "5.0"));
        insert(Card.published("zqa5 b").popularity(3, "1.0"));
        insert(Card.published("zqa5 c").popularity(1, "4.0"));
        insert(Card.published("zqa5 d").popularity(1, "5.0"));

        String[] expected = {"zqa5 b", "zqa5 a", "zqa5 d", "zqa5 c"};
        list("keyword=zqa5&sort=POPULAR")
                .andExpect(jsonPath("$.items[*].name", contains(expected)));
        list("keyword=zqa5").andExpect(jsonPath("$.items[*].name", contains(expected)));
    }

    @Test
    void AC5_LATEST는_등록일_내림차순이고_같으면_id_내림차순이다() throws Exception {
        insert(Card.published("zqa5l a").createdAt(BASE.plusDays(1)));
        insert(Card.published("zqa5l b").createdAt(BASE.plusDays(3)));
        insert(Card.published("zqa5l c").createdAt(BASE.plusDays(1)).popularity(99, "5.0"));

        list("keyword=zqa5l&sort=LATEST")
                .andExpect(jsonPath("$.items[*].name", contains("zqa5l b", "zqa5l c", "zqa5l a")));
    }

    // ── AC6 ──

    @Test
    void AC6_25개를_10개씩_나누면_2페이지는_11에서_20번째_4페이지는_빈_목록이다() throws Exception {
        // 찜 수 25..1 → 인기순 1번째가 zqa6-01
        IntStream.rangeClosed(1, 25)
                .forEach(
                        i ->
                                insert(
                                        Card.published("zqa6-%02d".formatted(i))
                                                .popularity(26 - i, "0.0")));

        list("keyword=zqa6&page=2&size=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.total").value(25))
                .andExpect(
                        jsonPath(
                                "$.items[*].name",
                                contains(
                                        IntStream.rangeClosed(11, 20)
                                                .mapToObj("zqa6-%02d"::formatted)
                                                .toArray(String[]::new))));
        list("keyword=zqa6&page=4&size=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(25));
    }

    // ── AC7 ──

    static Stream<Arguments> invalidParams() {
        String long51 = "가".repeat(51);
        return Stream.of(
                Arguments.of(
                        "difficulty=EXPERT",
                        "difficulty",
                        "난이도는 BEGINNER, JUNIOR, SENIOR 중 하나여야 합니다."),
                Arguments.of(
                        "difficulty=junior",
                        "difficulty",
                        "난이도는 BEGINNER, JUNIOR, SENIOR 중 하나여야 합니다."),
                Arguments.of("sort=NAME", "sort", "정렬은 POPULAR, LATEST 중 하나여야 합니다."),
                Arguments.of("sort=latest", "sort", "정렬은 POPULAR, LATEST 중 하나여야 합니다."),
                Arguments.of("page=0", "page", "page는 1 이상이어야 합니다."),
                Arguments.of("size=101", "size", "size는 100 이하여야 합니다."),
                Arguments.of("keyword=" + long51, "keyword", "검색어는 50자 이하로 입력하세요."),
                Arguments.of("category=" + long51, "category", "카테고리는 50자 이하여야 합니다."));
    }

    @ParameterizedTest
    @MethodSource("invalidParams")
    void AC7_규칙을_어긴_값이면_400_COMMON_400과_필드별_사유(String query, String field, String message)
            throws Exception {
        list(query)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details." + field).value(message));
    }

    @Test
    void AC7_검색어는_trim_후_50자면_통과한다() throws Exception {
        list("keyword=  " + "가".repeat(50) + "  ").andExpect(status().isOk());
    }

    // ── AC8 ──

    @Test
    void AC8_시드_카드_상태에서_조건_검색이_500ms_안에_끝난다() throws Exception {
        assertThat(publishedCount()).isGreaterThanOrEqualTo(85);
        String query = "keyword=인증&category=security&sort=POPULAR";
        list(query).andExpect(status().isOk()); // 워밍업

        long start = System.nanoTime();
        list(query).andExpect(status().isOk());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isLessThan(500);
    }
}
