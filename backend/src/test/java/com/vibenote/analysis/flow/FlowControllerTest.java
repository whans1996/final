package com.vibenote.analysis.flow;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

/** F-07 AC8~AC10 (+ AC1 202 응답): POST·GET /api/analyses/{id}/flows. 생성기는 가짜로 둔다. */
class FlowControllerTest extends FlowTestSupport {

    @MockitoBean FlowGenerator generator;

    private static final List<String> APIS = List.of("POST /api/todos", "ANY /api/todos/x");

    private ResultActions postFlows(long userId, long analysisId, String body) throws Exception {
        return mockMvc.perform(
                post("/api/analyses/{id}/flows", analysisId)
                        .header("Authorization", bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    private static String body(String... apis) {
        StringBuilder items = new StringBuilder();
        for (String api : apis) {
            String[] parts = api.split(" ", 2);
            items.append(items.isEmpty() ? "" : ",")
                    .append("{\"method\":\"%s\",\"path\":\"%s\"}".formatted(parts[0], parts[1]));
        }
        return "{\"endpoints\":[" + items + "]}";
    }

    @Test
    void AC1_DONE_분석에_흐름을_요청하면_202와_PENDING을_받는다() throws Exception {
        long userId = createUser("ctl1");
        long analysisId = analysis(userId, "DONE", "JUNIOR", APIS);

        postFlows(userId, analysisId, body("POST /api/todos"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].method").value("POST"))
                .andExpect(jsonPath("$[0].path").value("/api/todos"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].errorCode").isEmpty())
                .andExpect(jsonPath("$[0].summary").isEmpty())
                .andExpect(jsonPath("$[0].steps").isEmpty())
                .andExpect(
                        jsonPath("$[0].createdAt").value(org.hamcrest.Matchers.endsWith("+09:00")));
    }

    @Test
    void AC8_endpoints_개수_중복_빈_값이면_COMMON_400() throws Exception {
        long userId = createUser("ctl2");
        long analysisId = analysis(userId, "DONE", "JUNIOR", APIS);

        for (String body :
                List.of(
                        "{\"endpoints\":[]}",
                        "{}",
                        body("POST /a", "POST /b", "POST /c", "POST /d"),
                        "{\"endpoints\":[{\"method\":\"\",\"path\":\"/api/todos\"}]}",
                        "{\"endpoints\":[{\"method\":\"POST\"}]}")) {
            postFlows(userId, analysisId, body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_400"))
                    .andExpect(jsonPath("$.details").isMap());
        }
        postFlows(userId, analysisId, body("POST /api/todos", "POST /api/todos"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.endpoints").value(FlowService.DUPLICATE_ENDPOINT));
    }

    @Test
    void AC8_개요_apis에_없는_API는_ANALYSIS_002() throws Exception {
        long userId = createUser("ctl3");
        long analysisId = analysis(userId, "DONE", "JUNIOR", APIS);

        for (String api : List.of("GET /api/todos", "post /api/todos", "GET /api/todos/x")) {
            postFlows(userId, analysisId, body(api))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ANALYSIS_002"));
        }
        postFlows(userId, analysisId, body("ANY /api/todos/x")).andExpect(status().isAccepted());
    }

    @Test
    void AC9_DONE이_아니면_409_남의_것_없는_것_삭제된_것은_404_토큰_없으면_401() throws Exception {
        long userId = createUser("ctl4");
        long other = createUser("ctl4o");
        long running = analysis(userId, "EXPLAINING", "JUNIOR", APIS);
        long failed = analysis(userId, "FAILED", "JUNIOR", APIS);
        long others = analysis(other, "DONE", "JUNIOR", APIS);
        long deleted = analysis(userId, "DONE", "JUNIOR", APIS);
        jdbc.update("UPDATE analyses SET deleted_at = NOW() WHERE id = ?", deleted);

        for (long id : List.of(running, failed)) {
            postFlows(userId, id, body("POST /api/todos"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("ANALYSIS_409"));
        }
        for (long id : List.of(others, deleted, 999_999_999L)) {
            postFlows(userId, id, body("POST /api/todos"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
        }
        mockMvc.perform(
                        post("/api/analyses/{id}/flows", running)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("POST /api/todos")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    @Test
    void AC10_흐름_목록은_id순이고_DONE이_아니면_summary_steps가_null() throws Exception {
        long userId = createUser("ctl5");
        long analysisId = analysis(userId, "DONE", "JUNIOR", APIS);
        long done = insertFlow(analysisId, "POST /api/todos", "DONE", null);
        long failed = insertFlow(analysisId, "ANY /api/todos/x", "FAILED", "LLM_503");

        mockMvc.perform(
                        get("/api/analyses/{id}/flows", analysisId)
                                .header("Authorization", bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(done))
                .andExpect(jsonPath("$[0].status").value("DONE"))
                .andExpect(jsonPath("$[0].summary").value("요약"))
                .andExpect(jsonPath("$[0].steps[0].no").value(1))
                .andExpect(jsonPath("$[0].steps[0].title").value("t"))
                .andExpect(jsonPath("$[1].id").value(failed))
                .andExpect(jsonPath("$[1].method").value("ANY"))
                .andExpect(jsonPath("$[1].path").value("/api/todos/x"))
                .andExpect(jsonPath("$[1].status").value("FAILED"))
                .andExpect(jsonPath("$[1].errorCode").value("LLM_503"))
                .andExpect(jsonPath("$[1].errorMessage").value("AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요."))
                .andExpect(jsonPath("$[1].summary").isEmpty())
                .andExpect(jsonPath("$[1].steps").isEmpty());
    }

    @Test
    void AC10_흐름이_없거나_진행_중이면_빈_배열_권한은_404_401() throws Exception {
        long userId = createUser("ctl6");
        long other = createUser("ctl6o");
        long running = analysis(userId, "SELECTING", "JUNIOR", APIS);
        long others = analysis(other, "DONE", "JUNIOR", APIS);

        mockMvc.perform(
                        get("/api/analyses/{id}/flows", running)
                                .header("Authorization", bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(
                        get("/api/analyses/{id}/flows", others)
                                .header("Authorization", bearer(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
        mockMvc.perform(get("/api/analyses/{id}/flows", running))
                .andExpect(status().isUnauthorized());
    }
}
