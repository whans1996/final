package com.vibenote.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmRequest;
import com.vibenote.llm.LlmResponse;
import com.vibenote.llm.LlmStage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.JsonNode;

/**
 * F-06 EXPLAINING 단계 (docs/specs/F-06-overview-explanation.md AC1·AC2·AC5·AC6). LLM은 실제 빈(사용량 기록 +
 * Mock fixture)을 감싼 spy이고, 단계별로 깨진 응답·LLM_503을 지정한다.
 */
class AnalysisExplainingPipelineTest extends AnalysisTestSupport {

    @Autowired AnalysisPipeline pipeline;

    @MockitoSpyBean LlmClient llmClient;

    @MockitoSpyBean AnalysisService analysisService;

    private static final String CONTROLLER =
            "src/main/java/com/example/todo/controller/UserController.java";

    private long prepare(long userId, String level, Map<String, String> entries)
            throws IOException {
        long id = insertAnalysis(userId, "FETCHING", null, false, "todo-app");
        jdbc.update("UPDATE analyses SET level = ? WHERE id = ?", level, id);
        Path tmp = tempZip(id);
        Files.createDirectories(tmp.getParent());
        Files.write(tmp, zip(entries));
        return id;
    }

    private static Map<String, String> springProject() {
        Map<String, String> entries = new LinkedHashMap<>();
        String java = "todo-app/src/main/java/com/example/todo/";
        entries.put(
                java + "controller/UserController.java",
                """
                @RestController
                @RequestMapping("/api/users")
                class UserController {
                    @GetMapping("/{id}") User get() { return null; }
                    @PostMapping User create() { return null; }
                }
                """);
        entries.put(java + "service/UserService.java", "class UserService {}");
        entries.put(java + "mapper/UserMapper.java", "interface UserMapper {}");
        entries.put(java + "domain/User.java", "class User {}");
        entries.put("todo-app/src/main/resources/mapper/UserMapper.xml", "<mapper/>");
        entries.put("todo-app/src/main/resources/application.yml", "server:\n  port: 8080\n");
        entries.put("todo-app/frontend/src/views/TodoView.vue", "<template><div/></template>");
        return entries;
    }

    private List<Map<String, Object>> files(long analysisId) {
        return jdbc.queryForList(
                "SELECT id, path, role, summary FROM analysis_files WHERE analysis_id = ? ORDER BY"
                        + " id",
                analysisId);
    }

    private JsonNode overview(long analysisId) {
        String json =
                jdbc.queryForObject(
                        "SELECT overview FROM analyses WHERE id = ?", String.class, analysisId);
        return json == null ? null : objectMapper.readTree(json);
    }

    private static LlmRequest stage(LlmStage stage) {
        return argThat(r -> r != null && r.stage() == stage);
    }

