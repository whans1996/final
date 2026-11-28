package com.vibenote.quiz;

import com.vibenote.quiz.domain.Quiz;
import com.vibenote.quiz.domain.QuizAttempt;
import com.vibenote.quiz.domain.QuizQuestion;
import com.vibenote.quiz.domain.WrongNoteRow;
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

    /** 미삭제 분석의 퀴즈와 분석 주인 id({@code ownerId}). 없거나 분석이 삭제됐으면 empty. 권한은 호출자가 본다 (F-20b AC5). */
    Optional<Quiz> findActive(@Param("quizId") long quizId);

    /** id 오름차순. */
    List<QuizQuestion> findQuestions(@Param("quizId") long quizId);

    /** 내 가장 최근 풀이 ({@code created_at}, 같으면 id가 큰 것). */
    Optional<QuizAttempt> findLastAttempt(
            @Param("quizId") long quizId, @Param("userId") long userId);

    /** 그 퀴즈의 내 풀이 최신순({@code created_at} 내림차순, 같으면 id 내림차순) {@code limit}개 (L-1b AC1). */
    List<QuizAttempt> findAttempts(
            @Param("quizId") long quizId, @Param("userId") long userId, @Param("limit") int limit);

    /** 그 퀴즈의 내 풀이 하나. 남의 풀이·다른 퀴즈의 풀이·없는 id면 empty (L-1b AC4). */
    Optional<QuizAttempt> findAttempt(
            @Param("quizId") long quizId,
            @Param("userId") long userId,
            @Param("attemptId") long attemptId);

    /** 풀이의 답 (문항 id 오름차순). */
    List<AnswerRow> findAnswers(@Param("attemptId") long attemptId);

    /** 지금 열 수 있는 분석에서 내 마지막 답이 틀린 문항 수 (L-1b AC5·AC7·AC8). */
    int countWrongNotes(@Param("userId") long userId);

    /** 위 문항을 마지막으로 답한 시각 내림차순 → 문항 id 오름차순으로 {@code limit}개. */
    List<WrongNoteRow> findWrongNotes(@Param("userId") long userId, @Param("limit") int limit);

    void insertAttempt(QuizAttempt attempt);

    /** answers가 비면 부르지 않는다. */
    void insertAnswers(
            @Param("attemptId") long attemptId, @Param("answers") List<AnswerRow> answers);

    /** quiz_answers 행. */
    record AnswerRow(long questionId, int selectedIndex, boolean correct) {}
}
