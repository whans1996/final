package com.vibenote.interview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.vibenote.common.Level;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.llm.LlmClient;
import com.vibenote.llm.LlmRequest;
import com.vibenote.llm.LlmResponse;
import com.vibenote.llm.LlmStage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.JsonNode;

/** I-1a AC1·AC4: PENDING 면접 하나를 생성기로 직접 만든다 (비동기 실행은 InterviewServiceTest). */
class InterviewGeneratorTest extends InterviewTestSupport {

    @Autowired InterviewGenerator generator;

    @MockitoSpyBean LlmClient llmClient;

    private List<LlmRequest> requests() {
        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmClient, atLeast(0)).complete(captor.capture());
        return captor.getAllValues();
    }

    private static LlmResponse response(String content) {
        return new LlmResponse(content, "fake", 10, 20);
    }

    /** 근거가 파일 1의 L1-L2인 정상 질문 n개. */
    private String questions(int n) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            list.add(
                    Map.of(
                            "category", "FLOW",
                            "question", "질문 " + i,
                            "keyPoints", List.of("포인트 " + i),
                            "evidence", List.of(Map.of("fileNo", 1, "lines", "L1-L2")),
                            "conceptIds", List.of()));
        }
        return objectMapper.writeValueAsString(Map.of("questions", list));
    }

    @Test
    void AC1_PENDING_면접을_분석_수준_프롬프트로_1회_불러_DONE과_질문을_저장한다() {
        long userId = createUser("gen1");
        long analysisId = analysis(userId, "DONE", "JUNIOR");
        long hidden = concept("숨김", "HIDDEN");
        long shown = concept("계층형", "PUBLISHED");
        link(analysisId, hidden, "숨긴 카드");
        link(analysisId, shown, "Controller가 Service를 부릅니다.");
        long interviewId = insertInterview(analysisId, "PENDING", 0, null);

        generator.generate(interviewId);

        List<LlmRequest> requests = requests();
        assertThat(requests).hasSize(1);
        LlmRequest request = requests.get(0);
        assertThat(request.stage()).isEqualTo(LlmStage.INTERVIEW);
        assertThat(request.level()).isEqualTo(Level.JUNIOR);
        assertThat(request.userId()).isEqualTo(userId);
        assertThat(request.analysisId()).isEqualTo(analysisId);
        assertThat(request.prompt())
                .contains("주니어 개발자")
                .contains("todo-app")
                .contains("요약: 할 일 앱")
                .contains("- POST /api/todos")
                .contains("1. src/main/java/todo/TodoController.java [controller]")
                .contains("2. src/main/java/todo/TodoService.java [service]")
                .doesNotContain("Unselected.java")
                .contains(shown + " | I1A-계층형 | Controller가 Service를 부릅니다.")
                .doesNotContain("I1A-숨김")
                .contains("<<<FILE no=1 path=src/main/java/todo/TodoController.java")
                .contains("1: @RestController");

        Map<String, Object> row = interviewRow(analysisId);
        assertThat(row.get("status")).isEqualTo("DONE");
        assertThat(row.get("error_code")).isNull();
        assertThat(row.get("generated_at")).isNotNull();
        assertThat(questionRows(interviewId)).hasSize(8);
    }

    @Test
    void AC1_사용량에_stage_interview와_user_analysis가_기록된다() {
        long userId = createUser("gen2");
        long analysisId = analysis(userId, "DONE", "BEGINNER");
        long interviewId = insertInterview(analysisId, "PENDING", 0, null);

        generator.generate(interviewId);

        List<Map<String, Object>> logs =
                jdbc.queryForList(
                        "SELECT stage, user_id FROM llm_usage_logs WHERE analysis_id = ?",
                        analysisId);
        assertThat(logs).hasSize(1);
        assertThat((String) logs.get(0).get("stage")).isEqualTo("interview");
        assertThat(((Number) logs.get(0).get("user_id")).longValue()).isEqualTo(userId);
        assertThat(requests().get(0).prompt()).contains("입문자");
    }

    @Test
    void AC1_main_fixture로_질문_8개가_범주마다_2개씩_DONE이_된다() throws IOException {
        // 테스트 리소스 llm-fixtures가 main을 가릴 수 있으므로 main 파일을 경로로 읽는다
        String fixture =
                Files.readString(
                        Path.of("src/main/resources/llm-fixtures/interview.json"),
                        StandardCharsets.UTF_8);
        doReturn(response(fixture)).when(llmClient).complete(any());
        long userId = createUser("gen3");
        long analysisId = analysis(userId, "DONE", "SENIOR");
        long interviewId = insertInterview(analysisId, "PENDING", 0, null);

        generator.generate(interviewId);

        assertThat(interviewRow(analysisId).get("status")).isEqualTo("DONE");
        List<Map<String, Object>> rows = questionRows(interviewId);
        assertThat(rows).hasSize(8);
        assertThat(rows)
                .extracting(r -> r.get("category"))
                .containsExactly(
                        "STRUCTURE",
                        "STRUCTURE",
                        "TECH_CHOICE",
                        "TECH_CHOICE",
                        "FLOW",
                        "FLOW",
                        "LIMITATION",
                        "LIMITATION");
        long controller = fileIds(analysisId).get(0);
        JsonNode evidence = objectMapper.readTree((String) rows.get(0).get("evidence"));
        assertThat(evidence.get(0).get("fileId").asLong()).isEqualTo(controller);
        assertThat(evidence.get(0).get("path").asString())
                .isEqualTo("src/main/java/todo/TodoController.java");
        assertThat(evidence.get(0).get("startLine").asInt()).isEqualTo(1);
        assertThat(evidence.get(0).get("endLine").asInt()).isEqualTo(3);
    }

    @Test
    void AC4_JSON이_아니면_1회_다시_묻고_두_번_다_틀리면_FAILED_LLM_502이며_질문이_없다() {
        doReturn(response("not json")).when(llmClient).complete(any());
        long userId = createUser("gen4");
        long analysisId = analysis(userId, "DONE", "JUNIOR");
        long interviewId = insertInterview(analysisId, "PENDING", 0, null);

        generator.generate(interviewId);

        assertThat(requests()).hasSize(2);
        Map<String, Object> row = interviewRow(analysisId);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("error_code")).isEqualTo("LLM_502");
        assertThat(questionRows(interviewId)).isEmpty();
    }

    @Test
    void AC4_유효_질문이_5개_미만이면_다시_묻고_두번째가_맞으면_DONE이다() {
        doReturn(response(questions(4)), response(questions(6))).when(llmClient).complete(any());
        long userId = createUser("gen5");
        long analysisId = analysis(userId, "DONE", "JUNIOR");
        long interviewId = insertInterview(analysisId, "PENDING", 0, null);

        generator.generate(interviewId);

        assertThat(requests()).hasSize(2);
        assertThat(interviewRow(analysisId).get("status")).isEqualTo("DONE");
        assertThat(questionRows(interviewId)).hasSize(6);
    }

    @Test
    void AC4_유효_질문이_두_번_다_5개_미만이면_FAILED_LLM_502() {
        doReturn(response(questions(4))).when(llmClient).complete(any());
        long userId = createUser("gen6");
        long analysisId = analysis(userId, "DONE", "JUNIOR");
        long interviewId = insertInterview(analysisId, "PENDING", 0, null);

        generator.generate(interviewId);

        assertThat(requests()).hasSize(2);
        assertThat(interviewRow(analysisId).get("error_code")).isEqualTo("LLM_502");
    }

    @Test
    void AC4_LLM_503은_다시_묻지_않고_FAILED_LLM_503() {
        doThrow(new BusinessException(ErrorCode.LLM_503)).when(llmClient).complete(any());
        long userId = createUser("gen7");
        long analysisId = analysis(userId, "DONE", "JUNIOR");
        long interviewId = insertInterview(analysisId, "PENDING", 0, null);

        generator.generate(interviewId);

        assertThat(requests()).hasSize(1);
        Map<String, Object> row = interviewRow(analysisId);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("error_code")).isEqualTo("LLM_503");
    }

    @Test
    void AC1_PENDING이_아니면_LLM을_부르지_않는다() {
        long userId = createUser("gen8");
        long analysisId = analysis(userId, "DONE", "JUNIOR");
        long interviewId = insertInterview(analysisId, "FAILED", 0, "LLM_503");

        generator.generate(interviewId);

        assertThat(requests()).isEmpty();
        assertThat(interviewRow(analysisId).get("status")).isEqualTo("FAILED");
    }
}
