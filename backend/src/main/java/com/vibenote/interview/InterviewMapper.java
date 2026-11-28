package com.vibenote.interview;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL: resources/mapper/interview/InterviewMapper.xml (I-1a). */
@Mapper
public interface InterviewMapper {

    Optional<AnalysisInterview> findByAnalysis(long analysisId);

    Optional<AnalysisInterview> findById(long id);

    /** 생성된 id를 {@code interview.id}에 채운다. */
    void insert(AnalysisInterview interview);

    /** FAILED를 PENDING으로 되돌리고 실패 코드를 비운다. 횟수는 바꾸지 않는다 (AC5). */
    int resetFailed(@Param("id") long id, @Param("now") LocalDateTime now);

    /** DONE이고 횟수가 남았으면 PENDING으로 바꾸고 횟수를 1 늘린다 (AC6). 0이면 바꾸지 않았다. */
    int startRegeneration(
            @Param("id") long id, @Param("max") int max, @Param("now") LocalDateTime now);

    /** PENDING을 DONE으로 (generated_at = now). 이미 끝났으면 0. */
    int complete(@Param("id") long id, @Param("now") LocalDateTime now);

    /** PENDING을 FAILED로. 이미 끝났으면 0. */
    int markFailed(
            @Param("id") long id,
            @Param("errorCode") String errorCode,
            @Param("now") LocalDateTime now);

    /** PENDING인 면접의 질문을 모두 지운다 (서버 재시작, AC10). {@link #failAllPending}보다 먼저 부른다. */
    int deleteQuestionsOfPending();

    /** PENDING을 모두 FAILED로 (서버 재시작, AC10). */
    int failAllPending(@Param("errorCode") String errorCode, @Param("now") LocalDateTime now);

    int deleteQuestions(long interviewId);

    /** questions가 비면 부르지 않는다. */
    void insertQuestions(
            @Param("interviewId") long interviewId,
            @Param("questions") List<GeneratedQuestion> questions);

    /** no 오름차순. */
    List<InterviewQuestionRow> findQuestions(long interviewId);

    /** 분석의 면접 행을 지운다 (질문은 FK CASCADE, AC11). */
    int deleteByAnalysis(long analysisId);
}
