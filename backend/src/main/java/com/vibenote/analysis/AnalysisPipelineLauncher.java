package com.vibenote.analysis;

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
 * 요청 트랜잭션이 커밋된 뒤에 파이프라인을 분석 전용 실행기에 넘긴다. 커밋 전에 시작하면 다른 스레드가 아직 없는 행을 읽는다 (F-05a 5절).
 *
 * <p>{@code @Async} 대신 실행기를 직접 부르는 이유: 큐가 차서 거절될 때 그 분석을 FAILED로 표시해야 한다.
 */
@Component
class AnalysisPipelineLauncher {

    private static final Logger log = LoggerFactory.getLogger(AnalysisPipelineLauncher.class);

    private final AnalysisPipeline pipeline;
    private final AnalysisService analysisService;
    private final AnalysisTempFiles tempFiles;
    private final TaskExecutor executor;

    AnalysisPipelineLauncher(
            AnalysisPipeline pipeline,
            AnalysisService analysisService,
            AnalysisTempFiles tempFiles,
            @Qualifier(AnalysisExecutorConfig.EXECUTOR) TaskExecutor executor) {
        this.pipeline = pipeline;
        this.analysisService = analysisService;
        this.tempFiles = tempFiles;
        this.executor = executor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void launch(AnalysisRequestedEvent event) {
        long id = event.analysisId();
        try {
            executor.execute(() -> pipeline.run(id));
        } catch (TaskRejectedException e) {
            log.warn("분석 실행기가 가득 차 분석 {}을 시작하지 못함", id);
            analysisService.markFailed(
                    id, ErrorCode.ANALYSIS_500, ErrorCode.ANALYSIS_500.message());
            tempFiles.delete(id);
        }
    }

    /** 요청 트랜잭션이 롤백되면 행이 없으므로 먼저 써 둔 임시 파일도 지운다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    void discard(AnalysisRequestedEvent event) {
        tempFiles.delete(event.analysisId());
    }
}
