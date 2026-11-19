package com.vibenote.analysis.flow;

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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.JsonNode;

/** F-07 AC1·AC4·AC5: PENDING 흐름 하나를 생성기로 직접 만든다 (비동기 실행은 FlowServiceTest·컨트롤러 테스트). */
class FlowGeneratorTest extends FlowTestSupport {

    @Autowired FlowGenerator generator;

    @MockitoSpyBean LlmClient llmClient;

    private List<LlmRequest> requests() {
        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmClient, atLeast(0)).complete(captor.capture());
        return captor.getAllValues();
    }

    private static LlmResponse response(String content) {
        return new LlmResponse(content, "fake", 10, 20);
    }

    @Test
    void AC1_PENDING_흐름을_수준별_프롬프트로_1회_불러_DONE으로_저장한다() {
        long userId = createUser("gen1");
        long analysisId = analysis(userId, "DONE", "JUNIOR", List.of("POST /api/todos"));
        long flowId = insertFlow(analysisId, "POST /api/todos", "PENDING", null);

        generator.generate(flowId);

        List<LlmRequest> requests = requests();
        assertThat(requests).hasSize(1);
        LlmRequest request = requests.get(0);
        assertThat(request.stage()).isEqualTo(LlmStage.FLOW);
        assertThat(request.level()).isEqualTo(Level.JUNIOR);
        assertThat(request.userId()).isEqualTo(userId);
        assertThat(request.analysisId()).isEqualTo(analysisId);
        assertThat(request.prompt())
                .contains("주니어 개발자")
                .contains("POST /api/todos")
                .contains("할 일 앱")
                .contains("path=src/main/java/todo/TodoController.java layer=controller")
                .contains("path=src/main/java/todo/TodoService.java layer=service")
                .contains("path=src/main/java/todo/TodoMapper.java layer=mapper");

        Map<String, Object> row = flowRow(flowId);
        assertThat(row.get("status")).isEqualTo("DONE");
        assertThat((String) row.get("summary")).isNotBlank();
        JsonNode steps = objectMapper.readTree((String) row.get("steps"));
        assertThat(steps.size()).isEqualTo(6);
        assertThat(steps.get(0).get("no").asInt()).isEqualTo(1);
        // mock fixture는 dev 데이터(realworld) 경로라 이 분석의 파일이 아니다 (AC4)
        assertThat(steps.get(0).get("fileId").isNull()).isTrue();
        assertThat(row.get("error_code")).isNull();
        assertThat(row.get("updated_at")).isNotNull();
    }

    @Test
    void AC1_사용량에_user와_analysis가_기록된다() {
        long userId = createUser("gen2");
        long analysisId = analysis(userId, "DONE", "BEGINNER", List.of("POST /api/todos"));
        long flowId = insertFlow(analysisId, "POST /api/todos", "PENDING", null);

        generator.generate(flowId);

        List<Map<String, Object>> logs =
                jdbc.queryForList(
                        "SELECT stage, user_id FROM llm_usage_logs WHERE analysis_id = ?",
                        analysisId);
        assertThat(logs).hasSize(1);
        assertThat(((String) logs.get(0).get("stage"))).isEqualToIgnoringCase("FLOW");
        assertThat(((Number) logs.get(0).get("user_id")).longValue()).isEqualTo(userId);
        assertThat(requests().get(0).prompt()).contains("입문자");
    }

    @Test
    void AC4_분석_파일_경로인_단계에는_fileId와_layer를_저장한다() {
        long userId = createUser("gen3");
        long analysisId = analysis(userId, "DONE", "SENIOR", List.of("POST /api/todos"));
        long flowId = insertFlow(analysisId, "POST /api/todos", "PENDING", null);
        long controllerId =
                jdbc.queryForObject(
                        "SELECT MIN(id) FROM analysis_files WHERE analysis_id = ?",
                        Long.class,
                        analysisId);
        doReturn(
                        response(
                                """
                                {"summary":"요약","steps":[
                                {"path":"src/main/java/todo/TodoController.java","symbol":"TodoController.create","title":"받기","description":"설명"},
                                {"path":"없는/파일.java","title":"모름","description":"설명"}]}\
                                """))
                .when(llmClient)
                .complete(any());

        generator.generate(flowId);

        JsonNode steps = objectMapper.readTree((String) flowRow(flowId).get("steps"));
        assertThat(steps.get(0).get("fileId").asLong()).isEqualTo(controllerId);
        assertThat(steps.get(0).get("layer").asString()).isEqualTo("controller");
        assertThat(steps.get(1).get("fileId").isNull()).isTrue();
        assertThat(steps.get(1).get("symbol").isNull()).isTrue();
        assertThat(requests().get(0).prompt()).contains("시니어 개발자");
    }

    @Test
    void AC5_형식이_두_번_틀리면_FAILED_LLM_502이고_다른_흐름은_그대로() {
        long userId = createUser("gen4");
        long analysisId =
                analysis(
                        userId,
                        "DONE",
                        "JUNIOR",
                        List.of("POST /api/todos", "PATCH /api/todos/{id}"));
        long done = insertFlow(analysisId, "PATCH /api/todos/{id}", "DONE", null);
        long flowId = insertFlow(analysisId, "POST /api/todos", "PENDING", null);
        doReturn(response("not json"))
                .doReturn(response("{\"summary\":\"s\",\"steps\":[]}"))
                .when(llmClient)
                .complete(any());

        generator.generate(flowId);

        assertThat(requests()).hasSize(2);
        Map<String, Object> row = flowRow(flowId);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("error_code")).isEqualTo("LLM_502");
        assertThat(row.get("steps")).isNull();
        assertThat(flowRow(done).get("status")).isEqualTo("DONE");
    }

    @Test
    void AC5_형식이_한_번_틀리고_다시_맞으면_DONE() {
        long userId = createUser("gen5");
        long analysisId = analysis(userId, "DONE", "JUNIOR", List.of("POST /api/todos"));
        long flowId = insertFlow(analysisId, "POST /api/todos", "PENDING", null);
        doReturn(response("```json"))
                .doReturn(
                        response(
                                "{\"summary\":\"s\",\"steps\":[{\"path\":\"p\",\"title\":\"t\",\"description\":\"d\"}]}"))
                .when(llmClient)
                .complete(any());

        generator.generate(flowId);

        assertThat(requests()).hasSize(2);
        assertThat(flowRow(flowId).get("status")).isEqualTo("DONE");
    }

    @Test
    void AC5_LLM_503이면_재시도_없이_FAILED_LLM_503() {
        long userId = createUser("gen6");
        long analysisId = analysis(userId, "DONE", "JUNIOR", List.of("POST /api/todos"));
        long flowId = insertFlow(analysisId, "POST /api/todos", "PENDING", null);
        doThrow(new BusinessException(ErrorCode.LLM_503)).when(llmClient).complete(any());

        generator.generate(flowId);

        assertThat(requests()).hasSize(1);
        Map<String, Object> row = flowRow(flowId);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("error_code")).isEqualTo("LLM_503");
    }

    @Test
    void PENDING이_아니면_LLM을_부르지_않는다() {
        long userId = createUser("gen7");
        long analysisId = analysis(userId, "DONE", "JUNIOR", List.of("POST /api/todos"));
        long flowId = insertFlow(analysisId, "POST /api/todos", "FAILED", "LLM_503");

        generator.generate(flowId);

        assertThat(requests()).isEmpty();
        assertThat(flowRow(flowId).get("status")).isEqualTo("FAILED");
    }
}
