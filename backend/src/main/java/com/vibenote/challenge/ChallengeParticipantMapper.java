package com.vibenote.challenge;

import com.vibenote.challenge.domain.Challenge;
import com.vibenote.challenge.domain.ChallengeParticipant;
import com.vibenote.challenge.domain.JoinedChallenge;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SQL: resources/mapper/challenge/ChallengeParticipantMapper.xml. 참여·달성도 (F-25). 집계 쿼리는
 * review_plans·quiz_attempts· analyses를 읽기만 하고, 삭제한 분석의 활동도 센다 (backend/CLAUDE.md).
 *
 * <p>기간 인자 {@code from}·{@code until}은 챌린지 시작일과 종료일 다음 날이다 ({@code from ≤ 시각 < until}, DB 세션
 * +09:00).
 */
@Mapper
public interface ChallengeParticipantMapper {

    /** 이미 참여했으면 넣지 않는다. @return 새로 넣었으면 1 */
    int insertIgnore(
            @Param("challengeId") long challengeId,
            @Param("userId") long userId,
            @Param("joinedAt") LocalDateTime joinedAt);

    int delete(@Param("challengeId") long challengeId, @Param("userId") long userId);

    Optional<ChallengeParticipant> find(
            @Param("challengeId") long challengeId, @Param("userId") long userId);

    /** 취소용. 집계(refresh)가 참여 행만 잠그므로 같은 행을 잠그고 최신 달성 여부를 읽는다. */
    Optional<ChallengeParticipant> findForUpdate(
            @Param("challengeId") long challengeId, @Param("userId") long userId);

    long countByChallenge(long challengeId);

    /** 회원이 참여한 챌린지 (joined_at DESC, challenge_id DESC). status가 null이면 전체다. */
    List<JoinedChallenge> findJoined(
            @Param("userId") long userId,
            @Param("status") ChallengeStatus status,
            @Param("today") LocalDate today,
            @Param("size") int size,
            @Param("offset") long offset);

    long countJoined(
            @Param("userId") long userId,
            @Param("status") ChallengeStatus status,
            @Param("today") LocalDate today);

    // ── 집계 ──

    /**
     * {@code date}가 기간 안이고 아직 달성하지 않은, 그 유형의 내 참여 챌린지. 참여 행을 잠근다 (FOR UPDATE OF p, 챌린지 행은 잠그지 않는다).
     */
    List<Challenge> findOpenForUpdate(
            @Param("userId") long userId,
            @Param("goalType") ChallengeGoalType goalType,
            @Param("date") LocalDate date);

    /**
     * 종료일이 {@code from} 이상 {@code until} 미만인 챌린지의 미달성 참여 (challenge_id, user_id 순). 잠그지 않는다 (#146
     * 다시 세기 대상).
     */
    List<ChallengeParticipant> findOpenEnded(
            @Param("from") LocalDate from, @Param("until") LocalDate until);

    /** 아직 달성하지 않은 그 참여의 챌린지. 참여 행을 잠근다 ({@link #findOpenForUpdate}와 같은 FOR UPDATE OF p). */
    Optional<Challenge> findOpenOneForUpdate(
            @Param("challengeId") long challengeId, @Param("userId") long userId);

    /** DONE이고 done_at이 기간 안인 복습 계획 수. */
    int countDoneReviews(
            @Param("userId") long userId,
            @Param("from") LocalDate from,
            @Param("until") LocalDate until);

    /** 기간 안 퀴즈 풀이의 (퀴즈, 날짜) 조합 수. */
    int countQuizDays(
            @Param("userId") long userId,
            @Param("from") LocalDate from,
            @Param("until") LocalDate until);

    /** DONE이고 요청 시각(created_at)이 기간 안인 분석 수. */
    int countDoneAnalyses(
            @Param("userId") long userId,
            @Param("from") LocalDate from,
            @Param("until") LocalDate until);

    /** 미달성 참여만 바꾼다. completedAt이 null이 아니면 달성 처리한다. @return 바꾼 행 수 */
    int updateProgress(
            @Param("challengeId") long challengeId,
            @Param("userId") long userId,
            @Param("progress") int progress,
            @Param("completedAt") LocalDateTime completedAt);
}
