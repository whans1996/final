package com.vibenote.quiz;

import com.vibenote.analysis.AnalysisService;
import com.vibenote.analysis.concept.AnalysisConceptService;
import com.vibenote.analysis.domain.Analysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 퀴즈 재생성 1회 (L-1a AC5·AC6). F-14 QUIZ 단계와 같은 입력(연결 개념·선별 파일·분석 수준)으로 LLM을 트랜잭션 밖에서 부르고 결과만 짧은
 * 트랜잭션으로 저장한다. 빈 결과·예기치 못한 예외는 FAILED이고 사용한 횟수는 돌려주지 않는다. 분석 실행기 스레드에서 부른다.
 */
@Component
public class QuizRegenerationRunner {

    private static final Logger log = LoggerFactory.getLogger(QuizRegenerationRunner.class);

    private final AnalysisService analysisService;
    private final AnalysisConceptService analysisConceptService;
    private final QuizGenerator generator;
    private final QuizSaveService quizSaveService;

    public QuizRegenerationRunner(
            AnalysisService analysisService,
            AnalysisConceptService analysisConceptService,
            QuizGenerator generator,
            QuizSaveService quizSaveService) {
        this.analysisService = analysisService;
        this.analysisConceptService = analysisConceptService;
        this.generator = generator;
        this.quizSaveService = quizSaveService;
    }

    public void run(long analysisId) {
        try {
            if (!analysisService.isQuizRegenerating(analysisId)) {
                // 그사이 삭제·탈퇴했으면 결과를 버리게 되므로 LLM을 부르지 않는다 (AC8)
                log.info("분석 {}이 삭제됐거나 재생성 중이 아니어서 퀴즈 재생성을 멈춤", analysisId);
                return;
            }
            Analysis analysis = analysisService.findForPipeline(analysisId);
            quizSaveService.saveRegenerated(
                    analysisId,
                    generator.generate(
                            analysis,
                            analysisConceptService.findForQuiz(analysisId),
                            analysisService.findFiles(analysisId)));
        } catch (RuntimeException e) {
            log.error("분석 {} 퀴즈 재생성 오류", analysisId, e);
            analysisService.failQuizRegeneration(analysisId);
        } catch (Error e) {
            // GENERATING으로 남으면 화면이 계속 폴링하므로 FAILED로 바꾸고 스레드에는 그대로 알린다 (AnalysisPipeline과 같음)
            log.error("분석 {} 퀴즈 재생성 치명적 오류", analysisId, e);
            analysisService.failQuizRegeneration(analysisId);
            throw e;
        }
    }
}
