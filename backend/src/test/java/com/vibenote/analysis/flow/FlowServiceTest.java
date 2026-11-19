package com.vibenote.analysis.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** F-07 AC1(접수·실행기 제출)·AC6(캐시·재생성)·AC7(개수 상한·동시 요청)·AC11(큐 거부). 생성기는 가짜로 둔다. */
class FlowServiceTest extends FlowTestSupport {

    static final List<String> APIS =
            List.of(
                    "GET /api/todos",
                    "POST /api/todos",
                    "PATCH /api/todos/{id}",
                    "DELETE /api/todos/{id}",
                    "ANY /api/todos/x");

    @Autowired FlowService flowService;

    @MockitoBean FlowGenerator generator;

    static List<CreateFlowsRequest.Endpoint> endpoints(String... apis) {
        List<CreateFlowsRequest.Endpoint> result = new ArrayList<>();
        for (String api : apis) {
            String[] parts = api.split(" ", 2);
            result.add(new CreateFlowsRequest.Endpoint(parts[0], parts[1]));
        }
        return result;
    }

    @Test
    void AC1_새_API는_PENDING으로_만들고_커밋_뒤_실행기에서_생성한다() {
        long userId = createUser("svc1");
        long analysisId = analysis(userId, "DONE", "JUNIOR", APIS);

        List<FlowResponse> flows =
                flowService.request(userId, analysisId, endpoints("POST /api/todos"));

        assertThat(flows).hasSize(1);
        FlowResponse flow = flows.get(0);
        assertThat(flow.method()).isEqualTo("POST");
        assertThat(flow.path()).isEqualTo("/api/todos");
        assertThat(flow.status()).isEqualTo(FlowStatus.PENDING);
        assertThat(flow.summary()).isNull();
        assertThat(flow.steps()).isNull();
        assertThat(flowRow(flow.id()).get("endpoint")).isEqualTo("POST /api/todos");
        verify(generator, timeout(2000)).generate(flow.id());
    }

    @Test
    void AC6_PENDING_DONE이면_다시_만들지_않고_FAILED면_같은_행을_되돌린다() {
        long userId = createUser("svc2");
        long analysisId = analysis(userId, "DONE", "JUNIOR", APIS);
        long pending = insertFlow(analysisId, "GET /api/todos", "PENDING", null);
        long done = insertFlow(analysisId, "POST /api/todos", "DONE", null);
        long failed = insertFlow(analysisId, "PATCH /api/todos/{id}", "FAILED", "LLM_503");

        List<FlowResponse> flows =
                flowService.request(
                        userId,
                        analysisId,
                        endpoints("GET /api/todos", "POST /api/todos", "PATCH /api/todos/{id}"));

        assertThat(flows).extracting(FlowResponse::id).containsExactly(pending, done, failed);
        assertThat(flows)
                .extracting(FlowResponse::status)
                .containsExactly(FlowStatus.PENDING, FlowStatus.DONE, FlowStatus.PENDING);
        assertThat(flows.get(1).summary()).isEqualTo("요약");
        assertThat(flows.get(1).steps()).hasSize(1);
        Map<String, Object> reset = flowRow(failed);
        assertThat(reset.get("status")).isEqualTo("PENDING");
        assertThat(reset.get("error_code")).isNull();
        assertThat(flowRows(analysisId)).hasSize(3);
        verify(generator, timeout(2000)).generate(failed);
        verify(generator, never()).generate(pending);
        verify(generator, never()).generate(done);
    }

    @Test
    void AC7_FAILED가_아닌_흐름이_3개를_넘으면_ANALYSIS_003이고_아무것도_바꾸지_않는다() {
        long userId = createUser("svc3");
        long analysisId = analysis(userId, "DONE", "JUNIOR", APIS);
        insertFlow(analysisId, "GET /api/todos", "DONE", null);
        insertFlow(analysisId, "POST /api/todos", "PENDING", null);
        long failed = insertFlow(analysisId, "ANY /api/todos/x", "FAILED", "LLM_502");

        assertThatThrownBy(
                        () ->
                                flowService.request(
                                        userId,
                                        analysisId,
                                        endpoints(
                                                "PATCH /api/todos/{id}", "DELETE /api/todos/{id}")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ANALYSIS_003);
        assertThat(flowRows(analysisId)).hasSize(3);
        assertThat(flowRow(failed).get("status")).isEqualTo("FAILED");

        // FAILED는 세지 않으므로 하나는 더 만들 수 있다
        assertThat(flowService.request(userId, analysisId, endpoints("PATCH /api/todos/{id}")))
                .hasSize(4);
    }

    @Test
    void AC7_동시_요청도_합계_3개를_넘지_않는다() throws Exception {
        long userId = createUser("svc4");
        long analysisId = analysis(userId, "DONE", "JUNIOR", APIS);
        insertFlow(analysisId, "GET /api/todos", "DONE", null);

        CountDownLatch start = new CountDownLatch(1);
        List<Callable<String>> calls =
                List.of(
                        () -> {
                            start.await();
                            return call(
                                    userId, analysisId, "POST /api/todos", "PATCH /api/todos/{id}");
                        },
                        () -> {
                            start.await();
                            return call(
                                    userId,
                                    analysisId,
                                    "DELETE /api/todos/{id}",
                                    "ANY /api/todos/x");
                        });
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<String>> results = new ArrayList<>();
            for (Callable<String> c : calls) {
                results.add(pool.submit(c));
            }
            start.countDown();
            List<String> outcomes = new ArrayList<>();
            for (Future<String> f : results) {
                outcomes.add(f.get());
            }
            assertThat(outcomes).containsExactlyInAnyOrder("OK", "ANALYSIS_003");
        } finally {
            pool.shutdownNow();
        }
        assertThat(flowRows(analysisId)).hasSize(3);
    }

    private String call(long userId, long analysisId, String... apis) {
        try {
            flowService.request(userId, analysisId, endpoints(apis));
            return "OK";
        } catch (BusinessException e) {
            return e.getErrorCode().name();
        }
    }

    @Test
    void AC11_큐_거부면_FAILED() {
        long userId = createUser("svc5");
        long analysisId = analysis(userId, "DONE", "JUNIOR", APIS);
        long flowId = insertFlow(analysisId, "POST /api/todos", "PENDING", null);
        FlowLauncher launcher =
                new FlowLauncher(
                        generator,
                        flowService,
                        task -> {
                            throw new TaskRejectedException("가득 참");
                        });

        launcher.launch(new FlowRequestedEvent(List.of(flowId)));

        Map<String, Object> row = flowRow(flowId);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("error_code")).isEqualTo("ANALYSIS_500");
        verify(generator, never()).generate(anyLong());
        assertThat(flowService.list(userId, analysisId).get(0).errorMessage())
                .isEqualTo("실행 흐름 생성이 중단되었습니다. 다시 요청해 주세요.");
    }
}
