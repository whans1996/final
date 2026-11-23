package com.vibenote.team;

import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.team.domain.Team;
import com.vibenote.team.dto.InviteCodeResponse;
import com.vibenote.team.dto.JoinTeamResponse;
import com.vibenote.team.dto.MyTeamResponse;
import com.vibenote.team.dto.TeamCreatedResponse;
import com.vibenote.team.dto.TeamDetailResponse;
import com.vibenote.team.dto.TeamMemberResponse;
import com.vibenote.team.dto.TeamRequest;
import com.vibenote.user.UserService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 팀 (F-20a). 팀당 최대 10명, 1인당 최대 5개 팀, 역할은 OWNER(팀당 1명, 위임 없음)·MEMBER다.
 *
 * <p>판단 순서는 모든 API에서 401(탈퇴 회원) → 404 TEAM_404(없는 팀·내가 멤버가 아닌 팀) → 403 TEAM_403(팀장 전용) → 400 검증이다.
 *
 * <p>잠금 순서는 users → teams로 고정한다. 만들기·가입은 내 users 행을 배타 잠금한 뒤(같은 회원의 요청이 겹치지 않음) 팀 행을 잠그고, 회원 탈퇴도
 * users 행을 먼저 잠근 뒤 팀을 정리하므로 서로 엇갈리지 않는다. 팀을 바꾸는 요청은 teams 행을 배타 잠금한 뒤 멤버를 보므로 가입의 "인원 세기 → 넣기"와 겹치지
 * 않는다 (AC6).
 */
@Service
public class TeamService {

    static final int MAX_MEMBERS = 10;
    static final int MAX_TEAMS_PER_USER = 5;

    /** 생성한 코드가 이미 있으면 새로 뽑아 다시 시도하는 횟수 (AC1). 처음 시도를 빼고 센다. */
    static final int CODE_RETRIES = 3;

    private final TeamMapper teamMapper;
    private final UserService userService;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final Validator validator;
    private final Clock clock;

    public TeamService(
            TeamMapper teamMapper,
            UserService userService,
            InviteCodeGenerator inviteCodeGenerator,
            Validator validator,
            Clock clock) {
        this.teamMapper = teamMapper;
        this.userService = userService;
        this.inviteCodeGenerator = inviteCodeGenerator;
        this.validator = validator;
        this.clock = clock;
    }

    /** 본문 형식은 Controller의 {@code @Valid}가 이미 검증했다. 팀과 팀장 멤버 행을 같은 트랜잭션에서 만든다 (AC1·AC6). */
    @Transactional
    public TeamCreatedResponse create(long userId, TeamRequest request) {
        userService.lockActiveMember(userId);
        if (teamMapper.countTeamsOf(userId) >= MAX_TEAMS_PER_USER) {
            throw new BusinessException(ErrorCode.TEAM_003);
        }
        LocalDateTime now = now();
        Team team = new Team(request.name(), request.description(), userId, null, now);
        withFreshCode(
                code -> {
                    team.setInviteCode(code);
                    teamMapper.insertTeam(team);
                });
        teamMapper.insertMember(team.getId(), userId, TeamRole.OWNER, now);
        return new TeamCreatedResponse(team.getId(), team.getInviteCode());
    }

    /** 내가 가입한 시각 최신순 (AC3). 1인당 최대 5개라 페이지 없이 전부 준다. */
    @Transactional(readOnly = true)
    public List<MyTeamResponse> myTeams(long userId) {
        userService.getActiveMember(userId);
        return teamMapper.findMyTeams(userId).stream().map(MyTeamResponse::from).toList();
    }

    /** 멤버만 본다. 초대 코드는 팀장에게만 준다 (AC4). */
    @Transactional(readOnly = true)
    public TeamDetailResponse getDetail(long teamId, long userId) {
        userService.getActiveMember(userId);
        Team team = teamMapper.findById(teamId);
        TeamRole role = team == null ? null : teamMapper.findRole(teamId, userId);
        if (role == null) {
            throw new BusinessException(ErrorCode.TEAM_404);
        }
        return detailOf(team, role);
    }

