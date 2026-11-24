package com.vibenote.challenge;

import com.vibenote.challenge.domain.Challenge;
import com.vibenote.challenge.domain.ChallengeParticipant;
import com.vibenote.challenge.dto.ChallengeDetailResponse;
import com.vibenote.challenge.dto.ChallengeProgressResponse;
import com.vibenote.challenge.dto.ChallengeSearchRequest;
import com.vibenote.challenge.dto.ChallengeSummaryResponse;
import com.vibenote.challenge.dto.JoinedChallengeSearchRequest;
import com.vibenote.challenge.dto.MyChallengeResponse;
import com.vibenote.challenge.dto.ParticipationResponse;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.feed.ActivityService;
import com.vibenote.feed.ActivityType;
import com.vibenote.user.UserService;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원용 챌린지 목록·상세, 참여·취소, 참여한 챌린지 목록 (F-25). 판단 순서는 401 → 404 → 400 → 409다.
 *
 * <p>참여·취소는 챌린지 행을 잠가(FOR UPDATE) 같은 챌린지의 참여·취소·관리자 수정을 직렬화한다. 둘 다 멱등이다.
 */
@Service
@Transactional(readOnly = true)
public class ChallengeService {

    private final ChallengeMapper challengeMapper;
    private final ChallengeParticipantMapper participantMapper;
    private final ChallengeProgressService progressService;
    private final ActivityService activityService;
    private final UserService userService;
    private final Validator validator;
    private final Clock clock;

    public ChallengeService(
            ChallengeMapper challengeMapper,
            ChallengeParticipantMapper participantMapper,
            ChallengeProgressService progressService,
            ActivityService activityService,
            UserService userService,
            Validator validator,
            Clock clock) {
        this.challengeMapper = challengeMapper;
        this.participantMapper = participantMapper;
        this.progressService = progressService;
        this.activityService = activityService;
        this.userService = userService;
        this.validator = validator;
        this.clock = clock;
    }

    // ── 목록·상세 (AC1·AC2) ──

    /**
     * @param viewerId 비로그인이면 null (joined는 모두 false)
     */
    public PageResponse<ChallengeSummaryResponse> search(
            ChallengeSearchRequest search, Long viewerId) {
        LocalDate today = today();
        ChallengeStatus status = search.statusValue();
        PageQuery page = search.pageQuery();
        long total = challengeMapper.countForMember(status, today);
        List<ChallengeSummaryResponse> items =
                total <= page.offset()
                        ? List.of()
                        : challengeMapper
                                .findForMember(status, today, viewerId, page.size(), page.offset())
                                .stream()
                                .map(c -> ChallengeSummaryResponse.of(c, today))
                                .toList();
        return PageResponse.of(items, page, total);
    }

    /**
     * @param viewerId 비로그인이면 null (participation은 null)
     */
    public ChallengeDetailResponse getDetail(long id, Long viewerId) {
        Challenge challenge =
                challengeMapper
                        .findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_404));
        ChallengeProgressResponse participation =
                viewerId == null ? null : progressOf(challenge, viewerId);
        return ChallengeDetailResponse.of(challenge, today(), participation);
    }

    // ── 참여·취소 (AC3·AC4·AC9) ──

    /**
     * 예정·진행 중 챌린지에 참여한다. 처음 참여하면 CHALLENGE_JOIN 활동을 남기고 시작일부터 다시 센다 (참여하자마자 달성할 수 있다).
     *
     * @throws BusinessException AUTH_401 탈퇴한 회원, CHALLENGE_404, CHALLENGE_002 종료된 챌린지
     */
    @Transactional
    public ParticipationResponse join(long challengeId, long userId) {
        userService.getActiveMember(userId);
        Challenge challenge = lockOrThrow(challengeId);
        rejectEnded(challenge);
        if (participantMapper.insertIgnore(challengeId, userId, now()) == 1) {
            activityService.record(userId, ActivityType.CHALLENGE_JOIN, challengeId);
            progressService.recount(challenge, userId);
        }
        return new ParticipationResponse(
                true,
                participantMapper.countByChallenge(challengeId),
                progressOf(challenge, userId));
    }

    /**
     * 참여를 취소하고 그 챌린지의 내 CHALLENGE_JOIN 활동을 지운다. 참여하지 않았으면 아무것도 하지 않는다.
     *
     * @throws BusinessException AUTH_401 탈퇴한 회원, CHALLENGE_404, CHALLENGE_002 종료된 챌린지,
     *     CHALLENGE_003 달성한 챌린지
     */
    @Transactional
    public ParticipationResponse cancel(long challengeId, long userId) {
        userService.getActiveMember(userId);
        Challenge challenge = lockOrThrow(challengeId);
        rejectEnded(challenge);
        ChallengeParticipant participant =
                participantMapper.findForUpdate(challengeId, userId).orElse(null);
        if (participant != null) {
            if (participant.isCompleted()) {
                throw new BusinessException(ErrorCode.CHALLENGE_003);
            }
            participantMapper.delete(challengeId, userId);
            activityService.delete(userId, ActivityType.CHALLENGE_JOIN, challengeId);
        }
        return new ParticipationResponse(
                false, participantMapper.countByChallenge(challengeId), null);
    }

    // ── 참여한 챌린지 (AC10·AC11) ──

    /** 내 챌린지. 인증은 SecurityConfig가 막는다. */
    public PageResponse<MyChallengeResponse> listMine(
            long userId, JoinedChallengeSearchRequest search) {
        return listJoined(userId, search);
    }

    /**
     * 회원의 참여 챌린지 (공개).
     *
     * @throws BusinessException USER_404 없거나 탈퇴한 회원, COMMON_400 잘못된 조건
     */
    public PageResponse<MyChallengeResponse> listByUser(
            long userId, JoinedChallengeSearchRequest search) {
        userService.getPublicMember(userId);
        return listJoined(userId, search);
    }

    private PageResponse<MyChallengeResponse> listJoined(
            long userId, JoinedChallengeSearchRequest search) {
        validate(search);
        LocalDate today = today();
        ChallengeStatus status = search.statusValue();
        PageQuery page = search.pageQuery();
        long total = participantMapper.countJoined(userId, status, today);
        List<MyChallengeResponse> items =
                total <= page.offset()
                        ? List.of()
                        : participantMapper
                                .findJoined(userId, status, today, page.size(), page.offset())
                                .stream()
                                .map(c -> MyChallengeResponse.of(c, today))
                                .toList();
        return PageResponse.of(items, page, total);
    }

    private void validate(JoinedChallengeSearchRequest search) {
        Map<String, String> details = new LinkedHashMap<>();
        validator
                .validate(search)
                .forEach(v -> details.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        if (!details.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_400, details);
        }
    }

    // ── 공통 ──

    private ChallengeProgressResponse progressOf(Challenge challenge, long userId) {
        return participantMapper
                .find(challenge.getId(), userId)
                .map(p -> ChallengeProgressResponse.of(p, challenge.getGoalValue()))
                .orElse(null);
    }

    private Challenge lockOrThrow(long id) {
        return challengeMapper
                .findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_404));
    }

    private void rejectEnded(Challenge challenge) {
        if (challenge.statusOn(today()) == ChallengeStatus.ENDED) {
            throw new BusinessException(ErrorCode.CHALLENGE_002);
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
