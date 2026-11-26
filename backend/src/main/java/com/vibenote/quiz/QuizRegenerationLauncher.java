package com.vibenote.quiz;

import com.vibenote.analysis.AnalysisExecutorConfig;
import com.vibenote.analysis.AnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 재생성 요청이 커밋된 뒤 분석 실행기에 넘긴다 (L-1a AC5, AnalysisPipelineLauncher와 같은 패턴). 큐가 차서 거절되면 FAILED (AC6).
 */
@Component
class QuizRegenerationLauncher {

    private static final Logger log = LoggerFactory.getLogger(QuizRegenerationLauncher.class);

    private final QuizRegenerationRunner runner;
    private final AnalysisService analysisService;
    private final TaskExecutor executor;

    QuizRegenerationLauncher(
            QuizRegenerationRunner runner,
            AnalysisService analysisService,
            @Qualifier(AnalysisExecutorConfig.EXECUTOR) TaskExecutor executor) {
        this.runner = runner;
        this.analysisService = analysisService;
        this.executor = executor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void launch(QuizRegenerationRequestedEvent event) {
        long id = event.analysisId();
        try {
            executor.execute(() -> runner.run(id));
        } catch (TaskRejectedException e) {
            log.warn("분석 실행기가 가득 차 분석 {}의 퀴즈 재생성을 시작하지 못함", id);
            analysisService.failQuizRegeneration(id);
        }
    }
}
