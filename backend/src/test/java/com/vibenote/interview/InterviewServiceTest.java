package com.vibenote.interview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.vibenote.analysis.AnalysisPipeline;
import com.vibenote.analysis.AnalysisService;
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

/** I-1a AC1(접수)·AC5~AC7(상태 전이·횟수·동시 요청)·AC10(큐 거부)·AC11(분석 삭제). 생성기는 가짜로 둔다. */
class InterviewServiceTest extends InterviewTestSupport {

    @Autowired InterviewService interviewService;
    @Autowired InterviewSaveService saveService;
    @Autowired AnalysisService analysisService;

    @MockitoBean InterviewGenerator generator;

    /** 재실행(AC11)이 실제 저장소를 받으러 가지 않게 한다. */
    @MockitoBean AnalysisPipeline pipeline;

    private String code(Runnable call) {
        try {
            call.run();
            return "OK";
        } catch (BusinessException e) {
            return e.getErrorCode().name();
        }
    }

    @Test
    void AC1_처음_요청하면_PENDING_행을_만들고_커밋_뒤_생성기를_부른다() {
        long userId = createUser("svc1");
        long analysisId = analysis(userId, "DONE", "JUNIOR");

        InterviewResponse response = interviewService.request(userId, analysisId);

        assertThat(response.status()).isEqualTo(InterviewStatus.PENDING);
        assertThat(response.questions()).isNull();
        assertThat(response.generatedAt()).isNull();
        assertThat(response.regenerationsLeft()).isEqualTo(2);
        long interviewId = (Long) interviewRow(analysisId).get("id");
        verify(generator, timeout(2000)).generate(interviewId);
    }

