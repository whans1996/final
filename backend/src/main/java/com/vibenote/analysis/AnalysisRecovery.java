package com.vibenote.analysis;

import com.vibenote.analysis.flow.FlowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

/**
 * 서버가 꺼질 때 진행 중이던 분석은 이어서 실행할 스레드가 없으므로 시작할 때 FAILED로 정리하고 남은 임시 ZIP을 지운다 (F-05a AC11). 생성 중이던 실행
 * 흐름도 FAILED로 정리한다 (F-07 AC11). 재생성 중이던 퀴즈도 FAILED로 정리한다 (L-1a AC8). 서버는 한 대라고 가정한다.
 *
 * <p>ApplicationReadyEvent는 웹 서버가 요청을 받기 시작한 뒤라 그 사이에 들어온 새 분석까지 정리할 수 있다. 그래서 모든 싱글톤을 만든 직후, 웹 서버가
 * 시작되기 전에 실행한다.
 */
@Component
public class AnalysisRecovery implements SmartInitializingSingleton {

    static final String RESTART_MESSAGE = "서버가 다시 시작되어 분석이 중단되었습니다. 다시 요청해 주세요.";

    private static final Logger log = LoggerFactory.getLogger(AnalysisRecovery.class);

    private final AnalysisService analysisService;
    private final AnalysisTempFiles tempFiles;
    private final FlowService flowService;

    public AnalysisRecovery(
            AnalysisService analysisService, AnalysisTempFiles tempFiles, FlowService flowService) {
        this.analysisService = analysisService;
        this.tempFiles = tempFiles;
        this.flowService = flowService;
    }

    @Override
    public void afterSingletonsInstantiated() {
        recover();
    }

    public void recover() {
        int failed = analysisService.failInterrupted(RESTART_MESSAGE);
        if (failed > 0) {
            log.warn("재시작으로 중단된 분석 {}개를 FAILED로 표시함", failed);
        }
        tempFiles.deleteAll();
        int flows = flowService.failInterrupted();
        if (flows > 0) {
            log.warn("재시작으로 중단된 실행 흐름 {}개를 FAILED로 표시함", flows);
        }
        int quizzes = analysisService.failInterruptedQuizRegenerations();
        if (quizzes > 0) {
            log.warn("재시작으로 중단된 퀴즈 재생성 {}개를 FAILED로 표시함", quizzes);
        }
    }
}