    private List<LlmRequest> requests() {
        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmClient, atLeast(0)).complete(captor.capture());
        return captor.getAllValues();
    }

    private static int fileBlocks(String prompt) {
        // 템플릿 규칙 문장의 `<<<FILE no=...>>>`는 세지 않는다 (숫자 번호가 붙은 구분자만)
        Matcher m = Pattern.compile("<<<FILE no=\\d+ ").matcher(prompt);
        int count = 0;
        while (m.find()) {
            count++;
        }
        return count;
    }

    @Test
    void AC1_SELECTING_뒤에_EXPLAINING을_거쳐_DONE이_된다() throws IOException {
        long id = prepare(createUser("f06ac1", "JUNIOR"), "JUNIOR", springProject());

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("DONE");
        assertThat(row.get("progress")).isEqualTo(100);
        InOrder order = inOrder(analysisService);
        order.verify(analysisService).advance(id, AnalysisStatus.SELECTING, 25);
        order.verify(analysisService).advance(id, AnalysisStatus.EXPLAINING, 40);
        // F-08이 EXPLAINING 뒤에 EXTRACTING을 끼웠다 (F-08 AC1)
        order.verify(analysisService).advance(id, AnalysisStatus.EXTRACTING, 70);
        // F-14가 EXTRACTING 뒤에 QUIZ를 끼웠다 (F-14 AC1)
        order.verify(analysisService).advance(id, AnalysisStatus.QUIZ, 85);
        order.verify(analysisService).complete(eq(id));

        List<Map<String, Object>> files = files(id);
        assertThat(files).hasSize(7);
        assertThat(files).allSatisfy(f -> assertThat(f.get("role")).isNotNull());
        assertThat(files).allSatisfy(f -> assertThat(f.get("summary")).isNotNull());
        long controllerId =
                files.stream()
                        .filter(f -> CONTROLLER.equals(f.get("path")))
                        .map(f -> ((Number) f.get("id")).longValue())
                        .findFirst()
                        .orElseThrow();

        JsonNode overview = overview(id);
        assertThat(overview.path("summary").asString()).startsWith("테스트 전용 fixture");
        assertThat(overview.path("techStack").size()).isEqualTo(2);
        assertThat(overview.path("layers").size()).isEqualTo(2);
        assertThat(overview.path("readingOrder").get(0).path("fileId").asLong())
                .isEqualTo(controllerId);
        // MySQL JSON 컬럼은 키 순서를 바꿔 저장하므로 트리로 비교한다
        assertThat(overview.path("apis"))
                .isEqualTo(
                        objectMapper.readTree(
                                ("[{\"method\":\"POST\",\"path\":\"/api/users\",\"fileId\":%d},"
                                     + "{\"method\":\"GET\",\"path\":\"/api/users/{id}\","
                                     + "\"fileId\":%d}]")
                                        .formatted(controllerId, controllerId)));
    }

    @Test
    void AC2_14개_파일은_개요_1회와_6_6_2개_묶음_3회로_부르고_사용량과_진행률을_남긴다() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        for (int i = 0; i < 14; i++) {
            entries.put(
                    "p/svc/S%02dService.java".formatted(i), "class S%02dService {}".formatted(i));
        }
        long userId = createUser("f06ac2", "JUNIOR");
        long id = prepare(userId, "SENIOR", entries);

        pipeline.run(id);

        assertThat(analysisRow(id).get("status")).isEqualTo("DONE");
        List<LlmRequest> requests = requests();
        assertThat(requests)
                .extracting(LlmRequest::stage)
                .containsExactly(
                        LlmStage.OVERVIEW,
                        LlmStage.FILE,
                        LlmStage.FILE,
                        LlmStage.FILE,
                        LlmStage.CONCEPT,
                        LlmStage.QUIZ);
        assertThat(requests)
                .allSatisfy(
                        r -> {
                            assertThat(r.level()).isEqualTo(com.vibenote.common.Level.SENIOR);
                            assertThat(r.userId()).isEqualTo(userId);
                            assertThat(r.analysisId()).isEqualTo(id);
                        });
        assertThat(requests.subList(1, 4))
                .extracting(r -> fileBlocks(r.prompt()))
                .containsExactly(6, 6, 2);
        // 묶음은 analysis_files.id 오름차순
        List<Map<String, Object>> files = files(id);
        assertThat(requests.get(1).prompt()).contains("path=" + files.get(0).get("path") + " ");
        assertThat(requests.get(3).prompt()).contains("path=" + files.get(13).get("path") + " ");
        // 숙련 템플릿 (prompts/<stage>/senior.md)
        assertThat(requests.get(0).prompt()).contains("숙련 개발자");

        assertThat(
                        jdbc.queryForList(
                                "SELECT stage, user_id FROM llm_usage_logs WHERE analysis_id = ?"
                                        + " ORDER BY id",
                                id))
                .extracting(r -> r.get("stage") + " " + r.get("user_id"))
                .containsExactly(
                        "overview " + userId,
                        "file " + userId,
                        "file " + userId,
                        "file " + userId,
                        "concept " + userId,
                        "quiz " + userId);

        InOrder order = inOrder(analysisService);
        order.verify(analysisService).advance(id, AnalysisStatus.EXPLAINING, 40);
        order.verify(analysisService).advance(id, AnalysisStatus.EXPLAINING, 45);
        order.verify(analysisService).advance(id, AnalysisStatus.EXPLAINING, 53);
        order.verify(analysisService).advance(id, AnalysisStatus.EXPLAINING, 61);
        order.verify(analysisService).advance(id, AnalysisStatus.EXPLAINING, 69);
        order.verify(analysisService).advance(id, AnalysisStatus.EXTRACTING, 70);
        // F-14가 EXTRACTING 뒤에 QUIZ를 끼웠다 (F-14 AC1)
        order.verify(analysisService).advance(id, AnalysisStatus.QUIZ, 85);
        order.verify(analysisService).complete(eq(id));
    }

    @Test
    void AC5_개요가_두_번_다_형식이_틀리면_FAILED_LLM_502() throws IOException {
        long id = prepare(createUser("f06ac5o", "JUNIOR"), "JUNIOR", springProject());
        doReturn(new LlmResponse("{\"summary\":1}", "fake", 0, 0))
                .when(llmClient)
                .complete(stage(LlmStage.OVERVIEW));

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("error_code")).isEqualTo("LLM_502");
        assertThat(row.get("error_message"))
                .isEqualTo("AI 응답 형식이 올바르지 않아 설명을 만들지 못했습니다. 다시 요청해 주세요.");
        assertThat(row.get("progress")).isEqualTo(40);
        assertThat(requests())
                .extracting(LlmRequest::stage)
                .containsExactly(LlmStage.OVERVIEW, LlmStage.OVERVIEW);
        assertThat(tempZip(id)).doesNotExist();
    }

    @Test
    void AC5_파일_묶음이_두_번_다_틀리면_그_묶음만_NULL이고_DONE() throws IOException {
        long id = prepare(createUser("f06ac5f", "JUNIOR"), "JUNIOR", springProject());
        doReturn(new LlmResponse("not json", "fake", 0, 0))
                .when(llmClient)
                .complete(stage(LlmStage.FILE));

        pipeline.run(id);

        assertThat(analysisRow(id).get("status")).isEqualTo("DONE");
        assertThat(overview(id)).isNotNull();
        assertThat(files(id)).allSatisfy(f -> assertThat(f.get("summary")).isNull());
        // 7개 = 6 + 1 묶음, 묶음마다 1회 재시도
        assertThat(requests())
                .extracting(LlmRequest::stage)
                .containsExactly(
                        LlmStage.OVERVIEW,
                        LlmStage.FILE,
                        LlmStage.FILE,
                        LlmStage.FILE,
                        LlmStage.FILE,
                        LlmStage.CONCEPT,
                        LlmStage.QUIZ);
    }

    @Test
    void AC6_개요가_LLM_503이면_재시도_없이_FAILED_LLM_503() throws IOException {
        long id = prepare(createUser("f06ac6o", "JUNIOR"), "JUNIOR", springProject());
        doThrow(new BusinessException(ErrorCode.LLM_503))
                .when(llmClient)
                .complete(stage(LlmStage.OVERVIEW));

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("error_code")).isEqualTo("LLM_503");
        assertThat(row.get("error_message")).isEqualTo("AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요.");
        assertThat(row.get("progress")).isEqualTo(40);
        assertThat(overview(id)).isNull();
        assertThat(requests()).hasSize(1);
    }

    @Test
    void AC6_파일_묶음이_LLM_503이면_그_묶음만_NULL이고_계속한다() throws IOException {
        long id = prepare(createUser("f06ac6f", "JUNIOR"), "JUNIOR", springProject());
        doThrow(new BusinessException(ErrorCode.LLM_503))
                .when(llmClient)
                .complete(stage(LlmStage.FILE));

        pipeline.run(id);

        assertThat(analysisRow(id).get("status")).isEqualTo("DONE");
        assertThat(files(id)).allSatisfy(f -> assertThat(f.get("role")).isNull());
        assertThat(requests())
                .extracting(LlmRequest::stage)
                .containsExactly(
                        LlmStage.OVERVIEW,
                        LlmStage.FILE,
                        LlmStage.FILE,
                        LlmStage.CONCEPT,
                        LlmStage.QUIZ);
    }
}
