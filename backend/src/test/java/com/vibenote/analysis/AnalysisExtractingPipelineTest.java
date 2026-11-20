package com.vibenote.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import com.vibenote.analysis.concept.ConceptLinkService;
import com.vibenote.common.Level;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * F-08 EXTRACTING 단계 (docs/specs/F-08-concept-extraction.md AC1·AC2·AC6). LLM은 실제 빈(사용량 기록 + Mock
 * fixture)을 감싼 spy이고, CONCEPT 단계 응답만 지정한다. 시드 카드에 기대지 않고 {@code f08-test-} 카드를 직접 넣는다.
 */
class AnalysisExtractingPipelineTest extends AnalysisTestSupport {

    @Autowired AnalysisPipeline pipeline;

    @MockitoSpyBean LlmClient llmClient;

    @MockitoSpyBean AnalysisService analysisService;

    @MockitoSpyBean ConceptLinkService conceptLinkService;

    @AfterEach
    void cleanUpConcepts() {
        jdbc.update("DELETE FROM concepts WHERE name LIKE 'f08-test-%'");
    }

    private long prepare(long userId, String level) throws IOException {
        long id = insertAnalysis(userId, "FETCHING", null, false, "todo-app");
        jdbc.update("UPDATE analyses SET level = ? WHERE id = ?", level, id);
        Map<String, String> entries = new LinkedHashMap<>();
        String java = "todo-app/src/main/java/com/example/todo/";
        entries.put(
                java + "controller/TodoController.java",
                "@RestController\nclass TodoController {\n  private final TodoService service;\n}");
        entries.put(java + "service/TodoService.java", "@Service\nclass TodoService {}");
        Path tmp = tempZip(id);
        Files.createDirectories(tmp.getParent());
        Files.write(tmp, zip(entries));
        return id;
    }

