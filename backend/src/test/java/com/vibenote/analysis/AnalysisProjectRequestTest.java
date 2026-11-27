package com.vibenote.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;

/**
 * P-1a 분석 요청을 프로젝트·버전에 묶기 (docs/specs/P-1a-project-versions.md AC1·AC4·AC5). F-05a의 AC 번호와 섞이지 않게
 * AnalysisControllerTest와 나눠 둔다. 저장소 버전은 FETCHING에서 정하므로(AC2) 여기서는 파이프라인을 실행하지 않는다.
 */
class AnalysisProjectRequestTest extends AnalysisTestSupport {

    private static final String URL = "/api/analyses";
    private static final String TODO = "https://github.com/octo/todo-app";

    @MockitoBean AnalysisPipeline pipeline;

    private static MockPart part(String name, String value) {
        return new MockPart(name, value.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartHttpServletRequestBuilder request(long userId) {
        return (MockMultipartHttpServletRequestBuilder)
                multipart(URL).header(HttpHeaders.AUTHORIZATION, bearer(userId));
    }

    private MockMultipartHttpServletRequestBuilder url(long userId, String sourceUrl) {
        return request(userId).part(part("sourceUrl", sourceUrl)).part(part("consent", "true"));
    }

    private MockMultipartHttpServletRequestBuilder zipFile(long userId, String filename) {
        return request(userId)
                .file(new MockMultipartFile("file", filename, "application/zip", sampleZip()))
                .part(part("consent", "true"));
    }

    private JsonNode accepted(MockMultipartHttpServletRequestBuilder builder) throws Exception {
        MvcResult result =
                mockMvc.perform(builder)
                        .andExpect(status().isAccepted())
                        .andExpect(jsonPath("$.projectId").isNumber())
                        .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private Map<String, Object> projectRow(long projectId) {
        return jdbc.queryForMap(
                "SELECT user_id, source_type, source_url, name FROM projects WHERE id = ?",
                projectId);
    }

    private Map<String, Object> versionOf(long analysisId) {
        return jdbc.queryForMap(
                "SELECT v.project_id, v.number, v.commit_sha, v.commit_message, v.committed_at"
                        + " FROM analyses a JOIN project_versions v ON v.id = a.project_version_id"
                        + " WHERE a.id = ?",
                analysisId);
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    // ───────────── AC1 ─────────────

    @Test
    void AC1_처음_분석하는_저장소면_저장소_이름으로_프로젝트를_만들고_projectId를_준다() throws Exception {
        long me = createUser("p1ac1new", "JUNIOR");

        JsonNode body = accepted(url(me, TODO));

        long projectId = body.get("projectId").asLong();
        long analysisId = body.get("analysisId").asLong();
        assertThat(projectRow(projectId))
                .containsEntry("user_id", me)
                .containsEntry("source_type", "URL")
                .containsEntry("source_url", TODO)
                .containsEntry("name", "todo-app");
        Map<String, Object> row = analysisRow(analysisId);
        assertThat(((Number) row.get("project_id")).longValue()).isEqualTo(projectId);
        assertThat(row.get("project_version_id")).isNull();
    }

    @Test
    void AC1_정규화한_주소가_같으면_같은_프로젝트에_붙고_다른_회원의_프로젝트와는_섞이지_않는다() throws Exception {
        long me = createUser("p1ac1same", "JUNIOR");
        long other = createUser("p1ac1other", "JUNIOR");

        long first = accepted(url(me, TODO)).get("projectId").asLong();
        long second =
                accepted(url(me, "http://www.github.com/octo/todo-app.git/"))
                        .get("projectId")
                        .asLong();
        long others = accepted(url(other, TODO)).get("projectId").asLong();

        assertThat(second).isEqualTo(first);
        assertThat(others).isNotEqualTo(first);
        assertThat(count("SELECT COUNT(*) FROM projects WHERE user_id = ?", me)).isEqualTo(1);
        assertThat(projectRow(others)).containsEntry("user_id", other);
    }

    // ───────────── AC4 ─────────────

    @Test
    void AC4_projectId_없이_ZIP을_올리면_파일_이름으로_새_프로젝트와_버전_1이_생긴다() throws Exception {
        long me = createUser("p1ac4new", "JUNIOR");

        JsonNode body = accepted(zipFile(me, "todo-app.zip"));
        JsonNode again = accepted(zipFile(me, "todo-app.zip"));

        long projectId = body.get("projectId").asLong();
        assertThat(projectRow(projectId))
                .containsEntry("source_type", "ZIP")
                .containsEntry("source_url", null)
                .containsEntry("name", "todo-app");
        Map<String, Object> version = versionOf(body.get("analysisId").asLong());
        assertThat(((Number) version.get("project_id")).longValue()).isEqualTo(projectId);
        assertThat(version)
                .containsEntry("number", 1)
                .containsEntry("commit_sha", null)
                .containsEntry("commit_message", null)
                .containsEntry("committed_at", null);
        // ZIP은 projectId를 보내지 않으면 같은 이름이어도 새 프로젝트다
        assertThat(again.get("projectId").asLong()).isNotEqualTo(projectId);
    }

    @Test
    void AC4_내_ZIP_프로젝트의_projectId를_보내면_그_프로젝트의_새_버전이_생긴다() throws Exception {
        long me = createUser("p1ac4next", "JUNIOR");
        long projectId = accepted(zipFile(me, "todo-app.zip")).get("projectId").asLong();

        JsonNode body =
                accepted(
                        zipFile(me, "todo-app-v2.zip")
                                .part(part("projectId", String.valueOf(projectId))));

        assertThat(body.get("projectId").asLong()).isEqualTo(projectId);
        Map<String, Object> version = versionOf(body.get("analysisId").asLong());
        assertThat(((Number) version.get("project_id")).longValue()).isEqualTo(projectId);
        assertThat(version).containsEntry("number", 2).containsEntry("commit_sha", null);
        assertThat(projectRow(projectId)).containsEntry("name", "todo-app");
        assertThat(count("SELECT COUNT(*) FROM projects WHERE user_id = ?", me)).isEqualTo(1);
    }

    @Test
    void AC4_저장소_프로젝트에_projectId와_같은_주소를_보내면_그_프로젝트에_붙는다() throws Exception {
        long me = createUser("p1ac4url", "JUNIOR");
        long projectId = accepted(url(me, TODO)).get("projectId").asLong();

        JsonNode body =
                accepted(
                        url(me, "https://github.com/octo/todo-app")
                                .part(part("projectId", String.valueOf(projectId))));

        assertThat(body.get("projectId").asLong()).isEqualTo(projectId);
    }

    // ───────────── AC5 ─────────────

    @Test
    void AC5_없거나_남의_프로젝트면_404_PROJECT_404이고_아무것도_생기지_않는다() throws Exception {
        long me = createUser("p1ac5me", "JUNIOR");
        long other = createUser("p1ac5other", "JUNIOR");
        long othersZip = accepted(zipFile(other, "secret.zip")).get("projectId").asLong();
        long othersUrl = accepted(url(other, TODO)).get("projectId").asLong();

        for (var builder :
                new MockMultipartHttpServletRequestBuilder[] {
                    zipFile(me, "a.zip").part(part("projectId", "999999999")),
                    zipFile(me, "a.zip").part(part("projectId", String.valueOf(othersZip))),
                    url(me, TODO).part(part("projectId", String.valueOf(othersUrl)))
                }) {
            mockMvc.perform(builder)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PROJECT_404"))
                    .andExpect(jsonPath("$.message").value("프로젝트를 찾을 수 없습니다."));
        }

        assertThat(count("SELECT COUNT(*) FROM analyses WHERE user_id = ?", me)).isZero();
        assertThat(count("SELECT COUNT(*) FROM projects WHERE user_id = ?", me)).isZero();
        assertThat(count("SELECT COUNT(*) FROM project_versions WHERE project_id = ?", othersZip))
                .isEqualTo(1);
    }

    @Test
    void AC5_형식이_틀리거나_입력_방식_주소가_프로젝트와_다르면_400_details_projectId() throws Exception {
        long me = createUser("p1ac5bad", "JUNIOR");
        long zipProject = accepted(zipFile(me, "todo-app.zip")).get("projectId").asLong();
        long urlProject = accepted(url(me, TODO)).get("projectId").asLong();
        long analysesBefore = count("SELECT COUNT(*) FROM analyses WHERE user_id = ?", me);
        long versionsBefore = count("SELECT COUNT(*) FROM project_versions");

        expectProjectIdError(zipFile(me, "a.zip").part(part("projectId", "abc")), null);
        expectProjectIdError(
                zipFile(me, "a.zip").part(part("projectId", String.valueOf(urlProject))),
                "저장소 주소 프로젝트에는 ZIP을 올릴 수 없습니다.");
        expectProjectIdError(
                url(me, "https://github.com/octo/other-app")
                        .part(part("projectId", String.valueOf(urlProject))),
                "이 프로젝트의 저장소 주소와 다릅니다.");
        expectProjectIdError(
                url(me, TODO).part(part("projectId", String.valueOf(zipProject))),
                "ZIP 프로젝트는 ZIP 파일로만 새 버전을 만들 수 있습니다.");

        assertThat(count("SELECT COUNT(*) FROM analyses WHERE user_id = ?", me))
                .isEqualTo(analysesBefore);
        assertThat(count("SELECT COUNT(*) FROM projects WHERE user_id = ?", me)).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM project_versions")).isEqualTo(versionsBefore);
    }

    private void expectProjectIdError(
            MockMultipartHttpServletRequestBuilder builder, String message) throws Exception {
        var result =
                mockMvc.perform(builder)
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("COMMON_400"))
                        .andExpect(jsonPath("$.details.projectId").isString());
        if (message != null) {
            result.andExpect(jsonPath("$.details.projectId").value(message));
        }
    }

    // ───────────── AC12 분석 상세·목록의 프로젝트·버전 ─────────────

    private static final String SHA = "9f2c4e1a7b3d5f60812c9e4a1b2c3d4e5f607182";

    private ResultActions detail(long viewerId, long analysisId) throws Exception {
        return mockMvc.perform(
                get(URL + "/" + analysisId).header(HttpHeaders.AUTHORIZATION, bearer(viewerId)));
    }

    /** 저장소 분석에 커밋 정보가 있는 버전을 붙인다 (FETCHING이 끝난 것처럼). */
    private long attachCommitVersion(long analysisId, long projectId) {
        jdbc.update(
                "INSERT INTO project_versions (project_id, number, commit_sha, commit_message,"
                        + " committed_at) VALUES (?, 1, ?, 'feat: JWT 로그인', '2026-11-28 14:02:11')",
                projectId,
                SHA);
        long versionId = count("SELECT id FROM project_versions WHERE project_id = ?", projectId);
        jdbc.update(
                "UPDATE analyses SET project_version_id = ? WHERE id = ?", versionId, analysisId);
        return versionId;
    }

    @Test
    void AC12_분석_상세에_프로젝트_이름과_버전이_있고_프로젝트_이름을_바꾸면_따라_바뀐다() throws Exception {
        long me = createUser("p1ac12zip", "JUNIOR");
        JsonNode body = accepted(zipFile(me, "todo-app.zip"));
        long analysisId = body.get("analysisId").asLong();
        long projectId = body.get("projectId").asLong();
        long versionId = count("SELECT project_version_id FROM analyses WHERE id = ?", analysisId);
        jdbc.update("UPDATE projects SET name = '할 일 앱' WHERE id = ?", projectId);

        detail(me, analysisId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project.id").value(projectId))
                .andExpect(jsonPath("$.project.name").value("할 일 앱"))
                .andExpect(jsonPath("$.title").value("todo-app"))
                .andExpect(jsonPath("$.version.id").value(versionId))
                .andExpect(jsonPath("$.version.number").value(1))
                .andExpect(jsonPath("$.version.commitSha").value(nullValue()))
                .andExpect(jsonPath("$.version.commitMessage").value(nullValue()))
                .andExpect(jsonPath("$.version.committedAt").value(nullValue()));
    }

    @Test
    void AC12_저장소_분석은_FETCHING_전에는_version이_null이고_붙으면_커밋_정보가_있다() throws Exception {
        long me = createUser("p1ac12url", "JUNIOR");
        JsonNode body = accepted(url(me, TODO));
        long analysisId = body.get("analysisId").asLong();
        long projectId = body.get("projectId").asLong();

        detail(me, analysisId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project.id").value(projectId))
                .andExpect(jsonPath("$.project.name").value("todo-app"))
                .andExpect(jsonPath("$.version").value(nullValue()));

        long versionId = attachCommitVersion(analysisId, projectId);
        detail(me, analysisId)
                .andExpect(jsonPath("$.version.id").value(versionId))
                .andExpect(jsonPath("$.version.number").value(1))
                .andExpect(jsonPath("$.version.commitSha").value(SHA))
                .andExpect(jsonPath("$.version.commitMessage").value("feat: JWT 로그인"))
                .andExpect(jsonPath("$.version.committedAt").value("2026-11-28T14:02:11+09:00"));
    }

    @Test
    void AC12_공개_분석을_보는_다른_회원에게도_같은_프로젝트_버전을_준다() throws Exception {
        long me = createUser("p1ac12owner", "JUNIOR");
        long viewer = createUser("p1ac12viewer", "JUNIOR");
        JsonNode body = accepted(url(me, TODO));
        long analysisId = body.get("analysisId").asLong();
        long projectId = body.get("projectId").asLong();
        long versionId = attachCommitVersion(analysisId, projectId);
        jdbc.update(
                "UPDATE analyses SET status = 'DONE', progress = 100, is_public = TRUE WHERE id ="
                        + " ?",
                analysisId);

        detail(viewer, analysisId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mine").value(false))
                .andExpect(jsonPath("$.project.id").value(projectId))
                .andExpect(jsonPath("$.project.name").value("todo-app"))
                .andExpect(jsonPath("$.version.id").value(versionId))
                .andExpect(jsonPath("$.version.commitSha").value(SHA));
    }

    @Test
    void AC12_분석_목록_항목에_projectId와_versionNumber가_있다() throws Exception {
        long me = createUser("p1ac12list", "JUNIOR");
        JsonNode zip = accepted(zipFile(me, "todo-app.zip"));
        jdbc.update(
                "UPDATE analyses SET created_at = created_at - INTERVAL 1 MINUTE WHERE id = ?",
                zip.get("analysisId").asLong());
        JsonNode repo = accepted(url(me, TODO));

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(repo.get("analysisId").asLong()))
                .andExpect(jsonPath("$.items[0].projectId").value(repo.get("projectId").asLong()))
                .andExpect(jsonPath("$.items[0].versionNumber").value(nullValue()))
                .andExpect(jsonPath("$.items[1].id").value(zip.get("analysisId").asLong()))
                .andExpect(jsonPath("$.items[1].projectId").value(zip.get("projectId").asLong()))
                .andExpect(jsonPath("$.items[1].versionNumber").value(1));
    }
}
