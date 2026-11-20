package com.vibenote.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import com.vibenote.analysis.concept.AnalysisConceptService;
import com.vibenote.analysis.concept.LinkedConcept;
import com.vibenote.common.Level;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmRequest;
import com.vibenote.llm.LlmResponse;
import com.vibenote.llm.LlmStage;
import com.vibenote.quiz.QuizSaveService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
 * F-14 QUIZ 단계 (docs/specs/F-14-code-quiz.md AC1·AC2·AC4). LLM은 실제 빈(사용량 기록 + Mock fixture)을 감싼
 * spy이고, CONCEPT·QUIZ 단계 응답만 지정한다. 시드 카드에 기대지 않고 {@code f14-test-} 카드를 직접 넣는다.
 */
class AnalysisQuizPipelineTest extends AnalysisTestSupport {

    @Autowired AnalysisPipeline pipeline;

    @Autowired AnalysisConceptService analysisConceptService;

    @MockitoSpyBean LlmClient llmClient;

    @MockitoSpyBean AnalysisService analysisService;

    @MockitoSpyBean QuizSaveService quizSaveService;

    @AfterEach
    void cleanUpConcepts() {
        jdbc.update("DELETE FROM concepts WHERE name LIKE 'f14-test-%'");
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

    private void answer(LlmStage stage, String content) {
        doReturn(new LlmResponse(content, "fake", 0, 0))
                .when(llmClient)
                .complete(argThat(r -> r != null && r.stage() == stage));
    }

    /** 정답이 첫 번째인 유효 문항 {@code count}개. 첫 문항만 {@code firstConceptId}를 가리킨다. */
    static String quizJson(int count, Object firstConceptId) {
        List<String> items = new ArrayList<>();
        for (int n = 1; n <= count; n++) {
            items.add(
                    """
                    {"conceptId": %s, "question": "질문 %d", "choices": ["a%d", "b%d", "c%d", "d%d"],
                     "answerIndex": 0, "explanation": "해설 %d"}\
                    """
                            .formatted(n == 1 ? firstConceptId : "null", n, n, n, n, n, n));
        }
        return "{\"questions\": [" + String.join(",", items) + "]}";
    }

    private List<LlmRequest> requests(LlmStage stage) {
        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmClient, atLeast(0)).complete(captor.capture());
        return captor.getAllValues().stream().filter(r -> r.stage() == stage).toList();
    }

    private int quizCount(long analysisId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM quizzes WHERE analysis_id = ?", Integer.class, analysisId);
    }

