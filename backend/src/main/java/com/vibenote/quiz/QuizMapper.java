package com.vibenote.quiz;

import com.vibenote.quiz.domain.Quiz;
import com.vibenote.quiz.domain.QuizAttempt;
import com.vibenote.quiz.domain.QuizQuestion;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SQL: resources/mapper/quiz/QuizMapper.xml. quizzes·quiz_questions·quiz_attempts·quiz_answers
 * (F-14).
 */
@Mapper
public interface QuizMapper {

    void insertQuiz(Quiz quiz);

    /** questions가 비면 부르지 않는다 (VALUES가 빈 SQL 오류). 순서대로 넣어 id 순서 = 문항 번호가 된다. */
    void insertQuestions(
            @Param("quizId") long quizId, @Param("questions") List<GeneratedQuestion> questions);

    Optional<Quiz> findByAnalysis(@Param("analysisId") long analysisId);

    /** 내 미삭제 분석의 퀴즈. 없거나 남의 분석이거나 분석이 삭제됐으면 empty. */
    Optional<Quiz> findOwned(@Param("quizId") long quizId, @Param("userId") long userId);

    /** id 오름차순. */
    List<QuizQuestion> findQuestions(@Param("quizId") long quizId);

    /** 내 가장 최근 풀이 ({@code created_at}, 같으면 id가 큰 것). */
    Optional<QuizAttempt> findLastAttempt(
            @Param("quizId") long quizId, @Param("userId") long userId);

    void insertAttempt(QuizAttempt attempt);

    /** answers가 비면 부르지 않는다. */
    void insertAnswers(
            @Param("attemptId") long attemptId, @Param("answers") List<AnswerRow> answers);

    /** quiz_answers 행. */
    record AnswerRow(long questionId, int selectedIndex, boolean correct) {}
}