    /** 이름·소개를 보낸 값으로 통째로 바꾼다. 팀원이 잘못된 본문을 보내도 403이 먼저라 검증을 팀장 확인 뒤 여기서 한다 (AC7). */
    @Transactional
    public TeamDetailResponse update(long teamId, long userId, TeamRequest request) {
        userService.getActiveMember(userId);
        lockAsOwner(teamId, userId);
        validate(request);
        teamMapper.updateInfo(teamId, request.name(), request.description());
        return detailOf(teamMapper.findById(teamId), TeamRole.OWNER);
    }

    /** 팀 행을 지우면 멤버 행은 FK CASCADE로 지워지고 코드도 무효가 된다 (AC9). */
    @Transactional
    public void delete(long teamId, long userId) {
        userService.getActiveMember(userId);
        lockAsOwner(teamId, userId);
        teamMapper.deleteTeam(teamId);
    }

    /** 새 코드로 바꾼다. 이전 코드는 바로 무효다 (AC7). */
    @Transactional
    public InviteCodeResponse regenerateInviteCode(long teamId, long userId) {
        userService.getActiveMember(userId);
        lockAsOwner(teamId, userId);
        String code = withFreshCode(c -> teamMapper.updateInviteCode(teamId, c));
        return new InviteCodeResponse(code);
    }

    /**
     * 코드(이미 앞뒤 공백 제거·대문자)로 가입한다 (AC5·AC6). 팀 행을 배타 잠금한 뒤 인원을 세므로 9명인 팀에 둘이 동시에 가입해도 한 명만 들어간다. 이미
     * 멤버면 TEAM_409, 내가 5개 팀에 속했으면 TEAM_003, 팀이 10명이면 TEAM_002 순으로 본다.
     *
     * <p><b>잠금보다 앞에 일반 SELECT를 두지 않는다.</b> REPEATABLE READ에서는 트랜잭션의 첫 일반 SELECT가 읽기 시점(read view)을
     * 정한다. 지금은 두 잠금(users·teams FOR UPDATE) 뒤의 {@code findRole}이 첫 일반 SELECT라 {@code countMembers}가
     * 앞선 가입이 커밋된 뒤의 인원을 센다. 잠금 앞에 조회를 넣거나 {@code lockActiveMember}를 {@code getActiveMember}로 바꾸면 예전
     * 스냅샷을 세어 11명이 될 수 있다. {@code COUNT … FOR SHARE}로 바꾸지 않은 것은 탈퇴의 {@code deleteMembershipsOf}가
     * teams 잠금 없이 team_members를 지우므로 공유 잠금이 늘면 교착 가능성이 생기기 때문이다.
     */
    @Transactional
    public JoinTeamResponse join(long userId, String inviteCode) {
        userService.lockActiveMember(userId);
        Team team = teamMapper.findByInviteCodeForUpdate(inviteCode);
        if (team == null) {
            throw new BusinessException(ErrorCode.TEAM_001);
        }
        long teamId = team.getId();
        if (teamMapper.findRole(teamId, userId) != null) {
            throw new BusinessException(ErrorCode.TEAM_409);
        }
        if (teamMapper.countTeamsOf(userId) >= MAX_TEAMS_PER_USER) {
            throw new BusinessException(ErrorCode.TEAM_003);
        }
        if (teamMapper.countMembers(teamId) >= MAX_MEMBERS) {
            throw new BusinessException(ErrorCode.TEAM_002);
        }
        teamMapper.insertMember(teamId, userId, TeamRole.MEMBER, now());
        return new JoinTeamResponse(teamId);
    }

    /** 팀원이 스스로 떠난다. 팀장은 팀을 삭제해야 한다 (AC8). 내가 이 팀에 공유한 분석도 해제된다 (F-20b AC8). */
    @Transactional
    public void leave(long teamId, long userId) {
        userService.getActiveMember(userId);
        if (lockMembership(teamId, userId) == TeamRole.OWNER) {
            throw new BusinessException(ErrorCode.TEAM_004);
        }
        teamMapper.deleteMember(teamId, userId);
        teamMapper.deleteTeamAnalysesSharedBy(teamId, userId);
    }

