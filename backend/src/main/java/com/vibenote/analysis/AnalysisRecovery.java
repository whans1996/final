package com.vibenote.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 서버가 꺼질 때 진행 중이던 분석은 이어서 실행할 스레드가 없으므로 시작할 때 FAILED로 정리하고 남은 임시 ZIP을 지운다 (F-05a AC11). 서버는 한 대라고
 * 가정한다.
 */
@Component
public class AnalysisRecovery {

    static final String RESTART_MESSAGE = "서버가 다시 시작되어 분석이 중단되었습니다. 다시 요청해 주세요.";

    private static final Logger log = LoggerFactory.getLogger(AnalysisRecovery.class);

    private final AnalysisService analysisService;
    private final AnalysisTempFiles tempFiles;

    public AnalysisRecovery(AnalysisService analysisService, AnalysisTempFiles tempFiles) {
        this.analysisService = analysisService;
        this.tempFiles = tempFiles;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recover() {
        int failed = analysisService.failInterrupted(RESTART_MESSAGE);
        if (failed > 0) {
            log.warn("재시작으로 중단된 분석 {}개를 FAILED로 표시함", failed);
        }
        tempFiles.deleteAll();
    }
}
