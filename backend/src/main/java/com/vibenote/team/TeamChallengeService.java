package com.vibenote.team;

import com.vibenote.common.config.ClockConfig;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.team.TeamChallengeCalculator.Result;
import com.vibenote.team.TeamUnderstandingCalculator.Member;
import com.vibenote.team.domain.PeriodAttempt;
import com.vibenote.team.domain.Team;
import com.vibenote.team.domain.TeamChallenge;
import com.vibenote.team.dto.CreateTeamChallengeRequest;
import com.vibenote.team.dto.TeamChallengeResponse;
import com.vibenote.user.UserService;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 팀 기간 목표 (F-20c). 팀장이 공유 분석 하나에 기간과 목표 정답률을 정하고, 달성은 조회할 때마다 지금 멤버의 기간 안 가장 최근 풀이로 계산한다 (저장하지
 * 않는다). 공유가 해제되면 목표도 FK CASCADE로 지워진다.
 *
 * <p>판단 순서: 401(탈퇴 회원) → 404 TEAM_404 → 403 TEAM_403 → 400 검증 → 대상(404 TEAM_006·TEAM_013) → 409.
 */
@Service
public class TeamChallengeService {

    static final int MAX_CHALLENGES = 20;
    static final int MAX_DAYS = 30;

    /** 시작일 상한(오늘 + n일). MySQL DATE 범위(9999-12-31)를 넘는 날짜가 500이 되지 않게 막는다 (#95). */
    static final int MAX_START_AHEAD_DAYS = 365;

    /** 진행 중(종료일 ASC) → 예정(시작일 ASC) → 종료(종료일 DESC), 같으면 목표 id DESC (AC4). */
    private static final Comparator<TeamChallengeResponse> ORDER =
            Comparator.comparingInt((TeamChallengeResponse r) -> statusRank(r.status()))
                    .thenComparingLong(TeamChallengeService::dateKey)
                    .thenComparing(TeamChallengeResponse::challengeId, Comparator.reverseOrder());

    private final TeamMapper teamMapper;
    private final TeamChallengeMapper challengeMapper;
    private final UserService userService;
    private final Validator validator;
    private final Clock clock;

    public TeamChallengeService(
            TeamMapper teamMapper,
            TeamChallengeMapper challengeMapper,
            UserService userService,
            Validator validator,
            Clock clock) {
        this.teamMapper = teamMapper;
        this.challengeMapper = challengeMapper;
        this.userService = userService;
        this.validator = validator;
        this.clock = clock;
    }

