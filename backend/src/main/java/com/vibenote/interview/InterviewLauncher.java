package com.vibenote.interview;

import com.vibenote.analysis.flow.FlowExecutorConfig;
import com.vibenote.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 요청 트랜잭션이 커밋된 뒤 면접 질문 생성을 요청 시 생성 실행기({@code flowExecutor}, F-07과 같이 씀)에 넘긴다. 큐가 차서 거절되면 그 면접만
 * FAILED·ANALYSIS_500 (AC10).
 */
@Component
class InterviewLauncher {

    private static final Logger log = LoggerFactory.getLogger(InterviewLauncher.class);

    private final InterviewGenerator generator;
    private final InterviewSaveService saveService;
    private final TaskExecutor executor;

    InterviewLauncher(
            InterviewGenerator generator,
            InterviewSaveService saveService,
            @Qualifier(FlowExecutorConfig.EXECUTOR) TaskExecutor executor) {
        this.generator = generator;
        this.saveService = saveService;
        this.executor = executor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void launch(InterviewRequestedEvent event) {
        long id = event.interviewId();
        try {
            executor.execute(() -> generator.generate(id));
        } catch (TaskRejectedException e) {
            log.warn("요청 시 생성 실행기가 가득 차 면접 {}을 시작하지 못함", id);
            saveService.markFailed(id, ErrorCode.ANALYSIS_500);
        }
    }
}