    private long insertConcept(String name, String status) {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, status, source)"
                        + " VALUES (?, 'spring', 'JUNIOR', '요약', ?, 'SEED')",
                name,
                status);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void answerConcept(String content) {
        doReturn(new LlmResponse(content, "fake", 0, 0))
                .when(llmClient)
                .complete(argThat(r -> r != null && r.stage() == LlmStage.CONCEPT));
    }

    private List<LlmRequest> requests() {
        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmClient, atLeast(0)).complete(captor.capture());
        return captor.getAllValues();
    }

    private List<Map<String, Object>> links(long analysisId) {
        return jdbc.queryForList(
                "SELECT ac.concept_id, f.path, ac.line_hint, ac.context_note FROM analysis_concepts"
                    + " ac LEFT JOIN analysis_files f ON f.id = ac.file_id WHERE ac.analysis_id = ?"
                    + " ORDER BY ac.concept_id",
                analysisId);
    }

    @Test
    void AC1_EXPLAINING_뒤에_EXTRACTING을_거쳐_DONE이_되고_연결을_저장한다() throws IOException {
        long di = insertConcept("f08-test-의존성 주입 (DI)", "PUBLISHED");
        long id = prepare(createUser("f08ac1", "JUNIOR"), "JUNIOR");
        answerConcept(
                """
                {"concepts": [
                  {"conceptId": %d, "fileNo": 1, "lineHint": "L3", "contextNote": "생성자로 받습니다."},
                  {"new": {"name": "f08-test-스테레오타입", "category": "spring", "summary": "s"},
                   "fileNo": 2, "contextNote": "서비스 빈"}
                ]}
                """
                        .formatted(di));

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("DONE");
        assertThat(row.get("progress")).isEqualTo(100);
        assertThat(row.get("error_code")).isNull();
        InOrder order = inOrder(analysisService);
        order.verify(analysisService).advance(id, AnalysisStatus.EXPLAINING, 40);
        order.verify(analysisService).advance(id, AnalysisStatus.EXTRACTING, 70);
        // F-14가 EXTRACTING 뒤에 QUIZ를 끼웠다 (F-14 AC1)
        order.verify(analysisService).advance(id, AnalysisStatus.QUIZ, 85);
        order.verify(analysisService).complete(eq(id));

        long draft =
                jdbc.queryForObject(
                        "SELECT id FROM concepts WHERE name = 'f08-test-스테레오타입' AND status ="
                                + " 'DRAFT' AND source = 'AI'",
                        Long.class);
        assertThat(links(id))
                .extracting(
                        r ->
                                r.get("concept_id")
                                        + " "
                                        + r.get("path")
                                        + " "
                                        + r.get("line_hint")
                                        + " "
                                        + r.get("context_note"))
                .containsExactly(
                        di
                                + " src/main/java/com/example/todo/controller/TodoController.java"
                                + " L3 생성자로 받습니다.",
                        draft
                                + " src/main/java/com/example/todo/service/TodoService.java null"
                                + " 서비스 빈");
    }

    @Test
    void AC2_CONCEPT를_분석_수준으로_1회_부르고_사용량을_남긴다() throws IOException {
        long published = insertConcept("f08-test-공개", "PUBLISHED");
        long hidden = insertConcept("f08-test-숨김", "HIDDEN");
        long userId = createUser("f08ac2", "JUNIOR");
        long id = prepare(userId, "BEGINNER");

        pipeline.run(id);

        List<LlmRequest> concept =
                requests().stream().filter(r -> r.stage() == LlmStage.CONCEPT).toList();
        assertThat(concept).hasSize(1);
        LlmRequest request = concept.get(0);
        assertThat(request.level()).isEqualTo(Level.BEGINNER);
        assertThat(request.userId()).isEqualTo(userId);
        assertThat(request.analysisId()).isEqualTo(id);
        assertThat(request.prompt())
                .contains("입문자")
                .contains(published + " | f08-test-공개 | spring")
                .doesNotContain(hidden + " | f08-test-숨김")
                .contains("1. src/main/java/com/example/todo/controller/TodoController.java")
                .contains("1: @RestController\n2: class TodoController {");
        assertThat(
                        jdbc.queryForList(
                                "SELECT user_id FROM llm_usage_logs WHERE analysis_id = ? AND stage"
                                        + " = 'concept'",
                                Long.class,
                                id))
                .containsExactly(userId);
    }

    @Test
    void AC6_형식이_두_번_다_틀리면_연결_없이_DONE() throws IOException {
        long id = prepare(createUser("f08ac6f", "JUNIOR"), "JUNIOR");
        answerConcept("{\"concepts\": \"x\"}");

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("DONE");
        assertThat(row.get("error_code")).isNull();
        assertThat(links(id)).isEmpty();
        assertThat(requests().stream().filter(r -> r.stage() == LlmStage.CONCEPT)).hasSize(2);
    }

    @Test
    void AC6_LLM_503이면_재시도_없이_연결_없이_DONE() throws IOException {
        long id = prepare(createUser("f08ac6l", "JUNIOR"), "JUNIOR");
        doThrow(new BusinessException(ErrorCode.LLM_503))
                .when(llmClient)
                .complete(argThat(r -> r != null && r.stage() == LlmStage.CONCEPT));

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("DONE");
        assertThat(row.get("error_code")).isNull();
        assertThat(requests().stream().filter(r -> r.stage() == LlmStage.CONCEPT)).hasSize(1);
    }

    @Test
    void AC6_concepts가_빈_배열이면_연결_0개로_DONE() throws IOException {
        long id = prepare(createUser("f08ac6e", "JUNIOR"), "JUNIOR");

        pipeline.run(id);

        assertThat(analysisRow(id).get("status")).isEqualTo("DONE");
        assertThat(links(id)).isEmpty();
        assertThat(requests().stream().filter(r -> r.stage() == LlmStage.CONCEPT)).hasSize(1);
    }

    @Test
    void AC6_저장_중_예기치_못한_예외는_FAILED_ANALYSIS_500() throws IOException {
        long id = prepare(createUser("f08ac6d", "JUNIOR"), "JUNIOR");
        doThrow(new DataIntegrityViolationException("boom"))
                .when(conceptLinkService)
                .save(anyLong(), any());

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("error_code")).isEqualTo("ANALYSIS_500");
        assertThat(row.get("progress")).isEqualTo(70);
    }
}
