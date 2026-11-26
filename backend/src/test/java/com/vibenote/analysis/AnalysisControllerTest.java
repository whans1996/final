package com.vibenote.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/** F-05a 분석 요청·목록·상태 API (docs/specs/F-05a-analysis-request-zip.md AC1~AC4, AC12, AC13). */
class AnalysisControllerTest extends AnalysisTestSupport {

    private static final String URL = "/api/analyses";

    /** 파이프라인 시작 여부만 확인한다. 실제 단계는 AnalysisPipelineTest가 run을 직접 불러 검증한다. */
    @MockitoBean AnalysisPipeline pipeline;

    private static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "application/zip", content);
    }

    private static MockPart part(String name, String value) {
        return new MockPart(name, value.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartHttpServletRequestBuilder request(long userId) {
        return (MockMultipartHttpServletRequestBuilder)
                multipart(URL).header(HttpHeaders.AUTHORIZATION, bearer(userId));
    }

    /** 분석 임시 폴더의 파일 목록 (AC2: 검증 실패 시 임시 파일이 생기지 않는다). */
    private List<Path> tempFiles() throws IOException {
        Path dir = Path.of(uploadDir, "analysis-tmp");
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.sorted().toList();
        }
    }

    private long countAnalyses(long userId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM analyses WHERE user_id = ?", Long.class, userId);
    }

    private long postZip(long userId) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                request(userId)
                                        .file(file("todo-app.zip", sampleZip()))
                                        .part(part("consent", "true")))
                        .andExpect(status().isAccepted())
                        .andReturn();
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("analysisId")
                .asLong();
    }

    // ───────────── AC1 ─────────────

    @Test
    void AC1_ZIP을_올리면_202와_analysisId를_받는다() throws Exception {
        long me = createUser("ac1me", "JUNIOR");

        MvcResult result =
                mockMvc.perform(
                                request(me)
                                        .file(file("todo-app.zip", sampleZip()))
                                        .part(part("consent", "true")))
                        .andExpect(status().isAccepted())
                        .andExpect(jsonPath("$.analysisId").isNumber())
                        .andExpect(jsonPath("$.status").value("FETCHING"))
                        .andReturn();
        long id =
                objectMapper
                        .readTree(result.getResponse().getContentAsString())
                        .get("analysisId")
                        .asLong();

        Map<String, Object> row = analysisRow(id);
        assertThat(((Number) row.get("user_id")).longValue()).isEqualTo(me);
        assertThat(row.get("source_type")).isEqualTo("ZIP");
        assertThat(row.get("source_url")).isNull();
        assertThat(row.get("project_name")).isEqualTo("todo-app");
        assertThat(row.get("title")).isEqualTo("todo-app");
        assertThat(row.get("level")).isEqualTo("JUNIOR");
        assertThat(row.get("status")).isEqualTo("FETCHING");
        assertThat(((Number) row.get("progress")).intValue()).isZero();
        assertThat(row.get("is_public")).isEqualTo(false);
        assertThat(tempZip(id)).exists().hasBinaryContent(sampleZip());
        verify(pipeline, timeout(2000).times(1)).run(id);
    }

    @Test
    void AC1_level을_보내면_그_값이_저장된다() throws Exception {
        long me = createUser("ac1senior", "BEGINNER");

        MvcResult result =
                mockMvc.perform(
                                request(me)
                                        .file(file("My.Project.ZIP", sampleZip()))
                                        .part(part("level", "SENIOR"))
                                        .part(part("consent", "true")))
                        .andExpect(status().isAccepted())
                        .andReturn();
        long id =
                objectMapper
                        .readTree(result.getResponse().getContentAsString())
                        .get("analysisId")
                        .asLong();

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("level")).isEqualTo("SENIOR");
        assertThat(row.get("title")).isEqualTo("My.Project");
    }

    // ───────────── AC2 ─────────────

    private void expectFieldError(
            long me, MockMultipartHttpServletRequestBuilder builder, String field)
            throws Exception {
        List<Path> tempFilesBefore = tempFiles();
        mockMvc.perform(builder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details." + field).isString());
        assertThat(countAnalyses(me)).isZero();
        assertThat(tempFiles()).isEqualTo(tempFilesBefore);
    }

    @Test
    void AC2_파일이_없거나_비었거나_ZIP이_아니면_400_details_file() throws Exception {
        long me = createUser("ac2file", "JUNIOR");

        expectFieldError(me, request(me).part(part("consent", "true")), "file");
        expectFieldError(
                me,
                request(me).file(file("empty.zip", new byte[0])).part(part("consent", "true")),
                "file");
        expectFieldError(
                me,
                request(me).file(file("todo.txt", sampleZip())).part(part("consent", "true")),
                "file");
        expectFieldError(
                me,
                request(me)
                        .file(file("fake.zip", "not a zip".getBytes(StandardCharsets.UTF_8)))
                        .part(part("consent", "true")),
                "file");
        verify(pipeline, never()).run(anyLong());
    }

    @Test
    void AC2_동의하지_않으면_400_details_consent() throws Exception {
        long me = createUser("ac2consent", "JUNIOR");

        expectFieldError(me, request(me).file(file("todo.zip", sampleZip())), "consent");
        expectFieldError(
                me,
                request(me).file(file("todo.zip", sampleZip())).part(part("consent", "false")),
                "consent");
    }

    @Test
    void F27_AC12_동의_거부_메시지는_GMS_기준이다() throws Exception {
        long me = createUser("f27consent", "JUNIOR");

        mockMvc.perform(request(me).file(file("todo.zip", sampleZip())))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.details.consent")
                                .value("주요 파일이 SSAFY GMS를 거쳐 외부 AI로 전송되는 데 동의해야 분석할 수" + " 있습니다."));
    }

    @Test
    void AC2_허용되지_않은_level이면_400_details_level() throws Exception {
        long me = createUser("ac2level", "JUNIOR");

        expectFieldError(
                me,
                request(me)
                        .file(file("todo.zip", sampleZip()))
                        .part(part("level", "EXPERT"))
                        .part(part("consent", "true")),
                "level");
    }

    // 10MB 초과 → 413 COMMON_413은 multipart 한도(MaxUploadSizeExceededException)가 처리한다.
    // MockMvc는 MultipartResolver를 거치지 않아 한도를 재현할 수 없으므로 GlobalExceptionHandlerTest의 매핑으로 갈음한다.

    // ───────────── AC3 ─────────────

    @Test
    void AC3_토큰이_없으면_401_AUTH_401() throws Exception {
        mockMvc.perform(multipart(URL).file(file("todo.zip", sampleZip())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
        mockMvc.perform(get(URL + "/{id}/status", 1))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    // ───────────── AC4 ─────────────

    @Test
    void AC4_오늘_FAILED가_아닌_분석이_5개면_429_ANALYSIS_429() throws Exception {
        long me = createUser("ac4full", "JUNIOR");
        insertAnalysis(me, "DONE", null, false, "a");
        insertAnalysis(me, "DONE", null, true, "삭제해도 센다");
        insertAnalysis(me, "FETCHING", null, false, "c");
        insertAnalysis(me, "SELECTING", null, false, "d");
        insertAnalysis(me, "DONE", LocalDate.now(KST).atStartOfDay(), false, "오늘 0시");

        mockMvc.perform(
                        request(me)
                                .file(file("todo.zip", sampleZip()))
                                .part(part("consent", "true")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("ANALYSIS_429"))
                .andExpect(
                        jsonPath("$.message").value("오늘 분석 요청 횟수(5회)를 모두 사용했습니다. 내일 다시 시도해 주세요."));
        assertThat(countAnalyses(me)).isEqualTo(5);
        verify(pipeline, never()).run(anyLong());
    }

    @Test
    void AC4_FAILED와_어제_분석은_세지_않는다() throws Exception {
        long me = createUser("ac4ok", "JUNIOR");
        LocalDateTime yesterday = LocalDate.now(KST).atStartOfDay().minusMinutes(1);
        insertAnalysis(me, "DONE", null, false, "a");
        insertAnalysis(me, "DONE", null, false, "b");
        insertAnalysis(me, "DONE", null, false, "c");
        insertAnalysis(me, "FAILED", null, false, "실패");
        insertAnalysis(me, "DONE", yesterday, false, "어제 23:59");

        postZip(me);

        insertAnalysis(me, "DONE", null, false, "다섯째");
        mockMvc.perform(
                        request(me)
                                .file(file("todo.zip", sampleZip()))
                                .part(part("consent", "true")))
                .andExpect(status().isTooManyRequests());
    }

    // ───────────── AC12 ─────────────

    @Test
    void AC12_내_미삭제_분석만_최신순으로_페이지_응답을_준다() throws Exception {
        long me = createUser("ac12me", "JUNIOR");
        long other = createUser("ac12other", "JUNIOR");
        LocalDateTime now = LocalDateTime.now(KST).withNano(0);
        long older = insertAnalysis(me, "DONE", now.minusHours(1), false, "older");
        long same1 = insertAnalysis(me, "SELECTING", now, false, "same1");
        long same2 = insertAnalysis(me, "FAILED", now, false, "same2");
        insertAnalysis(me, "DONE", now.plusHours(1), true, "deleted");
        insertAnalysis(other, "DONE", now, false, "other1");
        insertAnalysis(other, "DONE", now, false, "other2");
        jdbc.update("UPDATE analyses SET progress = 25 WHERE id = ?", same1);

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].id").value(same2))
                .andExpect(jsonPath("$.items[1].id").value(same1))
                .andExpect(jsonPath("$.items[2].id").value(older))
                .andExpect(jsonPath("$.items[1].title").value("same1"))
                .andExpect(jsonPath("$.items[1].projectName").value("same1"))
                .andExpect(jsonPath("$.items[1].sourceType").value("ZIP"))
                .andExpect(jsonPath("$.items[1].level").value("BEGINNER"))
                .andExpect(jsonPath("$.items[1].status").value("SELECTING"))
                .andExpect(jsonPath("$.items[1].progress").value(25))
                .andExpect(jsonPath("$.items[1].isPublic").value(false))
                .andExpect(jsonPath("$.items[1].createdAt", endsWith("+09:00")));

        mockMvc.perform(
                        get(URL).param("page", "2")
                                .param("size", "2")
                                .header(HttpHeaders.AUTHORIZATION, bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(older));
    }

    @Test
    void AC12_page나_size가_범위를_벗어나면_400_COMMON_400() throws Exception {
        long me = createUser("ac12range", "JUNIOR");

        for (String[] q : new String[][] {{"1", "0"}, {"1", "101"}, {"0", "20"}}) {
            mockMvc.perform(
                            get(URL).param("page", q[0])
                                    .param("size", q[1])
                                    .header(HttpHeaders.AUTHORIZATION, bearer(me)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_400"));
        }
    }

    // ───────────── AC13 ─────────────

    @Test
    void AC13_내_분석의_상태를_준다() throws Exception {
        long me = createUser("ac13me", "JUNIOR");
        long running = insertAnalysis(me, "FILTERING", null, false, "running");
        jdbc.update("UPDATE analyses SET progress = 15 WHERE id = ?", running);
        long failed = insertAnalysis(me, "FAILED", null, false, "failed");
        jdbc.update(
                "UPDATE analyses SET progress = 15, error_code = 'ANALYSIS_415',"
                        + " error_message = '분석할 수 있는 코드가 없습니다.' WHERE id = ?",
                failed);

        mockMvc.perform(
                        get(URL + "/{id}/status", running)
                                .header(HttpHeaders.AUTHORIZATION, bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisId").value(running))
                .andExpect(jsonPath("$.status").value("FILTERING"))
                .andExpect(jsonPath("$.progress").value(15))
                .andExpect(jsonPath("$.errorCode").value(nullValue()))
                .andExpect(jsonPath("$.errorMessage").value(nullValue()))
                .andExpect(jsonPath("$.updatedAt", endsWith("+09:00")));

        mockMvc.perform(
                        get(URL + "/{id}/status", failed)
                                .header(HttpHeaders.AUTHORIZATION, bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorCode").value("ANALYSIS_415"))
                .andExpect(jsonPath("$.errorMessage").value("분석할 수 있는 코드가 없습니다."));
    }

    /** L-1a AC4: 상태에 퀴즈 상태를 함께 준다. QUIZ 단계 전이면 null. */
    @Test
    void L1a_AC4_상태에_quizStatus를_준다() throws Exception {
        long me = createUser("l1aac4s", "JUNIOR");
        long running = insertAnalysis(me, "EXTRACTING", null, false, "running");
        long generating = insertAnalysis(me, "DONE", null, false, "generating");
        jdbc.update("UPDATE analyses SET quiz_status = 'GENERATING' WHERE id = ?", generating);

        mockMvc.perform(
                        get(URL + "/{id}/status", running)
                                .header(HttpHeaders.AUTHORIZATION, bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizStatus").value(nullValue()));
        mockMvc.perform(
                        get(URL + "/{id}/status", generating)
                                .header(HttpHeaders.AUTHORIZATION, bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizStatus").value("GENERATING"));
    }

    @Test
    void AC13_없거나_남의_것이거나_삭제된_분석이면_404_ANALYSIS_404() throws Exception {
        long me = createUser("ac13404", "JUNIOR");
        long other = createUser("ac13other", "JUNIOR");
        long others = insertAnalysis(other, "DONE", null, false, "남의 것");
        long deleted = insertAnalysis(me, "DONE", null, true, "삭제");

        for (long id : new long[] {others, deleted, others + 100_000}) {
            mockMvc.perform(
                            get(URL + "/{id}/status", id)
                                    .header(HttpHeaders.AUTHORIZATION, bearer(me)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
        }
    }

    // ═════════════ F-05b 저장소 URL (docs/specs/F-05b-analysis-request-url.md) ═════════════

    private static final String GITHUB_URL = "https://github.com/octo/todo-app";

    private long postUrl(long userId, String sourceUrl) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                request(userId)
                                        .part(part("sourceUrl", sourceUrl))
                                        .part(part("consent", "true")))
                        .andExpect(status().isAccepted())
                        .andExpect(jsonPath("$.analysisId").isNumber())
                        .andExpect(jsonPath("$.status").value("FETCHING"))
                        .andReturn();
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("analysisId")
                .asLong();
    }

    private void expectDetail(
            long me, MockMultipartHttpServletRequestBuilder builder, String field, String message)
            throws Exception {
        List<Path> tempFilesBefore = tempFiles();
        mockMvc.perform(builder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details." + field).value(message));
        assertThat(countAnalyses(me)).isZero();
        assertThat(tempFiles()).isEqualTo(tempFilesBefore);
    }

    @Test
    void F05b_AC1_저장소_URL을_보내면_202와_analysisId를_받는다() throws Exception {
        long me = createUser("b1me", "JUNIOR");
        List<Path> tempFilesBefore = tempFiles();

        long id = postUrl(me, "https://github.com/octo/todo-app.git/");

        Map<String, Object> row = analysisRow(id);
        assertThat(((Number) row.get("user_id")).longValue()).isEqualTo(me);
        assertThat(row.get("source_type")).isEqualTo("URL");
        assertThat(row.get("source_url")).isEqualTo(GITHUB_URL);
        assertThat(row.get("project_name")).isEqualTo("todo-app");
        assertThat(row.get("title")).isEqualTo("todo-app");
        assertThat(row.get("level")).isEqualTo("JUNIOR");
        assertThat(row.get("status")).isEqualTo("FETCHING");
        assertThat(((Number) row.get("progress")).intValue()).isZero();
        assertThat(tempFiles()).isEqualTo(tempFilesBefore);
        verify(pipeline, timeout(2000).times(1)).run(id);
    }

    @Test
    void F05b_AC1_GitLab_주소와_level을_저장한다() throws Exception {
        long me = createUser("b1gitlab", "BEGINNER");

        MvcResult result =
                mockMvc.perform(
                                request(me)
                                        .part(
                                                part(
                                                        "sourceUrl",
                                                        "https://gitlab.com/group/sub/my-app"))
                                        .part(part("level", "SENIOR"))
                                        .part(part("consent", "true")))
                        .andExpect(status().isAccepted())
                        .andReturn();
        long id =
                objectMapper
                        .readTree(result.getResponse().getContentAsString())
                        .get("analysisId")
                        .asLong();

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("source_url")).isEqualTo("https://gitlab.com/group/sub/my-app");
        assertThat(row.get("project_name")).isEqualTo("my-app");
        assertThat(row.get("level")).isEqualTo("SENIOR");
    }

    @Test
    void F05b_AC2_ZIP과_주소를_둘_다_보내거나_둘_다_없으면_400() throws Exception {
        long me = createUser("b2both", "JUNIOR");

        expectDetail(
                me,
                request(me)
                        .file(file("todo.zip", sampleZip()))
                        .part(part("sourceUrl", GITHUB_URL))
                        .part(part("consent", "true")),
                "sourceUrl",
                "ZIP 파일과 저장소 주소 중 하나만 보내세요.");
        expectDetail(
                me,
                request(me).part(part("consent", "true")),
                "file",
                "ZIP 파일을 올리거나 저장소 주소를 입력하세요.");
        verify(pipeline, never()).run(anyLong());
    }

    @Test
    void F05b_AC2_규칙을_어긴_주소는_details_sourceUrl() throws Exception {
        long me = createUser("b2url", "JUNIOR");
        Map<String, String> cases =
                Map.of(
                        "https://lab.ssafy.com/octo/todo-app",
                        "GitHub(github.com)나 GitLab(gitlab.com)의 공개 저장소만 분석할 수 있습니다.",
                        "ftp://github.com/octo/todo-app",
                        "https://로 시작하는 GitHub·GitLab 저장소 주소를 입력하세요.",
                        "https://github.com/octo/todo-app/tree/dev",
                        "저장소 주소만 입력하세요. 브랜치·파일 경로(/tree/…, /blob/…)는 지원하지 않습니다.",
                        "https://github.com/octo/..",
                        "저장소 주소 형식이 올바르지 않습니다. 예: https://github.com/owner/repo",
                        "   ",
                        "저장소 주소를 입력하세요.",
                        "https://github.com/octo/" + "r".repeat(477),
                        "저장소 주소는 500자 이하로 입력하세요.");

        for (Map.Entry<String, String> c : cases.entrySet()) {
            expectDetail(
                    me,
                    request(me).part(part("sourceUrl", c.getKey())).part(part("consent", "true")),
                    "sourceUrl",
                    c.getValue());
        }
        verify(pipeline, never()).run(anyLong());
    }

    @Test
    void F05b_AC2_주소_동의_수준_오류를_함께_모은다() throws Exception {
        long me = createUser("b2all", "JUNIOR");

        mockMvc.perform(
                        request(me)
                                .part(part("sourceUrl", "https://octo.github.io/todo-app"))
                                .part(part("level", "EXPERT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.sourceUrl").isString())
                .andExpect(jsonPath("$.details.consent").isString())
                .andExpect(jsonPath("$.details.level").isString())
                .andExpect(jsonPath("$.details.file").doesNotExist());
        assertThat(countAnalyses(me)).isZero();
    }

    @Test
    void F05b_AC3_한도는_ZIP과_URL을_합쳐_세고_토큰이_없으면_401() throws Exception {
        long me = createUser("b3limit", "JUNIOR");
        for (int i = 0; i < 4; i++) {
            insertAnalysis(me, "DONE", null, false, "zip" + i);
        }
        postUrl(me, GITHUB_URL);

        mockMvc.perform(
                        request(me)
                                .part(part("sourceUrl", GITHUB_URL))
                                .part(part("consent", "true")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("ANALYSIS_429"));
        mockMvc.perform(
                        request(me)
                                .file(file("todo.zip", sampleZip()))
                                .part(part("consent", "true")))
                .andExpect(status().isTooManyRequests());
        assertThat(countAnalyses(me)).isEqualTo(5);

        mockMvc.perform(
                        multipart(URL)
                                .part(part("sourceUrl", GITHUB_URL))
                                .part(part("consent", "true")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    @Test
    void F05b_AC12_상세에_sourceUrl이_있고_목록_항목은_그대로다() throws Exception {
        long me = createUser("b12", "JUNIOR");
        long zipId = insertAnalysis(me, "DONE", null, false, "zip-app");
        long urlId = postUrl(me, "https://www.github.com/octo/todo-app");

        mockMvc.perform(get(URL + "/{id}", urlId).header(HttpHeaders.AUTHORIZATION, bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("URL"))
                .andExpect(jsonPath("$.sourceUrl").value(GITHUB_URL));
        mockMvc.perform(get(URL + "/{id}", zipId).header(HttpHeaders.AUTHORIZATION, bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("ZIP"))
                .andExpect(jsonPath("$.sourceUrl").value(nullValue()));
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sourceType").exists())
                .andExpect(jsonPath("$.items[0].sourceUrl").doesNotExist());
    }
}
