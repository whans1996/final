package com.vibenote.quiz;

import com.vibenote.analysis.AnalysisService;
import com.vibenote.analysis.AnalysisStatus;
import com.vibenote.analysis.domain.Analysis;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.quiz.dto.QuizRegenerationResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 퀴즈 재생성 요청 접수 (L-1a AC5·AC7). 상태 전환은 조건부 UPDATE 한 문장이고, 생성은 커밋 뒤 분석 실행기에서 한다 ({@link
 * QuizRegenerationLauncher}).
 */
@Service
public class QuizRegenerationService {

    private final AnalysisService analysisService;
    private final QuizProperties properties;
    private final ApplicationEventPublisher events;

    public QuizRegenerationService(
            AnalysisService analysisService,
            QuizProperties properties,
            ApplicationEventPublisher events) {
        this.analysisService = analysisService;
        this.properties = properties;
        this.events = events;
    }

    /**
     * 내 DONE 분석의 실패한 퀴즈를 다시 만들기 시작한다. 시작하지 못하면 분석을 다시 읽어 사유를 고른다: 없거나 남의 것(팀 공유 포함)·삭제됨은
     * ANALYSIS_404, DONE이 아니거나 퀴즈가 FAILED가 아님은 QUIZ_409, 횟수 소진은 QUIZ_429.
     */
    @Transactional
    public QuizRegenerationResponse request(long userId, long analysisId) {
        int max = properties.maxRegenerations();
        if (!analysisService.startQuizRegeneration(userId, analysisId, max)) {
            Analysis analysis =
                    analysisService
                            .findOwned(userId, analysisId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_404));
            if (analysis.getStatus() != AnalysisStatus.DONE
                    || analysis.getQuizStatus() != QuizStatus.FAILED
                    || analysis.getQuizRegenerations() < max) {
                throw new BusinessException(ErrorCode.QUIZ_409);
            }
            throw new BusinessException(
                    ErrorCode.QUIZ_429,
                    ErrorCode.QUIZ_429.message().replace("{limit}", String.valueOf(max)),
                    null);
        }
        int used =
                analysisService
                        .findOwned(userId, analysisId)
                        .map(Analysis::getQuizRegenerations)
                        .orElse(max);
        events.publishEvent(new QuizRegenerationRequestedEvent(analysisId));
        return new QuizRegenerationResponse(
                analysisId, QuizStatus.GENERATING, Math.max(0, max - used));
    }
}
