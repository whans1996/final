package com.vibenote.challenge;

import com.vibenote.challenge.domain.Challenge;
import com.vibenote.feed.ActivityService;
import com.vibenote.feed.ActivityType;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 챌린지 달성도 집계 (F-25). 활동이 생기거나 사라진 트랜잭션 안에서(분석 완료는 커밋 뒤 별도 트랜잭션에서, {@link
 * AnalysisChallengeListener}) 불려 그 회원의 참여 행을 다시 센다. 조회 API는 저장된 값만 읽는다.
 *
 * <p>달성(completed_at)한 참여는 다시 세지 않는다. {@code progress = min(센 수, 목표)}이고, 목표에 닿는 순간 completed_at을
 * 채우고 CHALLENGE_COMPLETE 활동을 남긴다. 미달성 조건부 UPDATE라 같은 회원의 활동이 겹쳐도 COMPLETE는 한 번만 남는다.
 *
 * <p>부르는 트랜잭션은 READ_COMMITTED여야 한다. REPEATABLE READ면 참여 행 잠금 전에 잡힌 스냅샷으로 세어, 잠금을 기다리는 동안 커밋된 같은 회원의
 * 활동이 빠진다 (#104). COUNT에 {@code FOR SHARE}는 쓰지 않는다 (서로의 미커밋 활동 행을 기다려 교착).
 */
@Service
public class ChallengeProgressService {

    private final ChallengeParticipantMapper participantMapper;
    private final ActivityService activityService;
    private final Clock clock;

    public ChallengeProgressService(
            ChallengeParticipantMapper participantMapper,
            ActivityService activityService,
            Clock clock) {
        this.participantMapper = participantMapper;
        this.activityService = activityService;
        this.clock = clock;
    }

    /**
     * {@code activityDate}가 기간 안이고 아직 달성하지 않은, {@code goalType} 유형의 내 참여를 모두 다시 센다. 호출자의 트랜잭션에
     * 참여한다.
     *
     * @param activityDate 생기거나 사라진 활동의 날짜 (복습 done_at, 퀴즈 풀이 시각, 분석 요청 시각의 Asia/Seoul 날짜)
     */
    @Transactional
    public void refresh(long userId, ChallengeGoalType goalType, LocalDate activityDate) {
        for (Challenge challenge :
                participantMapper.findOpenForUpdate(userId, goalType, activityDate)) {
            recount(challenge, userId);
        }
    }

    /**
     * 아직 달성하지 않은 참여 하나를 잠그고 다시 센다 (#146, {@link ChallengeRecountJob}). 그 사이 달성했거나 취소했으면 하지 않는다.
     * 호출자의 트랜잭션에 참여한다.
     */
    @Transactional
    public void recountOpen(long challengeId, long userId) {
        participantMapper
                .findOpenOneForUpdate(challengeId, userId)
                .ifPresent(challenge -> recount(challenge, userId));
    }

    /** 참여 직후 그 챌린지 하나를 센다 (AC4). 호출자가 챌린지를 잠그고 미달성 참여 행을 넣은 뒤 부른다. */
    @Transactional
    public void recount(Challenge challenge, long userId) {
        LocalDate from = challenge.getStartDate();
        LocalDate until = challenge.getEndDate().plusDays(1);
        int count =
                switch (challenge.getGoalType()) {
                    case REVIEW_COUNT -> participantMapper.countDoneReviews(userId, from, until);
                    case QUIZ_COUNT -> participantMapper.countQuizDays(userId, from, until);
                    case ANALYSIS_COUNT -> participantMapper.countDoneAnalyses(userId, from, until);
                };
        int progress = Math.min(count, challenge.getGoalValue());
        LocalDateTime completedAt = progress >= challenge.getGoalValue() ? now() : null;
        int updated =
                participantMapper.updateProgress(challenge.getId(), userId, progress, completedAt);
        if (completedAt != null && updated == 1) {
            activityService.record(userId, ActivityType.CHALLENGE_COMPLETE, challenge.getId());
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