    @Test
    void AC1_요청_시_생성은_흐름_실행기에서_돈다() {
        long userId = createUser("svc9");
        long analysisId = analysis(userId, "DONE", "JUNIOR");
        List<String> threads = new ArrayList<>();
        CountDownLatch ran = new CountDownLatch(1);
        org.mockito.Mockito.doAnswer(
                        inv -> {
                            threads.add(Thread.currentThread().getName());
                            ran.countDown();
                            return null;
                        })
                .when(generator)
                .generate(anyLong());

        interviewService.request(userId, analysisId);

        try {
            assertThat(ran.await(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        assertThat(threads.get(0)).startsWith("flow-");
    }

    @Test
    void AC5_PENDING이면_409이고_생성기를_부르지_않는다() {
        long userId = createUser("svc2");
        long analysisId = analysis(userId, "DONE", "JUNIOR");
        insertInterview(analysisId, "PENDING", 0, null);

        assertThatThrownBy(() -> interviewService.request(userId, analysisId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INTERVIEW_409);
        verify(generator, after(300).never()).generate(anyLong());
    }

    @Test
    void AC5_FAILED면_같은_행을_PENDING으로_되돌리고_횟수를_쓰지_않는다() {
        long userId = createUser("svc3");
        long analysisId = analysis(userId, "DONE", "JUNIOR");
        long interviewId = insertInterview(analysisId, "FAILED", 1, "LLM_503");

        InterviewResponse response = interviewService.request(userId, analysisId);

        assertThat(response.status()).isEqualTo(InterviewStatus.PENDING);
        assertThat(response.errorCode()).isNull();
        assertThat(response.regenerationsLeft()).isEqualTo(1);
        Map<String, Object> row = interviewRow(analysisId);
        assertThat(row.get("id")).isEqualTo(interviewId);
        assertThat(row.get("error_code")).isNull();
        assertThat(row.get("regenerations")).isEqualTo(1);
        verify(generator, timeout(2000)).generate(interviewId);
    }

    @Test
    void AC6_DONE이면_PENDING으로_바꾸고_횟수를_1_늘리며_2회를_다_쓰면_429이고_바꾸지_않는다() {
        long userId = createUser("svc4");
        long analysisId = analysis(userId, "DONE", "JUNIOR");
        long interviewId = insertInterview(analysisId, "DONE", 1, null);

        InterviewResponse response = interviewService.request(userId, analysisId);

        assertThat(response.status()).isEqualTo(InterviewStatus.PENDING);
        assertThat(response.regenerationsLeft()).isEqualTo(0);
        assertThat(interviewRow(analysisId).get("regenerations")).isEqualTo(2);
        verify(generator, timeout(2000)).generate(interviewId);

        jdbc.update("UPDATE analysis_interviews SET status = 'DONE' WHERE id = ?", interviewId);
        assertThatThrownBy(() -> interviewService.request(userId, analysisId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("이 분석의 면접 질문 다시 만들기 횟수(2회)를 모두 썼습니다.")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INTERVIEW_429);
        Map<String, Object> row = interviewRow(analysisId);
        assertThat(row.get("status")).isEqualTo("DONE");
        assertThat(row.get("regenerations")).isEqualTo(2);
    }

    @Test
    void AC7_질문_없는_분석에_동시_요청_2개면_하나만_접수되고_생성은_1회다() throws Exception {
        long userId = createUser("svc5");
        long analysisId = analysis(userId, "DONE", "JUNIOR");

        assertThat(concurrent(userId, analysisId)).containsExactlyInAnyOrder("OK", "INTERVIEW_409");
        assertThat(interviewCount(analysisId)).isEqualTo(1);
        verify(generator, after(500).times(1)).generate(anyLong());
    }

    @Test
    void AC7_DONE에_동시_요청_2개면_횟수는_1만_늘어난다() throws Exception {
        long userId = createUser("svc6");
        long analysisId = analysis(userId, "DONE", "JUNIOR");
        insertInterview(analysisId, "DONE", 0, null);

        assertThat(concurrent(userId, analysisId)).containsExactlyInAnyOrder("OK", "INTERVIEW_409");
        assertThat(interviewRow(analysisId).get("regenerations")).isEqualTo(1);
        verify(generator, after(500).times(1)).generate(anyLong());
    }

    private List<String> concurrent(long userId, long analysisId) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        Callable<String> call =
                () -> {
                    start.await();
                    return code(() -> interviewService.request(userId, analysisId));
                };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<String>> results = List.of(pool.submit(call), pool.submit(call));
            start.countDown();
            List<String> outcomes = new ArrayList<>();
            for (Future<String> f : results) {
                outcomes.add(f.get());
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void AC8_DONE이_아니면_409_남의_것_삭제된_것은_404다() {
        long userId = createUser("svc7");
        long other = createUser("svc7o");
        long running = analysis(userId, "EXPLAINING", "JUNIOR");
        long done = analysis(userId, "DONE", "JUNIOR");
        long deleted = analysis(userId, "DONE", "JUNIOR");
        jdbc.update("UPDATE analyses SET deleted_at = NOW() WHERE id = ?", deleted);

        assertThat(code(() -> interviewService.request(userId, running)))
                .isEqualTo("INTERVIEW_409");
        assertThat(code(() -> interviewService.request(other, done))).isEqualTo("ANALYSIS_404");
        assertThat(code(() -> interviewService.request(userId, deleted))).isEqualTo("ANALYSIS_404");
        assertThat(code(() -> interviewService.request(userId, 999_999_999L)))
                .isEqualTo("ANALYSIS_404");
        assertThat(interviewCount(running)).isZero();
    }

    @Test
    void AC10_큐_거부면_그_행만_FAILED_ANALYSIS_500이고_전용_문구를_준다() {
        long userId = createUser("svc8");
        long analysisId = analysis(userId, "DONE", "JUNIOR");
        long interviewId = insertInterview(analysisId, "PENDING", 0, null);
        InterviewLauncher launcher =
                new InterviewLauncher(
                        generator,
                        saveService,
                        task -> {
                            throw new TaskRejectedException("가득 참");
                        });

        launcher.launch(new InterviewRequestedEvent(interviewId));

        Map<String, Object> row = interviewRow(analysisId);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("error_code")).isEqualTo("ANALYSIS_500");
        verify(generator, never()).generate(anyLong());
        InterviewResponse response = interviewService.get(userId, analysisId);
        assertThat(response.errorCode()).isEqualTo("ANALYSIS_500");
        assertThat(response.errorMessage()).isEqualTo("면접 질문 생성이 중단되었습니다. 다시 요청해 주세요.");
    }

    @Test
    void AC11_분석_삭제면_면접_질문도_지워진다() {
        long userId = createUser("svc10");
        long analysisId = analysis(userId, "DONE", "JUNIOR");
        long interviewId = insertInterview(analysisId, "DONE", 0, null);
        insertQuestion(interviewId, 1, fileIds(analysisId).get(0), "[]");

        analysisService.delete(userId, analysisId);

        assertThat(interviewCount(analysisId)).isZero();
        assertThat(questionRows(interviewId)).isEmpty();
    }

    @Test
    void AC11_실패_분석_재실행으로_원본이_지워지면_면접_질문도_지워진다() {
        long userId = createUser("svc11");
        long analysisId = analysis(userId, "FAILED", "JUNIOR");
        jdbc.update(
                "UPDATE analyses SET source_type = 'URL', source_url ="
                        + " 'https://github.com/vibe/todo' WHERE id = ?",
                analysisId);
        insertInterview(analysisId, "FAILED", 0, "LLM_503");

        String outcome = code(() -> analysisService.rerun(userId, analysisId));

        assertThat(outcome).isEqualTo("OK");
        assertThat(interviewCount(analysisId)).isZero();
    }
}
