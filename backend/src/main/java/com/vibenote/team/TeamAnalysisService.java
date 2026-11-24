package com.vibenote.team;

import com.vibenote.analysis.AnalysisStatus;
import com.vibenote.common.config.ClockConfig;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.team.TeamUnderstandingCalculator.Member;
import com.vibenote.team.TeamUnderstandingCalculator.Result;
import com.vibenote.team.domain.MemberAnswer;
import com.vibenote.team.domain.QuestionLayer;
import com.vibenote.team.domain.ShareTarget;
import com.vibenote.team.domain.Team;
import com.vibenote.team.domain.TeamAnalysis;
import com.vibenote.team.dto.ShareTeamAnalysisRequest;
import com.vibenote.team.dto.SharedByResponse;
import com.vibenote.team.dto.TeamAnalysisResponse;
import com.vibenote.team.dto.TeamAnalysisSharedResponse;
import com.vibenote.team.dto.TeamUnderstandingResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 팀 공유 분석과 팀 이해도 (F-20b). 멤버는 자기 DONE·퀴즈 있는 분석을 팀에 공유하고(팀당 최대 10개), 팀원은 그 퀴즈만 풀 수 있다 (권한은 {@link
 * TeamAnalysisAccess}).
 *
 * <p>판단 순서: 401(탈퇴 회원) → 404 TEAM_404(없는 팀·내가 멤버가 아닌 팀) → 400 검증 → 대상(404 ANALYSIS_404·TEAM_006) →
 * 403 TEAM_010 → 409.
 */
@Service
public class TeamAnalysisService {

    static final int MAX_SHARED = 10;

    private final TeamMapper teamMapper;
    private final UserService userService;
    private final Validator validator;
    private final Clock clock;

    public TeamAnalysisService(
            TeamMapper teamMapper, UserService userService, Validator validator, Clock clock) {
        this.teamMapper = teamMapper;
        this.userService = userService;
        this.validator = validator;
        this.clock = clock;
    }

    /**
     * 내 분석을 공유한다 (AC1·AC2). 잠금 순서는 {@link TeamService}와 같이 users → teams이고, 팀 행을 잠근 뒤 상한을 센다. 잠금보다
     * 앞에 일반 SELECT를 두지 않는다 (이유는 {@link TeamService#join} Javadoc).
     */
    @Transactional
    public TeamAnalysisSharedResponse share(
            long teamId, long userId, ShareTeamAnalysisRequest request) {
        userService.lockActiveMember(userId);
        Team team = teamMapper.findByIdForUpdate(teamId);
        requireRole(team, teamId, userId);
        validate(request);
        long analysisId = request.analysisId();
        ShareTarget target =
                teamMapper
                        .findShareTarget(analysisId)
                        .filter(t -> t.userId() == userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_404));
        if (target.status() != AnalysisStatus.DONE || !target.hasQuiz()) {
            throw new BusinessException(ErrorCode.TEAM_009);
        }
        if (teamMapper.existsTeamAnalysis(teamId, analysisId)) {
            throw new BusinessException(ErrorCode.TEAM_007);
        }
        if (teamMapper.countTeamAnalyses(teamId) >= MAX_SHARED) {
            throw new BusinessException(ErrorCode.TEAM_008);
        }
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        teamMapper.insertTeamAnalysis(teamId, analysisId, now);
        return new TeamAnalysisSharedResponse(
                analysisId, now.atZone(ClockConfig.ZONE).toOffsetDateTime());
    }

    /** 미삭제 공유 분석을 공유 최신순으로 (AC3). 공유한 사람이나 팀장이면 canUnshare. */
    @Transactional(readOnly = true)
    public List<TeamAnalysisResponse> list(long teamId, long userId) {
        userService.getActiveMember(userId);
        TeamRole role = requireRole(teamMapper.findById(teamId), teamId, userId);
        return teamMapper.findTeamAnalyses(teamId, userId).stream()
                .map(row -> TeamAnalysisResponse.of(row, canUnshare(row, role, userId)))
                .toList();
    }

    /** 공유 행만 지운다. 풀이 기록은 남는다 (AC4). */
    @Transactional
    public void unshare(long teamId, long userId, long analysisId) {
        userService.getActiveMember(userId);
        TeamRole role = requireRole(teamMapper.findById(teamId), teamId, userId);
        TeamAnalysis row = requireShared(teamId, analysisId, userId);
        if (!canUnshare(row, role, userId)) {
            throw new BusinessException(ErrorCode.TEAM_010);
        }
        teamMapper.deleteTeamAnalysis(teamId, analysisId);
    }

    /** 지금 멤버마다 가장 최근 풀이로 모듈별·멤버별 이해도를 계산한다 (AC6·AC7). */
    @Transactional(readOnly = true)
    public TeamUnderstandingResponse understanding(long teamId, long userId, long analysisId) {
        userService.getActiveMember(userId);
        requireRole(teamMapper.findById(teamId), teamId, userId);
        TeamAnalysis row = requireShared(teamId, analysisId, userId);
        List<Member> members =
                teamMapper.findMembers(teamId).stream()
                        .map(m -> new Member(m.getUserId(), m.getNickname(), m.getRole()))
                        .toList();
        Long quizId = row.getQuizId();
        List<QuestionLayer> questions =
                quizId == null ? List.of() : teamMapper.findQuestionLayers(quizId);
        List<MemberAnswer> answers =
                quizId == null ? List.of() : teamMapper.findLatestAnswers(teamId, quizId);
        Result result = TeamUnderstandingCalculator.calculate(questions, members, answers);
        return new TeamUnderstandingResponse(
                row.getAnalysisId(),
                row.getTitle(),
                row.getProjectName(),
                new SharedByResponse(row.getSharedByUserId(), row.getSharedByNickname()),
                questions.size(),
                members.size(),
                result.solvedMemberCount(),
                result.modules(),
                result.members());
    }

    private static boolean canUnshare(TeamAnalysis row, TeamRole role, long userId) {
        return role == TeamRole.OWNER || row.getSharedByUserId() == userId;
    }

    /** 내 역할. 없는 팀이거나 내가 멤버가 아니면 TEAM_404. */
    private TeamRole requireRole(Team team, long teamId, long userId) {
        TeamRole role = team == null ? null : teamMapper.findRole(teamId, userId);
        if (role == null) {
            throw new BusinessException(ErrorCode.TEAM_404);
        }
        return role;
    }

    private TeamAnalysis requireShared(long teamId, long analysisId, long userId) {
        TeamAnalysis row = teamMapper.findTeamAnalysis(teamId, analysisId, userId);
        if (row == null) {
            throw new BusinessException(ErrorCode.TEAM_006);
        }
        return row;
    }

    /** {@code @Valid}와 같은 제약을 멤버 확인 뒤에 검증하고 같은 형식({@code COMMON_400}, 필드별 details)으로 던진다. */
    private void validate(ShareTeamAnalysisRequest request) {
        Set<ConstraintViolation<ShareTeamAnalysisRequest>> violations = validator.validate(request);
        if (violations.isEmpty()) return;
        Map<String, String> details = new LinkedHashMap<>();
        violations.stream()
                .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .forEach(v -> details.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        throw new BusinessException(ErrorCode.COMMON_400, details);
    }
}
