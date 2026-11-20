package com.vibenote.quiz;

import com.vibenote.quiz.domain.Quiz;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 생성한 퀴즈 저장 (F-14 AC1). LLM 호출과 분리한 짧은 트랜잭션이다. */
@Service
public class QuizSaveService {

    private final QuizMapper mapper;
    private final Clock clock;

    public QuizSaveService(QuizMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    /** quizzes 1행과 문항을 순서대로 넣는다. questions가 비면 아무것도 하지 않는다. */
    @Transactional
    public void save(long analysisId, List<GeneratedQuestion> questions) {
        if (questions.isEmpty()) {
            return;
        }
        Quiz quiz = Quiz.of(analysisId, LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS));
        mapper.insertQuiz(quiz);
        mapper.insertQuestions(quiz.getId(), questions);
    }
}
