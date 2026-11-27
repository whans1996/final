package com.vibenote.concept.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.TestProjects;
import com.vibenote.common.security.JwtTokenProvider;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * F-13 관리자 카드 관리 (docs/specs/F-13-admin-concepts.md). 개수를 정확히 세려고 테스트마다 트랜잭션 안에서 카드를 모두 지우고 (시드 포함,
 * 끝나면 롤백) 필요한 카드만 넣는다.
 */
@Transactional
class AdminConceptApiTest extends IntegrationTestBase {

    static final String LIST = "/api/admin/concepts";
    static final String COUNTS = "/api/admin/concepts/status-counts";
    static final String ONE = "/api/admin/concepts/{id}";

    static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenProvider tokenProvider;

    long adminId;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM concepts");
        adminId = insertUser("f13admin");
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", adminId);
    }

    // ── 데이터 준비 ──

    long insertConcept(String name, String status, String source, LocalDateTime updatedAt) {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, status, source,"
                        + " created_at, updated_at) VALUES (?, 'spring', 'JUNIOR', ?, ?, ?, ?, ?)",
                name,
                name + " 요약",
                status,
                source,
                updatedAt,
                updatedAt);
        return lastId();
    }

    long insertConcept(String name, String status, String source) {
        return insertConcept(name, status, source, BASE);
    }

    long insertUser(String name) {
        jdbc.update(
                "INSERT INTO users (email, password_hash, nickname) VALUES (?, 'hash', ?)",
                name + "@f13.test",
                name);
        return lastId();
    }

    long insertAnalysis(long userId) {
        jdbc.update(
                "INSERT INTO analyses (user_id, project_id, source_type, project_name, title,"
                        + " level, status) VALUES (?, ?, 'ZIP', 'p', 't', 'BEGINNER', 'DONE')",
                userId,
                TestProjects.zip(jdbc, userId));
        return lastId();
    }

    void link(long analysisId, long conceptId) {
        jdbc.update(
                "INSERT INTO analysis_concepts (analysis_id, concept_id) VALUES (?, ?)",
                analysisId,
                conceptId);
    }

    /** jsonPath는 작은 정수를 Integer로 읽는다. */
    static Integer[] ints(long... ids) {
        return java.util.Arrays.stream(ids).mapToObj(id -> (int) id).toArray(Integer[]::new);
    }

    long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    String adminBearer() {
        return "Bearer " + tokenProvider.createAccessToken(adminId, "ADMIN");
    }

    ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header("Authorization", adminBearer()));
    }

    /** AC1·AC2 공통 데이터: DRAFT·AI 2, PUBLISHED·SEED 3, PUBLISHED·ADMIN 1, HIDDEN·SEED 1. */
    long[] ac1Data() {
        return new long[] {
            insertConcept("초안 하나", "DRAFT", "AI", BASE.plusHours(1)),
            insertConcept("초안 둘", "DRAFT", "AI", BASE.plusHours(7)),
            insertConcept("시드 하나", "PUBLISHED", "SEED", BASE.plusHours(3)),
            insertConcept("시드 둘", "PUBLISHED", "SEED", BASE.plusHours(3)),
            insertConcept("시드 셋", "PUBLISHED", "SEED", BASE.plusHours(2)),
            insertConcept("직접 작성", "PUBLISHED", "ADMIN", BASE.plusHours(5)),
            insertConcept("숨긴 시드", "HIDDEN", "SEED", BASE.plusHours(4)),
        };
    }

    // ── AC1 목록 ──

    @Test
    void AC1_상태_출처_조합으로_거르고_조건이_없으면_전체를_수정일_역순으로_준다() throws Exception {
        long[] ids = ac1Data();

        asAdmin(get(LIST).param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[*].status", contains("DRAFT", "DRAFT")));
        asAdmin(get(LIST).param("source", "SEED")).andExpect(jsonPath("$.total").value(4));
        asAdmin(get(LIST).param("status", "PUBLISHED").param("source", "ADMIN"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("직접 작성"));

        // updated_at DESC, id DESC (시드 하나·둘은 수정일이 같아 id 큰 쪽이 먼저)
        asAdmin(get(LIST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(7))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(
                        jsonPath(
                                "$.items[*].id",
                                contains(
                                        ints(
                                                ids[1], ids[5], ids[6], ids[3], ids[2], ids[4],
                                                ids[0]))));
    }

    @Test
    void AC1_항목은_요약_모양이고_본문과_예제는_없다() throws Exception {
        long id = insertConcept("본문 있는 카드", "DRAFT", "AI");
        jdbc.update(
                "UPDATE concepts SET body = '본문', example_code = 'code', wish_count = 3,"
                        + " avg_rating = 4.5, updated_at = updated_at WHERE id = ?",
                id);

        asAdmin(get(LIST))
                .andExpect(jsonPath("$.items[0].id").value(id))
                .andExpect(jsonPath("$.items[0].name").value("본문 있는 카드"))
                .andExpect(jsonPath("$.items[0].category").value("spring"))
                .andExpect(jsonPath("$.items[0].difficulty").value("JUNIOR"))
                .andExpect(jsonPath("$.items[0].summary").value("본문 있는 카드 요약"))
                .andExpect(jsonPath("$.items[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.items[0].source").value("AI"))
                .andExpect(jsonPath("$.items[0].wishCount").value(3))
                .andExpect(jsonPath("$.items[0].avgRating").value(4.5))
                .andExpect(jsonPath("$.items[0].updatedAt").value("2026-01-01T00:00:00+09:00"))
                .andExpect(jsonPath("$.items[0].body").doesNotExist())
                .andExpect(jsonPath("$.items[0].exampleCode").doesNotExist());
    }

    @Test
    void AC1_keyword는_이름_요약_부분_일치이고_와일드카드는_글자_그대로_찾는다() throws Exception {
        long percent = insertConcept("100% 커버리지", "PUBLISHED", "SEED");
        long under = insertConcept("snake_case", "DRAFT", "AI");
        long jwt = insertConcept("JWT 인증", "HIDDEN", "SEED");
        insertConcept("아무거나", "DRAFT", "AI");

        asAdmin(get(LIST).param("keyword", "%"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(percent));
        asAdmin(get(LIST).param("keyword", "_"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(under));
        // 대소문자 무시, trim, 요약(이름 + " 요약")도 찾는다
        asAdmin(get(LIST).param("keyword", "  jwt  "))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(jwt));
        asAdmin(get(LIST).param("keyword", "요약")).andExpect(jsonPath("$.total").value(4));
    }

    @Test
    void AC1_잘못된_조건은_400과_필드별_사유() throws Exception {
        asAdmin(get(LIST).param("status", "DELETED").param("source", "USER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.status").exists())
                .andExpect(jsonPath("$.details.source").exists());
        asAdmin(get(LIST).param("keyword", "가".repeat(51)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.keyword").exists());
        asAdmin(get(LIST).param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.size").exists());
    }

    @Test
    void AC1_범위를_넘은_page는_빈_목록과_실제_total() throws Exception {
        ac1Data();

        asAdmin(get(LIST).param("page", "3").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.page").value(3))
                .andExpect(jsonPath("$.total").value(7));
    }

    // ── AC2 상태별 개수 ──

    @Test
    void AC2_상태별_개수를_세_키_모두_준다() throws Exception {
        ac1Data();

        asAdmin(get(COUNTS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.DRAFT").value(2))
                .andExpect(jsonPath("$.PUBLISHED").value(4))
                .andExpect(jsonPath("$.HIDDEN").value(1));
    }

    @Test
    void AC2_카드가_없는_상태도_0으로_준다() throws Exception {
        insertConcept("초안만", "DRAFT", "AI");

        asAdmin(get(COUNTS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.DRAFT").value(1))
                .andExpect(jsonPath("$.PUBLISHED").value(0))
                .andExpect(jsonPath("$.HIDDEN").value(0));
    }

    // ── AC3 상세 ──

    @Test
    void AC3_DRAFT_카드도_모든_컬럼과_연결된_분석_수_리뷰_수를_준다() throws Exception {
        long draft = insertConcept("낙관적 락", "DRAFT", "AI", BASE.plusDays(1));
        jdbc.update(
                "UPDATE concepts SET body = '## 핵심', updated_at = updated_at WHERE id = ?", draft);
        long other = insertConcept("다른 카드", "DRAFT", "AI");
        long userId = insertUser("f13ac3");
        long a1 = insertAnalysis(userId);
        long a2 = insertAnalysis(userId);
        link(a1, draft);
        link(a2, draft);
        link(a1, other);
        jdbc.update(
                "INSERT INTO concept_reviews (concept_id, user_id, rating, content) VALUES (?, ?,"
                        + " 5, '좋아요')",
                draft,
                userId);

        asAdmin(get(ONE, draft))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(draft))
                .andExpect(jsonPath("$.name").value("낙관적 락"))
                .andExpect(jsonPath("$.category").value("spring"))
                .andExpect(jsonPath("$.difficulty").value("JUNIOR"))
                .andExpect(jsonPath("$.summary").value("낙관적 락 요약"))
                .andExpect(jsonPath("$.body").value("## 핵심"))
                .andExpect(jsonPath("$.exampleCode").value((Object) null))
                .andExpect(jsonPath("$.referenceUrl").value((Object) null))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.source").value("AI"))
                .andExpect(jsonPath("$.wishCount").value(0))
                .andExpect(jsonPath("$.avgRating").value(0.0))
                .andExpect(jsonPath("$.reviewCount").value(1))
                .andExpect(jsonPath("$.linkedAnalysisCount").value(2))
                .andExpect(jsonPath("$.createdAt").value("2026-01-02T00:00:00+09:00"))
                .andExpect(jsonPath("$.updatedAt").value("2026-01-02T00:00:00+09:00"));
    }

    @Test
    void AC3_HIDDEN_카드도_조회되고_없는_id는_404() throws Exception {
        long hidden = insertConcept("숨긴 카드", "HIDDEN", "SEED");

        asAdmin(get(ONE, hidden))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HIDDEN"))
                .andExpect(jsonPath("$.linkedAnalysisCount").value(0))
                .andExpect(jsonPath("$.reviewCount").value(0));
        asAdmin(get(ONE, 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONCEPT_404"));
    }

    // ── 쓰기 공통 ──

    static Map<String, Object> validBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "낙관적 락");
        body.put("category", "spring");
        body.put("difficulty", "SENIOR");
        body.put("summary", "저장 시점에 버전으로 충돌을 확인한다.");
        body.put("body", "## 핵심");
        body.put("exampleCode", "UPDATE t SET v = v + 1");
        body.put("referenceUrl", "https://docs.spring.io/x");
        body.put("status", "PUBLISHED");
        return body;
    }

    static Map<String, Object> with(String key, Object value) {
        Map<String, Object> body = validBody();
        body.put(key, value);
        return body;
    }

    static Map<String, Object> with(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> body = with(k1, v1);
        body.put(k2, v2);
        return body;
    }

    MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, Object body)
            throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }

    Map<String, Object> row(long id) {
        return jdbc.queryForMap("SELECT * FROM concepts WHERE id = ?", id);
    }

    long conceptCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM concepts", Long.class);
    }

    // ── AC4 생성 ──

    @Test
    void AC4_생성하면_201과_id_출처는_ADMIN_값은_trim하고_빈_선택값은_NULL() throws Exception {
        Map<String, Object> body = validBody();
        body.put("name", "  낙관적 락  ");
        body.put("summary", " 요약 ");
        body.put("body", "   ");
        body.put("exampleCode", "");
        body.put("referenceUrl", "  ");

        String res =
                asAdmin(json(post(LIST), body))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id").isNumber())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long id = objectMapper.readTree(res).get("id").asLong();

        Map<String, Object> row = row(id);
        assertThat(row.get("name")).isEqualTo("낙관적 락");
        assertThat(row.get("summary")).isEqualTo("요약");
        assertThat(row.get("body")).isNull();
        assertThat(row.get("example_code")).isNull();
        assertThat(row.get("reference_url")).isNull();
        assertThat(row.get("source")).isEqualTo("ADMIN");
        assertThat(row.get("status")).isEqualTo("PUBLISHED");
        assertThat(row.get("wish_count")).isEqualTo(0);
        assertThat(row.get("avg_rating").toString()).isEqualTo("0.0");

        // 공개로 만든 카드는 바로 F-10 상세로 보인다
        mockMvc.perform(get("/api/concepts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("낙관적 락"));
    }

    @Test
    void AC4_DRAFT로_만들_수_있고_HIDDEN은_400() throws Exception {
        String res =
                asAdmin(json(post(LIST), with("status", "DRAFT")))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long id = objectMapper.readTree(res).get("id").asLong();
        assertThat(row(id).get("status")).isEqualTo("DRAFT");
        mockMvc.perform(get("/api/concepts/{id}", id)).andExpect(status().isNotFound());

        long before = conceptCount();
        asAdmin(json(post(LIST), with("name", "숨김으로 생성", "status", "HIDDEN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.status").exists());
        assertThat(conceptCount()).isEqualTo(before);
    }

    // ── AC5 검증 ──

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                Arguments.of("name", null),
                Arguments.of("name", "   "),
                Arguments.of("name", "가".repeat(101)),
                Arguments.of("category", null),
                Arguments.of("category", "python"),
                Arguments.of("difficulty", null),
                Arguments.of("difficulty", "EXPERT"),
                Arguments.of("summary", null),
                Arguments.of("summary", "가".repeat(501)),
                Arguments.of("body", "a".repeat(10_001)),
                Arguments.of("exampleCode", "a".repeat(10_001)),
                Arguments.of("referenceUrl", "https://" + "a".repeat(493)),
                Arguments.of("referenceUrl", "javascript:alert(1)"),
                Arguments.of("referenceUrl", "ftp://x"),
                Arguments.of("referenceUrl", "//evil.com"),
                Arguments.of("status", null),
                Arguments.of("status", "DELETED"));
    }

    @ParameterizedTest(name = "{0}={1}")
    @MethodSource("invalidFields")
    void AC5_생성_요청의_잘못된_값은_400과_필드별_사유(String field, Object value) throws Exception {
        long before = conceptCount();

        asAdmin(json(post(LIST), with(field, value)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details." + field).exists());
        assertThat(conceptCount()).isEqualTo(before);
    }

    @ParameterizedTest(name = "{0}={1}")
    @MethodSource("invalidFields")
    void AC5_수정_요청의_잘못된_값은_400이고_카드는_그대로(String field, Object value) throws Exception {
        long id = insertConcept("원래 이름", "DRAFT", "AI");
        Map<String, Object> before = row(id);

        asAdmin(json(put(ONE, id), with(field, value)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details." + field).exists());
        assertThat(row(id)).isEqualTo(before);
    }

    @Test
    void AC5_출처_URL은_대소문자_무시하고_http_https를_받는다() throws Exception {
        asAdmin(json(post(LIST), with("referenceUrl", "HTTP://example.com/a")))
                .andExpect(status().isCreated());
        asAdmin(json(post(LIST), with("name", "다른 카드", "referenceUrl", "Https://x.dev")))
                .andExpect(status().isCreated());
    }

    // ── AC6 이름 중복 ──

    @Test
    void AC6_같은_이름으로_생성_수정하면_409_CONCEPT_002() throws Exception {
        long jwt = insertConcept("JWT 인증", "PUBLISHED", "SEED");
        long other = insertConcept("세션 인증", "DRAFT", "AI");

        asAdmin(json(post(LIST), with("name", "  JWT 인증 ")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCEPT_002"))
                .andExpect(jsonPath("$.message").value("같은 이름의 개념 카드가 이미 있습니다."))
                .andExpect(jsonPath("$.details.name").exists());
        // 콜레이션상 같은 이름(대소문자만 다름)도 UNIQUE에 걸리므로 같은 응답
        asAdmin(json(put(ONE, other), with("name", "jwt 인증")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCEPT_002"));
        assertThat(row(other).get("name")).isEqualTo("세션 인증");

        // 자기 이름을 그대로 둔 수정은 409가 아니다
        asAdmin(json(put(ONE, jwt), with("name", "JWT 인증"))).andExpect(status().isOk());
    }

    // ── AC7 수정·공개 ──

    @Test
    void AC7_초안을_고쳐_공개하면_출처와_찜_평점은_그대로이고_공개_화면에_나온다() throws Exception {
        long draft = insertConcept("초안 카드", "DRAFT", "AI");
        long userId = insertUser("f13ac7");
        long analysisId = insertAnalysis(userId);
        link(analysisId, draft);
        String userBearer = "Bearer " + tokenProvider.createAccessToken(userId, "USER");

        mockMvc.perform(
                        get("/api/analyses/{id}/concepts", analysisId)
                                .header("Authorization", userBearer))
                .andExpect(jsonPath("$.pendingCount").value(1))
                .andExpect(jsonPath("$.items", hasSize(0)));

        Map<String, Object> body = with("name", "f13 공개된 초안");
        body.put("wishCount", 99);
        body.put("avgRating", 5.0);
        body.put("source", "SEED");
        asAdmin(json(put(ONE, draft), body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(draft))
                .andExpect(jsonPath("$.name").value("f13 공개된 초안"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.source").value("AI"))
                .andExpect(jsonPath("$.wishCount").value(0))
                .andExpect(jsonPath("$.avgRating").value(0.0))
                .andExpect(jsonPath("$.linkedAnalysisCount").value(1))
                .andExpect(jsonPath("$.referenceUrl").value("https://docs.spring.io/x"));

        mockMvc.perform(get("/api/concepts/{id}", draft)).andExpect(status().isOk());
        mockMvc.perform(get("/api/concepts").param("keyword", "f13 공개된"))
                .andExpect(jsonPath("$.items[*].id", hasItem((int) draft)));
        mockMvc.perform(
                        get("/api/analyses/{id}/concepts", analysisId)
                                .header("Authorization", userBearer))
                .andExpect(jsonPath("$.pendingCount").value(0))
                .andExpect(jsonPath("$.items[*].concept.id", hasItem((int) draft)));
    }

    static Stream<Arguments> transitions() {
        return Stream.of(
                Arguments.of("DRAFT", "HIDDEN"),
                Arguments.of("PUBLISHED", "HIDDEN"),
                Arguments.of("HIDDEN", "PUBLISHED"),
                Arguments.of("DRAFT", "DRAFT"));
    }

    @ParameterizedTest(name = "{0}→{1}")
    @MethodSource("transitions")
    void AC7_허용된_상태_전이(String from, String to) throws Exception {
        long id = insertConcept("전이 카드", from, "AI");

        asAdmin(json(put(ONE, id), with("summary", "고친 요약", "status", to)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(to))
                .andExpect(jsonPath("$.summary").value("고친 요약"));
        assertThat(row(id).get("status")).isEqualTo(to);
    }

    // ── AC8 DRAFT로 되돌리기 금지 ──

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"PUBLISHED", "HIDDEN"})
    void AC8_공개_숨김_카드를_DRAFT로_되돌리면_409_CONCEPT_003(String from) throws Exception {
        long id = insertConcept("되돌릴 카드", from, "ADMIN");
        Map<String, Object> before = row(id);

        asAdmin(json(put(ONE, id), with("status", "DRAFT")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCEPT_003"))
                .andExpect(jsonPath("$.message").value("공개되었거나 숨긴 카드는 검수 대기로 되돌리거나 삭제할 수 없습니다."));
        assertThat(row(id)).isEqualTo(before);
    }

    @Test
    void AC8_없는_카드_수정은_404() throws Exception {
        asAdmin(json(put(ONE, 999999), validBody()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONCEPT_404"));
    }

    // ── AC9 시드 카드 이름 ──

    @Test
    void AC9_시드_카드_이름을_바꾸면_400_이름을_두면_다른_필드와_상태를_고칠_수_있다() throws Exception {
        long seed = insertConcept("시드 카드", "PUBLISHED", "SEED");
        Map<String, Object> before = row(seed);

        asAdmin(json(put(ONE, seed), with("name", "새 이름")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.name").value("시드 카드의 이름은 바꿀 수 없습니다."));
        assertThat(row(seed)).isEqualTo(before);

        asAdmin(json(put(ONE, seed), with("name", "시드 카드", "status", "HIDDEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HIDDEN"))
                .andExpect(jsonPath("$.source").value("SEED"))
                .andExpect(jsonPath("$.summary").value("저장 시점에 버전으로 충돌을 확인한다."));
    }

    // ── AC10 삭제 ──

    @Test
    void AC10_DRAFT는_연결과_함께_지워지고_공개_숨김은_409_없으면_404() throws Exception {
        long draft = insertConcept("지울 초안", "DRAFT", "AI");
        long published = insertConcept("공개 카드", "PUBLISHED", "SEED");
        long hidden = insertConcept("숨긴 카드", "HIDDEN", "SEED");
        long userId = insertUser("f13ac10");
        long a1 = insertAnalysis(userId);
        long a2 = insertAnalysis(userId);
        link(a1, draft);
        link(a2, draft);

        asAdmin(delete(ONE, draft)).andExpect(status().isNoContent());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM concepts WHERE id = ?", Long.class, draft))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM analysis_concepts WHERE concept_id = ?",
                                Long.class,
                                draft))
                .isZero();

        for (long id : new long[] {published, hidden}) {
            asAdmin(delete(ONE, id))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONCEPT_003"));
            assertThat(row(id)).isNotNull();
        }
        asAdmin(delete(ONE, 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONCEPT_404"));
    }

    // ── AC11 권한 ──

    static Stream<Arguments> endpoints() {
        return Stream.of(
                Arguments.of("GET", LIST),
                Arguments.of("GET", COUNTS),
                Arguments.of("GET", LIST + "/1"),
                Arguments.of("POST", LIST),
                Arguments.of("PUT", LIST + "/1"),
                Arguments.of("DELETE", LIST + "/1"));
    }

    MockHttpServletRequestBuilder request(String method, String url) throws Exception {
        MockHttpServletRequestBuilder builder =
                switch (method) {
                    case "GET" -> get(url);
                    case "POST" -> post(url);
                    case "PUT" -> put(url);
                    default -> delete(url);
                };
        return json(builder, validBody());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("endpoints")
    void AC11_토큰이_없으면_401_USER면_403(String method, String url) throws Exception {
        long before = conceptCount();

        mockMvc.perform(request(method, url))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
        long userId = insertUser("f13ac11");
        mockMvc.perform(
                        request(method, url)
                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + tokenProvider.createAccessToken(userId, "USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));
        assertThat(conceptCount()).isEqualTo(before);
    }
}
