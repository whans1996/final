package com.vibenote.concept.admin;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.security.JwtTokenProvider;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
                "INSERT INTO analyses (user_id, source_type, project_name, title, level, status)"
                        + " VALUES (?, 'ZIP', 'p', 't', 'BEGINNER', 'DONE')",
                userId);
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
}
