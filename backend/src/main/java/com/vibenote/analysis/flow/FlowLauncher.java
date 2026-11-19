package com.vibenote.analysis.flow;

import com.vibenote.analysis.AnalysisExecutorConfig;
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
 * 요청 트랜잭션이 커밋된 뒤 흐름 생성을 분석 실행기에 흐름별로 넘긴다 (F-05a AnalysisPipelineLauncher와 같은 패턴). 큐가 차서 거절되면 그 흐름만
 * FAILED·ANALYSIS_500 (AC11).
 */
@Component
class FlowLauncher {

    private static final Logger log = LoggerFactory.getLogger(FlowLauncher.class);

    private final FlowGenerator generator;
    private final FlowService flowService;
    private final TaskExecutor executor;

    FlowLauncher(
            FlowGenerator generator,
            FlowService flowService,
            @Qualifier(AnalysisExecutorConfig.EXECUTOR) TaskExecutor executor) {
        this.generator = generator;
        this.flowService = flowService;
        this.executor = executor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void launch(FlowRequestedEvent event) {
        for (long id : event.flowIds()) {
            try {
                executor.execute(() -> generator.generate(id));
            } catch (TaskRejectedException e) {
                log.warn("분석 실행기가 가득 차 흐름 {}을 시작하지 못함", id);
                flowService.markFailed(id, ErrorCode.ANALYSIS_500);
            }
        }
    }
}