    /**
     * 팀장이 팀원을 내보낸다. 자기 자신이면 TEAM_004, 대상이 그 팀 멤버가 아니면 TEAM_005 (AC8). 대상이 이 팀에 공유한 분석도 해제된다 (F-20b
     * AC8).
     */
    @Transactional
    public void removeMember(long teamId, long ownerId, long targetId) {
        userService.getActiveMember(ownerId);
        lockAsOwner(teamId, ownerId);
        if (targetId == ownerId) {
            throw new BusinessException(ErrorCode.TEAM_004);
        }
        if (teamMapper.deleteMember(teamId, targetId) == 0) {
            throw new BusinessException(ErrorCode.TEAM_005);
        }
        teamMapper.deleteTeamAnalysesSharedBy(teamId, targetId);
    }

    /**
     * 회원 탈퇴(F-03)의 트랜잭션 안에서 부른다: 내가 모든 팀에 공유한 분석을 해제하고(F-20b AC8), 팀장인 팀을 지우고(멤버·공유 행 CASCADE) 남은
     * 멤버 행을 지운다 (AC10).
     */
    @Transactional
    public void removeAllOf(long userId) {
        teamMapper.deleteAllTeamAnalysesSharedBy(userId);
        teamMapper.deleteTeamsOwnedBy(userId);
        teamMapper.deleteMembershipsOf(userId);
    }

    /** 팀 행을 잠그고 내 역할을 돌려준다. 없는 팀이거나 내가 멤버가 아니면 TEAM_404. */
    private TeamRole lockMembership(long teamId, long userId) {
        Team team = teamMapper.findByIdForUpdate(teamId);
        TeamRole role = team == null ? null : teamMapper.findRole(teamId, userId);
        if (role == null) {
            throw new BusinessException(ErrorCode.TEAM_404);
        }
        return role;
    }

    private void lockAsOwner(long teamId, long userId) {
        if (lockMembership(teamId, userId) != TeamRole.OWNER) {
            throw new BusinessException(ErrorCode.TEAM_403);
        }
    }

    /**
     * 새 코드로 저장을 시도하고, 코드가 이미 있으면(uk_teams_invite_code 위반) 새로 뽑아 최대 {@link #CODE_RETRIES}회 다시 시도한다.
     * MySQL은 실패한 문장만 되돌리므로 같은 트랜잭션에서 다시 시도할 수 있다. 다 실패하면 마지막 예외를 그대로 던진다 (500).
     */
    private String withFreshCode(Consumer<String> save) {
        for (int attempt = 0; ; attempt++) {
            String code = inviteCodeGenerator.generate();
            try {
                save.accept(code);
                return code;
            } catch (DuplicateKeyException e) {
                if (attempt >= CODE_RETRIES || !isInviteCodeConflict(e)) {
                    throw e;
                }
            }
        }
    }

    private static boolean isInviteCodeConflict(DuplicateKeyException e) {
        return String.valueOf(e.getMessage()).contains("uk_teams_invite_code");
    }

    private TeamDetailResponse detailOf(Team team, TeamRole myRole) {
        List<TeamMemberResponse> members =
                teamMapper.findMembers(team.getId()).stream()
                        .map(TeamMemberResponse::from)
                        .toList();
        return TeamDetailResponse.of(team, myRole, MAX_MEMBERS, members);
    }

    /** {@code @Valid}와 같은 제약을 직접 검증하고 같은 형식({@code COMMON_400}, 필드별 details)으로 던진다. */
    private void validate(TeamRequest request) {
        Set<ConstraintViolation<TeamRequest>> violations = validator.validate(request);
        if (violations.isEmpty()) return;
        Map<String, String> details = new LinkedHashMap<>();
        violations.stream()
                .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .forEach(v -> details.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        throw new BusinessException(ErrorCode.COMMON_400, details);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
