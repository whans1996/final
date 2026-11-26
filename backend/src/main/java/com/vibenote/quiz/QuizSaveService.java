package com.vibenote.quiz;

import com.vibenote.analysis.AnalysisService;
import com.vibenote.quiz.domain.Quiz;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 생성한 퀴즈 저장 (F-14 AC1)과 퀴즈 상태 기록 (L-1a AC4·AC5). LLM 호출과 분리한 짧은 트랜잭션이다. 상태는 분석 도메인({@link
 * AnalysisService})이 쓰고 이 트랜잭션에 참여한다.
 */
@Service
public class QuizSaveService {

    private final QuizMapper mapper;
    private final AnalysisService analysisService;
    private final Clock clock;

    public QuizSaveService(QuizMapper mapper, AnalysisService analysisService, Clock clock) {
        this.mapper = mapper;
        this.analysisService = analysisService;
        this.clock = clock;
    }

    /** QUIZ 단계: 문항이 있으면 quizzes 1행과 문항을 넣고 READY, 비었으면 FAILED를 같은 트랜잭션으로 기록한다. */
    @Transactional
    public void save(long analysisId, List<GeneratedQuestion> questions) {
        analysisService.updateQuizStatus(
                analysisId, questions.isEmpty() ? QuizStatus.FAILED : QuizStatus.READY);
        insert(analysisId, questions);
    }

    /**
     * 재생성 결과 (L-1a AC5·AC6·AC8). 분석이 아직 GENERATING이고 삭제되지 않았을 때만 상태를 바꾸고, 문항이 있으면 READY와 퀴즈를 한
     * 트랜잭션으로 넣는다. 그사이 삭제·탈퇴했으면 아무것도 하지 않는다.
     */
    @Transactional
    public void saveRegenerated(long analysisId, List<GeneratedQuestion> questions) {
        QuizStatus status = questions.isEmpty() ? QuizStatus.FAILED : QuizStatus.READY;
        if (analysisService.finishQuizRegeneration(analysisId, status)) {
            insert(analysisId, questions);
        }
    }

    /** questions가 비면 아무것도 하지 않는다. */
    private void insert(long analysisId, List<GeneratedQuestion> questions) {
        if (questions.isEmpty()) {
            return;
        }
        Quiz quiz = Quiz.of(analysisId, LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS));
        mapper.insertQuiz(quiz);
        mapper.insertQuestions(quiz.getId(), questions);
    }
}
