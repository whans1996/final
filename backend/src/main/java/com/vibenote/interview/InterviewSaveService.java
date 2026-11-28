package com.vibenote.interview;

import com.vibenote.analysis.AnalysisDeletedEvent;
import com.vibenote.common.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 면접 질문 생성 결과 기록 (I-1a AC1·AC4·AC6·AC10·AC11). LLM 호출과 분리한 짧은 트랜잭션으로, 상태가 PENDING일 때만 바꾼다. FAILED는
 * 질문 행이 없다.
 */
@Service
public class InterviewSaveService {

    private final InterviewMapper mapper;
    private final Clock clock;

    public InterviewSaveService(InterviewMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    /** 아직 PENDING인 면접 (생성기 입력). 이미 끝났거나 없으면 empty. */
    @Transactional(readOnly = true)
    public Optional<AnalysisInterview> findPending(long interviewId) {
        return mapper.findById(interviewId).filter(i -> i.getStatus() == InterviewStatus.PENDING);
    }

    /** 이전 질문을 새 질문으로 바꾸고 DONE으로 (AC6). 그 사이 복구 등으로 이미 끝났으면 아무것도 바꾸지 않는다. */
    @Transactional
    public boolean complete(long interviewId, List<GeneratedQuestion> questions) {
        if (mapper.complete(interviewId, now()) != 1) {
            return false;
        }
        mapper.deleteQuestions(interviewId);
        if (!questions.isEmpty()) {
            mapper.insertQuestions(interviewId, questions);
        }
        return true;
    }

    /** PENDING을 FAILED로 바꾸고 이전 질문을 지운다. 커밋 이후 콜백(AFTER_COMMIT)에서도 불리므로 새 트랜잭션으로 쓴다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(long interviewId, ErrorCode code) {
        if (mapper.markFailed(interviewId, code.name(), now()) == 1) {
            mapper.deleteQuestions(interviewId);
        }
    }

    /** PENDING을 모두 FAILED·ANALYSIS_500으로 (서버 재시작, AC10). 바꾼 수를 돌려준다. */
    @Transactional
    public int failInterrupted() {
        mapper.deleteQuestionsOfPending();
        return mapper.failAllPending(ErrorCode.ANALYSIS_500.name(), now());
    }

    /** 분석 삭제·실패 분석 재실행 때 그 분석의 면접 질문을 지운다. 삭제 트랜잭션 안에서 함께 처리된다 (AC11). */
    @EventListener
    public void onAnalysisDeleted(AnalysisDeletedEvent event) {
        mapper.deleteByAnalysis(event.analysisId());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