    /**
     * 목표를 만든다 (AC1~AC3). 잠금 순서는 {@link TeamAnalysisService#share}와 같이 users → teams이고, 팀 행을 잠근 뒤
     * 중복·상한을 센다. 마지막으로 공유 확인이 team_analyses 행을 공유 잠금으로 읽어, 공유 해제·탈퇴의 삭제와 겹쳐도 FK 위반(500) 대신
     * TEAM_006이 된다 (#168).
     */
    @Transactional
    public TeamChallengeResponse create(
            long teamId, long userId, CreateTeamChallengeRequest request) {
        userService.lockActiveMember(userId);
        Team team = teamMapper.findByIdForUpdate(teamId);
        requireOwner(team, teamId, userId);
        LocalDate today = LocalDate.now(clock);
        Period period = validate(request, today);
        long analysisId = request.analysisId();
        if (!challengeMapper.isShared(teamId, analysisId)) {
            throw new BusinessException(ErrorCode.TEAM_006);
        }
        if (challengeMapper.existsOpen(teamId, analysisId, today)) {
            throw new BusinessException(ErrorCode.TEAM_011);
        }
        if (challengeMapper.countByTeam(teamId) >= MAX_CHALLENGES) {
            throw new BusinessException(ErrorCode.TEAM_012);
        }
        challengeMapper.insert(
                teamId,
                analysisId,
                period.start(),
                period.end(),
                request.targetRate(),
                userId,
                LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS));
        long challengeId = challengeMapper.lastInsertId();
        return load(teamId, null, challengeId, today).get(0);
    }

    /** 미삭제 분석의 목표를 AC4 순서로 (analysisId를 주면 그 분석만). */
    @Transactional(readOnly = true)
    public List<TeamChallengeResponse> list(long teamId, long userId, Long analysisId) {
        userService.getActiveMember(userId);
        requireRole(teamMapper.findById(teamId), teamId, userId);
        if (analysisId != null && analysisId < 1) {
            throw new BusinessException(
                    ErrorCode.COMMON_400, Map.of("analysisId", "분석 ID는 1 이상이어야 합니다."));
        }
        return load(teamId, analysisId, null, LocalDate.now(clock));
    }

    /** 상태와 상관없이 목표 행만 지운다. 풀이 기록은 남는다 (AC7). */
    @Transactional
    public void delete(long teamId, long userId, long challengeId) {
        userService.getActiveMember(userId);
        requireOwner(teamMapper.findById(teamId), teamId, userId);
        if (challengeMapper.delete(teamId, challengeId) == 0) {
            throw new BusinessException(ErrorCode.TEAM_013);
        }
    }

    private List<TeamChallengeResponse> load(
            long teamId, Long analysisId, Long challengeId, LocalDate today) {
        List<TeamChallenge> rows = challengeMapper.findChallenges(teamId, analysisId, challengeId);
        if (rows.isEmpty()) return List.of();
        List<Member> members =
                teamMapper.findMembers(teamId).stream()
                        .map(m -> new Member(m.getUserId(), m.getNickname(), m.getRole()))
                        .toList();
        Map<Long, Map<Long, PeriodAttempt>> attempts = new HashMap<>();
        for (PeriodAttempt a :
                challengeMapper.findPeriodAttempts(teamId, analysisId, challengeId)) {
            attempts.computeIfAbsent(a.challengeId(), k -> new HashMap<>()).put(a.userId(), a);
        }
        return rows.stream()
                .map(
                        row ->
                                toResponse(
                                        row,
                                        today,
                                        members,
                                        attempts.getOrDefault(row.getId(), Map.of())))
                .sorted(ORDER)
                .toList();
    }

    private static TeamChallengeResponse toResponse(
            TeamChallenge row,
            LocalDate today,
            List<Member> members,
            Map<Long, PeriodAttempt> attempts) {
        Result r = TeamChallengeCalculator.evaluate(members, attempts, row.getTargetRate());
        return new TeamChallengeResponse(
                row.getId(),
                row.getAnalysisId(),
                row.getTitle(),
                row.getProjectName(),
                row.getStartDate(),
                row.getEndDate(),
                row.getTargetRate(),
                TeamChallengeCalculator.status(row.getStartDate(), row.getEndDate(), today),
                members.size(),
                r.solvedMemberCount(),
                r.achievedMemberCount(),
                r.achieved(),
                r.members(),
                row.getCreatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime());
    }

    private static int statusRank(TeamChallengeStatus status) {
        return switch (status) {
            case ONGOING -> 0;
            case UPCOMING -> 1;
            case ENDED -> 2;
        };
    }

    private static long dateKey(TeamChallengeResponse r) {
        return switch (r.status()) {
            case ONGOING -> r.endDate().toEpochDay();
            case UPCOMING -> r.startDate().toEpochDay();
            case ENDED -> -r.endDate().toEpochDay();
        };
    }

    /** 내 역할. 없는 팀이거나 내가 멤버가 아니면 TEAM_404. */
    private TeamRole requireRole(Team team, long teamId, long userId) {
        TeamRole role = team == null ? null : teamMapper.findRole(teamId, userId);
        if (role == null) {
            throw new BusinessException(ErrorCode.TEAM_404);
        }
        return role;
    }

    private void requireOwner(Team team, long teamId, long userId) {
        if (requireRole(team, teamId, userId) != TeamRole.OWNER) {
            throw new BusinessException(ErrorCode.TEAM_403);
        }
    }

    private record Period(LocalDate start, LocalDate end) {}

    /**
     * {@code @Valid}와 같은 제약과 날짜 규칙(형식, 오늘 ≤ 시작일 ≤ 오늘 + 365일, 종료일 ≥ 시작일, 양끝 포함 30일 이하)을 팀장 확인 뒤에
     * 검증하고, 필드별 첫 사유를 {@code COMMON_400} details로 던진다.
     */
    private Period validate(CreateTeamChallengeRequest request, LocalDate today) {
        Map<String, String> details = new TreeMap<>();
        validator
                .validate(request)
                .forEach(v -> details.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        LocalDate start = parseDate(request.startDate(), "startDate", "시작일", details);
        LocalDate end = parseDate(request.endDate(), "endDate", "종료일", details);
        if (start != null && start.isBefore(today)) {
            details.putIfAbsent("startDate", "시작일은 오늘이거나 그 뒤여야 합니다.");
        } else if (start != null && start.isAfter(today.plusDays(MAX_START_AHEAD_DAYS))) {
            details.putIfAbsent("startDate", "시작일은 오늘부터 " + MAX_START_AHEAD_DAYS + "일 안이어야 합니다.");
        }
        if (start != null && end != null) {
            if (end.isBefore(start)) {
                details.putIfAbsent("endDate", "종료일은 시작일이거나 그 뒤여야 합니다.");
            } else if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_DAYS) {
                details.putIfAbsent("endDate", "기간은 최대 " + MAX_DAYS + "일입니다.");
            }
        }
        if (!details.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_400, details);
        }
        return new Period(start, end);
    }

    private static LocalDate parseDate(
            String value, String field, String label, Map<String, String> details) {
        if (value == null || value.isBlank()) {
            details.putIfAbsent(field, label + "을 입력해 주세요.");
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            details.putIfAbsent(field, label + "은 YYYY-MM-DD 형식이어야 합니다.");
            return null;
        }
    }
}