    @Test
    void AC1_EXTRACTING_뒤에_QUIZ를_거쳐_DONE이_되고_문항을_저장한다() throws IOException {
        long di = insertConcept("f14-test-의존성 주입", "PUBLISHED");
        long id = prepare(createUser("f14ac1", "JUNIOR"), "JUNIOR");
        answer(
                LlmStage.CONCEPT,
                "{\"concepts\": [{\"conceptId\": %d, \"fileNo\": 1}]}".formatted(di));
        answer(LlmStage.QUIZ, quizJson(6, di));

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("DONE");
        assertThat(row.get("progress")).isEqualTo(100);
        assertThat(row.get("error_code")).isNull();
        InOrder order = inOrder(analysisService);
        order.verify(analysisService).advance(id, AnalysisStatus.EXTRACTING, 70);
        order.verify(analysisService).advance(id, AnalysisStatus.QUIZ, 85);
        order.verify(analysisService).complete(eq(id));

        assertThat(quizCount(id)).isEqualTo(1);
        List<Map<String, Object>> questions =
                jdbc.queryForList(
                        "SELECT q.concept_id, q.question, q.choices, q.answer_index, q.explanation"
                                + " FROM quiz_questions q JOIN quizzes z ON z.id = q.quiz_id"
                                + " WHERE z.analysis_id = ? ORDER BY q.id",
                        id);
        assertThat(questions)
                .extracting(r -> r.get("question"))
                .containsExactly("질문 1", "질문 2", "질문 3", "질문 4", "질문 5", "질문 6");
        Map<String, Object> first = questions.get(0);
        assertThat(((Number) first.get("concept_id")).longValue()).isEqualTo(di);
        assertThat(questions.get(1).get("concept_id")).isNull();
        assertThat(first.get("explanation")).isEqualTo("해설 1");
        String choices = first.get("choices").toString();
        assertThat(choices).contains("\"a1\"", "\"b1\"", "\"c1\"", "\"d1\"");
        // 셔플 뒤에도 정답 위치가 "a1"을 가리킨다
        int answer = ((Number) first.get("answer_index")).intValue();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT JSON_UNQUOTE(JSON_EXTRACT(q.choices, CONCAT('$[', ?, ']')))"
                                    + " FROM quiz_questions q JOIN quizzes z ON z.id = q.quiz_id"
                                    + " WHERE z.analysis_id = ? ORDER BY q.id LIMIT 1",
                                String.class,
                                answer,
                                id))
                .isEqualTo("a1");
    }

    @Test
    void AC2_QUIZ를_분석_수준으로_1회_부르고_연결_개념과_사용량을_남긴다() throws IOException {
        long published = insertConcept("f14-test-공개", "PUBLISHED");
        long draft = insertConcept("f14-test-초안", "DRAFT");
        long userId = createUser("f14ac2", "JUNIOR");
        long id = prepare(userId, "BEGINNER");
        answer(
                LlmStage.CONCEPT,
                ("{\"concepts\": [{\"conceptId\": %d, \"contextNote\": \"컨트롤러\"},"
                                + " {\"conceptId\": %d}]}")
                        .formatted(draft, published));

        pipeline.run(id);

        List<LlmRequest> quiz = requests(LlmStage.QUIZ);
        assertThat(quiz).hasSize(1);
        LlmRequest request = quiz.get(0);
        assertThat(request.level()).isEqualTo(Level.BEGINNER);
        assertThat(request.userId()).isEqualTo(userId);
        assertThat(request.analysisId()).isEqualTo(id);
        long first = Math.min(published, draft);
        assertThat(request.prompt())
                .contains("입문자")
                .contains(published + " | f14-test-공개")
                .contains(draft + " | f14-test-초안 | 컨트롤러")
                .contains(
                        "<<<FILE no=1"
                            + " path=src/main/java/com/example/todo/controller/TodoController.java"
                            + " layer=controller>>>\n"
                            + "@RestController")
                .doesNotContain("1: @RestController");
        assertThat(request.prompt().indexOf(first + " | "))
                .isLessThan(request.prompt().indexOf(Math.max(published, draft) + " | "));
        assertThat(
                        jdbc.queryForList(
                                "SELECT user_id FROM llm_usage_logs WHERE analysis_id = ? AND stage"
                                        + " = 'quiz'",
                                Long.class,
                                id))
                .containsExactly(userId);
        // Mock fixture(quiz.json)의 6문항이 저장된다
        assertThat(quizCount(id)).isEqualTo(1);
    }

    @Test
    void AC2_연결_개념이_없어도_코드만으로_1회_부른다() throws IOException {
        long id = prepare(createUser("f14ac2z", "JUNIOR"), "JUNIOR");

        pipeline.run(id);

        List<LlmRequest> quiz = requests(LlmStage.QUIZ);
        assertThat(quiz).hasSize(1);
        assertThat(quiz.get(0).prompt()).contains("(없음)").contains("<<<FILE no=1 ");
        assertThat(analysisRow(id).get("status")).isEqualTo("DONE");
    }

    @Test
    void AC2_findForQuiz는_HIDDEN을_빼고_concept_id_순으로_돌려준다() {
        long userId = createUser("f14ac2f", "JUNIOR");
        long id = insertAnalysis(userId, "DONE", null, false, "p");
        long hidden = insertConcept("f14-test-숨김", "HIDDEN");
        long draft = insertConcept("f14-test-초안2", "DRAFT");
        long published = insertConcept("f14-test-공개2", "PUBLISHED");
        for (long c : List.of(published, hidden, draft)) {
            jdbc.update(
                    "INSERT INTO analysis_concepts (analysis_id, concept_id, context_note)"
                            + " VALUES (?, ?, ?)",
                    id,
                    c,
                    c == draft ? "메모" : null);
        }

        assertThat(analysisConceptService.findForQuiz(id))
                .containsExactly(
                        new LinkedConcept(draft, "f14-test-초안2", "DRAFT", "메모"),
                        new LinkedConcept(published, "f14-test-공개2", "PUBLISHED", null));
    }

    @Test
    void AC4_형식이_두_번_다_틀리면_퀴즈_없이_DONE() throws IOException {
        long id = prepare(createUser("f14ac4f", "JUNIOR"), "JUNIOR");
        answer(LlmStage.QUIZ, quizJson(4, "null"));

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("DONE");
        assertThat(row.get("error_code")).isNull();
        assertThat(quizCount(id)).isZero();
        assertThat(requests(LlmStage.QUIZ)).hasSize(2);
    }

    @Test
    void AC4_LLM_503이면_재시도_없이_퀴즈_없이_DONE() throws IOException {
        long id = prepare(createUser("f14ac4l", "JUNIOR"), "JUNIOR");
        doThrow(new BusinessException(ErrorCode.LLM_503))
                .when(llmClient)
                .complete(argThat(r -> r != null && r.stage() == LlmStage.QUIZ));

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("DONE");
        assertThat(row.get("error_code")).isNull();
        assertThat(quizCount(id)).isZero();
        assertThat(requests(LlmStage.QUIZ)).hasSize(1);
    }

    @Test
    void AC4_저장_중_예기치_못한_예외는_FAILED_ANALYSIS_500() throws IOException {
        long id = prepare(createUser("f14ac4d", "JUNIOR"), "JUNIOR");
        doThrow(new DataIntegrityViolationException("boom"))
                .when(quizSaveService)
                .save(anyLong(), anyList());

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("error_code")).isEqualTo("ANALYSIS_500");
        assertThat(row.get("progress")).isEqualTo(85);
    }
}
