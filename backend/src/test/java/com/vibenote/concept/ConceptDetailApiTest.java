package com.vibenote.concept;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.TestProjects;
import com.vibenote.common.security.JwtProperties;
import com.vibenote.common.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * F-10 개념 카드 상세 (docs/specs/F-10-concept-detail.md AC1~AC8). 시드 카드에 기대지 않도록 고유 카테고리({@code f10-*})의
 * 카드·회원·분석·연결을 직접 넣고 롤백한다 (AC8만 시드 카드를 읽는다).
 */
@Transactional
class ConceptDetailApiTest extends IntegrationTestBase {

    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired JwtProperties jwtProperties;

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 1, 0, 0);

    // ── 데이터 준비 ──

    private long insertConcept(
            String name, String category, String status, int wishCount, String avgRating) {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, status, source,"
                        + " wish_count, avg_rating) VALUES (?, ?, 'JUNIOR', ?, ?, 'AI', ?, ?)",
                name,
                category,
                name + " 요약",
                status,
                wishCount,
                avgRating);
        return lastId();
    }

    private long published(String name, String category) {
        return insertConcept(name, category, "PUBLISHED", 0, "0.0");
    }

    private long insertUser(String name) {
        jdbc.update(
                "INSERT INTO users (email, password_hash, nickname) VALUES (?, 'hash', ?)",
                name + "@f10.test",
                name);
        return lastId();
    }

    private long insertAnalysis(
            long userId,
            String status,
            LocalDateTime createdAt,
            boolean deleted,
            String title,
            String projectName) {
        jdbc.update(
                "INSERT INTO analyses (user_id, project_id, source_type, project_name, title,"
                    + " level, status, is_public, created_at, deleted_at) VALUES (?, ?, 'ZIP', ?,"
                    + " ?, 'BEGINNER', ?, TRUE, ?, IF(?, NOW(), NULL))",
                userId,
                TestProjects.zip(jdbc, userId),
                projectName,
                title,
                status,
                createdAt,
                deleted);
        return lastId();
    }

    private long doneAnalysis(long userId, LocalDateTime createdAt) {
        return insertAnalysis(userId, "DONE", createdAt, false, "분석", null);
    }

    private long insertFile(long analysisId, String path) {
        jdbc.update(
                "INSERT INTO analysis_files (analysis_id, path, layer, content) VALUES (?, ?,"
                        + " 'service', 'class X {}')",
                analysisId,
                path);
        return lastId();
    }

    private void link(long analysisId, long conceptId, Long fileId, String hint, String note) {
        jdbc.update(
                "INSERT INTO analysis_concepts (analysis_id, concept_id, file_id, line_hint,"
                        + " context_note) VALUES (?, ?, ?, ?, ?)",
                analysisId,
                conceptId,
                fileId,
                hint,
                note);
    }

    private void link(long analysisId, long conceptId) {
        link(analysisId, conceptId, null, null, null);
    }

    private long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private String bearer(long userId) {
        return "Bearer " + tokenProvider.createAccessToken(userId, "USER");
    }

    private ResultActions detail(long id) throws Exception {
        return mockMvc.perform(get("/api/concepts/{id}", id));
    }

    private ResultActions detail(long id, String authorization) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/concepts/{id}", id);
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, authorization));
    }

    // ── AC1 ──

    @Test
    void AC1_공개_카드를_비로그인으로_조회하면_상세를_받는다() throws Exception {
        long id = insertConcept("f10-ac1 카드", "f10-ac1", "PUBLISHED", 12, "4.5");
        jdbc.update(
                "UPDATE concepts SET body = ?, example_code = ?, reference_url = ? WHERE id = ?",
                "## 핵심\n- 서명",
                "String token = jwt();",
                "https://docs.spring.io/x",
                id);

        detail(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("f10-ac1 카드"))
                .andExpect(jsonPath("$.category").value("f10-ac1"))
                .andExpect(jsonPath("$.difficulty").value("JUNIOR"))
                .andExpect(jsonPath("$.summary").value("f10-ac1 카드 요약"))
                .andExpect(jsonPath("$.body").value("## 핵심\n- 서명"))
                .andExpect(jsonPath("$.exampleCode").value("String token = jwt();"))
                .andExpect(jsonPath("$.referenceUrl").value("https://docs.spring.io/x"))
                .andExpect(jsonPath("$.wishCount").value(12))
                .andExpect(jsonPath("$.avgRating").value(4.5))
                .andExpect(jsonPath("$.appearances", empty()))
                .andExpect(jsonPath("$.related", empty()))
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.source").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist());
    }

    @Test
    void AC1_본문_예제_출처가_NULL이면_null로_받는다() throws Exception {
        long id = published("f10-ac1 빈 카드", "f10-ac1");

        detail(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value(nullValue()))
                .andExpect(jsonPath("$.exampleCode").value(nullValue()))
                .andExpect(jsonPath("$.referenceUrl").value(nullValue()))
                .andExpect(jsonPath("$.avgRating").value(0.0));
    }

    // ── AC2 ──

    @Test
    void AC2_DRAFT_HIDDEN_없는_id는_비로그인이든_로그인이든_404() throws Exception {
        long draft = insertConcept("f10-ac2 초안", "f10-ac2", "DRAFT", 0, "0.0");
        long hidden = insertConcept("f10-ac2 숨김", "f10-ac2", "HIDDEN", 0, "0.0");
        String token = bearer(insertUser("f10ac2"));

        for (long id : new long[] {draft, hidden, 999_999L}) {
            for (ResultActions result : List.of(detail(id), detail(id, token))) {
                result.andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("CONCEPT_404"))
                        .andExpect(jsonPath("$.message").value("개념 카드를 찾을 수 없습니다."));
            }
        }
    }

    // ── AC3 ──

    @Test
    void AC3_로그인하면_내_DONE_분석의_등장_위치를_최신순으로_받는다() throws Exception {
        long userId = insertUser("f10ac3");
        long c = published("f10-ac3 카드", "f10-ac3");
        long older = insertAnalysis(userId, "DONE", BASE, false, "쇼핑몰 과제", "shop-project");
        long newer = insertAnalysis(userId, "DONE", BASE.plusDays(1), false, null, "todo-app");
        long sameTimeLaterId = insertAnalysis(userId, "DONE", BASE.plusDays(1), false, null, null);
        long file = insertFile(older, "src/main/java/JwtProvider.java");
        link(older, c, file, "L24-L40", "로그인 성공 시 토큰을 만든다");
        link(newer, c);
        link(sameTimeLaterId, c, null, "L1", null);

        detail(c, bearer(userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appearances", hasSize(3)))
                .andExpect(
                        jsonPath(
                                "$.appearances[*].analysisId",
                                contains((int) sameTimeLaterId, (int) newer, (int) older)))
                .andExpect(jsonPath("$.appearances[0].analysisTitle").value("제목 없는 분석"))
                .andExpect(jsonPath("$.appearances[0].lineHint").value("L1"))
                .andExpect(jsonPath("$.appearances[1].analysisTitle").value("todo-app"))
                .andExpect(jsonPath("$.appearances[1].fileId").value(nullValue()))
                .andExpect(jsonPath("$.appearances[1].filePath").value(nullValue()))
                .andExpect(jsonPath("$.appearances[1].lineHint").value(nullValue()))
                .andExpect(jsonPath("$.appearances[1].contextNote").value(nullValue()))
                .andExpect(jsonPath("$.appearances[2].analysisTitle").value("쇼핑몰 과제"))
                .andExpect(jsonPath("$.appearances[2].fileId").value(file))
                .andExpect(
                        jsonPath("$.appearances[2].filePath")
                                .value("src/main/java/JwtProvider.java"))
                .andExpect(jsonPath("$.appearances[2].lineHint").value("L24-L40"))
                .andExpect(jsonPath("$.appearances[2].contextNote").value("로그인 성공 시 토큰을 만든다"));
    }

    @Test
    void AC3_제목이_빈_문자열이면_프로젝트_이름으로_대체한다() throws Exception {
        long userId = insertUser("f10ac3blank");
        long c = published("f10-ac3b 카드", "f10-ac3b");
        long blank = insertAnalysis(userId, "DONE", BASE, false, "", "blank-project");
        long spaces = insertAnalysis(userId, "DONE", BASE.plusDays(1), false, "  ", null);
        link(blank, c);
        link(spaces, c);

        detail(c, bearer(userId))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.appearances[*].analysisId", contains((int) spaces, (int) blank)))
                .andExpect(jsonPath("$.appearances[0].analysisTitle").value("제목 없는 분석"))
                .andExpect(jsonPath("$.appearances[1].analysisTitle").value("blank-project"));
    }

    // ── AC4 ──

    @Test
    void AC4_다른_회원_삭제된_분석_DONE이_아닌_분석은_빠진다() throws Exception {
        long me = insertUser("f10ac4me");
        long other = insertUser("f10ac4other");
        long c = published("f10-ac4 카드", "f10-ac4");
        long mine = doneAnalysis(me, BASE);
        long othersPublic = doneAnalysis(other, BASE.plusDays(1));
        long deleted = insertAnalysis(me, "DONE", BASE.plusDays(2), true, "삭제", null);
        long failed = insertAnalysis(me, "FAILED", BASE.plusDays(3), false, "실패", null);
        long running = insertAnalysis(me, "EXPLAINING", BASE.plusDays(4), false, "진행", null);
        for (long a : new long[] {mine, othersPublic, deleted, failed, running}) {
            link(a, c);
        }

        detail(c, bearer(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appearances[*].analysisId", contains((int) mine)));
    }

    @Test
    void AC4_연결된_분석이_11개면_최근_10개만_받는다() throws Exception {
        long me = insertUser("f10ac4many");
        long c = published("f10-ac4 많은 카드", "f10-ac4");
        long[] ids = new long[11];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = doneAnalysis(me, BASE.plusDays(i));
            link(ids[i], c);
        }

        detail(c, bearer(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appearances", hasSize(10)))
                .andExpect(jsonPath("$.appearances[0].analysisId").value(ids[10]))
                .andExpect(jsonPath("$.appearances[9].analysisId").value(ids[1]));
    }

    // ── AC5 ──

    @Test
    void AC5_깨지거나_만료된_토큰이면_200이고_등장_위치는_비어_있다() throws Exception {
        long me = insertUser("f10ac5");
        long c = published("f10-ac5 카드", "f10-ac5");
        link(doneAnalysis(me, BASE), c);
        Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
        String expired =
                Jwts.builder()
                        .subject(Long.toString(me))
                        .claim("role", "USER")
                        .issuedAt(Date.from(past))
                        .expiration(Date.from(past.plus(1, ChronoUnit.HOURS)))
                        .signWith(
                                Keys.hmacShaKeyFor(
                                        jwtProperties.secret().getBytes(StandardCharsets.UTF_8)))
                        .compact();

        for (String auth : new String[] {"Bearer not-a-jwt", "Bearer " + expired}) {
            detail(c, auth)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(c))
                    .andExpect(jsonPath("$.appearances", empty()));
        }
        // 같은 회원의 정상 토큰이면 나온다 (위 결과가 데이터 문제로 비지 않았음을 확인)
        detail(c, bearer(me)).andExpect(jsonPath("$.appearances", hasSize(1)));
    }

    // ── AC6 ──

    @Test
    void AC6_함께_등장한_공개_카드를_횟수와_인기순으로_최대_4개_받는다() throws Exception {
        long u1 = insertUser("f10ac6a");
        long u2 = insertUser("f10ac6b");
        long c = published("f10-ac6 C", "f10-ac6");
        long x = insertConcept("f10-ac6 X", "f10-ac6-x", "PUBLISHED", 0, "0.0");
        long y = insertConcept("f10-ac6 Y", "f10-ac6-x", "PUBLISHED", 5, "1.0");
        long z = insertConcept("f10-ac6 Z", "f10-ac6-x", "PUBLISHED", 1, "4.0");
        long w = insertConcept("f10-ac6 W", "f10-ac6-x", "PUBLISHED", 1, "3.0");
        long v = insertConcept("f10-ac6 V", "f10-ac6-x", "PUBLISHED", 99, "5.0");
        long d = insertConcept("f10-ac6 D", "f10-ac6-x", "DRAFT", 99, "5.0");

        long a1 = doneAnalysis(u1, BASE);
        long a2 = doneAnalysis(u1, BASE.plusDays(1));
        long a3 = doneAnalysis(u2, BASE.plusDays(2)); // 다른 회원의 분석도 센다
        long a4 = doneAnalysis(u2, BASE.plusDays(3));
        long a5 = doneAnalysis(u2, BASE.plusDays(4));
        long deleted = insertAnalysis(u1, "DONE", BASE, true, "삭제", null);
        long failed = insertAnalysis(u1, "FAILED", BASE, false, "실패", null);
        long running = insertAnalysis(u2, "QUIZ", BASE, false, "진행", null);
        for (long a : new long[] {a1, a2, a3, a4, a5, deleted, failed, running}) {
            link(a, c);
            link(a, d);
        }
        for (long a : new long[] {a1, a2, a3}) link(a, x);
        for (long a : new long[] {a1, a2}) link(a, y);
        for (long a : new long[] {a3, a4}) link(a, z);
        for (long a : new long[] {a4, a5}) link(a, w);
        // V는 완료된 분석 1개에만 있고 나머지는 삭제·실패·진행 중이라 1회로 센다 → 5번째로 밀려 빠진다
        for (long a : new long[] {a5, deleted, failed, running}) link(a, v);

        detail(c)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.related", hasSize(4)))
                .andExpect(
                        jsonPath("$.related[*].id", contains((int) x, (int) y, (int) z, (int) w)))
                .andExpect(jsonPath("$.related[0].name").value("f10-ac6 X"))
                .andExpect(jsonPath("$.related[0].category").value("f10-ac6-x"))
                .andExpect(jsonPath("$.related[0].difficulty").value("JUNIOR"))
                .andExpect(jsonPath("$.related[0].summary").value("f10-ac6 X 요약"))
                .andExpect(jsonPath("$.related[1].wishCount").value(5))
                .andExpect(jsonPath("$.related[2].avgRating").value(4.0))
                .andExpect(jsonPath("$.related[0].body").doesNotExist());
    }

    @Test
    void AC6_함께_등장_횟수가_같으면_인기순_다음_id_오름차순이다() throws Exception {
        long u = insertUser("f10ac6tie");
        long c = published("f10-ac6t C", "f10-ac6t");
        long first = insertConcept("f10-ac6t 1", "f10-ac6t-x", "PUBLISHED", 2, "4.0");
        long second = insertConcept("f10-ac6t 2", "f10-ac6t-x", "PUBLISHED", 2, "4.0");
        long a = doneAnalysis(u, BASE);
        link(a, c);
        link(a, second);
        link(a, first);

        detail(c).andExpect(jsonPath("$.related[*].id", contains((int) first, (int) second)));
    }

    // ── AC7 ──

    @Test
    void AC7_모자란_자리는_같은_카테고리_인기순으로_중복_없이_채운다() throws Exception {
        long u = insertUser("f10ac7");
        long c = published("f10-ac7 C", "f10-ac7");
        long co = insertConcept("f10-ac7 함께", "f10-ac7", "PUBLISHED", 50, "5.0");
        long s1 = insertConcept("f10-ac7 S1", "f10-ac7", "PUBLISHED", 9, "1.0");
        long s2 = insertConcept("f10-ac7 S2", "f10-ac7", "PUBLISHED", 3, "4.5");
        long s3 = insertConcept("f10-ac7 S3", "f10-ac7", "PUBLISHED", 3, "4.0");
        insertConcept("f10-ac7 S4", "f10-ac7", "PUBLISHED", 1, "5.0");
        insertConcept("f10-ac7 초안", "f10-ac7", "DRAFT", 100, "5.0");
        insertConcept("f10-ac7 다른 분야", "f10-ac7-other", "PUBLISHED", 100, "5.0");
        long a = doneAnalysis(u, BASE);
        link(a, c);
        link(a, co);

        detail(c)
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.related[*].id",
                                contains((int) co, (int) s1, (int) s2, (int) s3)));
    }

    @Test
    void AC7_함께_등장이_없고_채울_카드가_모자라면_있는_만큼만_받는다() throws Exception {
        long c = published("f10-ac7b C", "f10-ac7b");
        long only = insertConcept("f10-ac7b 하나", "f10-ac7b", "PUBLISHED", 1, "1.0");
        long alone = published("f10-ac7c 혼자", "f10-ac7c");

        detail(c).andExpect(jsonPath("$.related[*].id", contains((int) only)));
        detail(alone).andExpect(jsonPath("$.related", empty()));
    }

    // ── AC8 ──

    @Test
    void AC8_시드가_적재된_DB에서_로그인_상세가_500ms_이내다() throws Exception {
        long seedId =
                jdbc.queryForObject(
                        "SELECT MIN(id) FROM concepts WHERE status = 'PUBLISHED' AND source ="
                                + " 'SEED'",
                        Long.class);
        String token = bearer(insertUser("f10ac8"));
        detail(seedId, token).andExpect(status().isOk()); // 워밍업

        long start = System.nanoTime();
        detail(seedId, token).andExpect(status().isOk());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isLessThan(500);
    }
}
