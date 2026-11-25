package com.vibenote.team;

import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.security.AuthUser;
import com.vibenote.team.dto.CreateTeamChallengeRequest;
import com.vibenote.team.dto.TeamChallengeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 팀 기간 목표 (F-20c). 모든 엔드포인트는 로그인 필요. 판단 순서는 401 → 404 TEAM_404 → 403 TEAM_403 → 400 검증 → 대상(404
 * TEAM_006·TEAM_013) → 409다. 본문 검증은 팀장 확인 뒤 Service가 한다.
 */
@Tag(name = TeamApiDocs.TAG, description = TeamApiDocs.TAG_DESCRIPTION)
@RestController
public class TeamChallengeController {

    private static final String UNAUTHORIZED = "로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401)";
    private static final String TEAM_NOT_FOUND = "없는 팀이거나 내가 멤버가 아닌 팀 (TEAM_404)";

    private final TeamChallengeService teamChallengeService;

    public TeamChallengeController(TeamChallengeService teamChallengeService) {
        this.teamChallengeService = teamChallengeService;
    }

    @Operation(
            summary = "팀 목표 만들기 (팀장)",
            description =
                    "공유 분석 하나에 기간(Asia/Seoul 날짜, 양끝 포함, 최대 30일)과 목표 정답률을 정한다. 분석마다 끝나지 않은 목표는"
                            + " 하나, 팀당 목표는 최대 20개.")
    @ApiResponse(responseCode = "201", description = "만들기 성공")
    @ApiResponse(
            responseCode = "400",
            description =
                    "analysisId·startDate·endDate·targetRate 오류, 시작일이 오늘 전이거나 오늘 + 365일 뒤, 종료일이 시작일"
                            + " 전, 30일 초과 (COMMON_400, 필드별 details)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHORIZED,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "403",
            description = "팀장이 아님 (TEAM_403)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = TEAM_NOT_FOUND + ", 팀에 공유되지 않았거나 삭제된 분석 (TEAM_006)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "끝나지 않은 목표가 이미 있음 (TEAM_011), 목표 20개 (TEAM_012)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/api/teams/{id}/challenges")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamChallengeResponse createTeamChallenge(
            @Parameter(description = "팀 ID", example = "12") @PathVariable long id,
            @AuthenticationPrincipal AuthUser user,
            @RequestBody CreateTeamChallengeRequest request) {
        return teamChallengeService.create(id, user.id(), request);
    }

    @Operation(
            summary = "팀 목표 목록",
            description =
                    "진행 중(종료일 ASC) → 예정(시작일 ASC) → 종료(종료일 DESC), 같으면 목표 id DESC. 달성은 지금 멤버마다 기간 안"
                            + " 가장 최근 풀이로 조회할 때 계산한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "400",
            description = "analysisId가 숫자가 아니거나 1 미만 (COMMON_400)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHORIZED,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = TEAM_NOT_FOUND,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/api/teams/{id}/challenges")
    public List<TeamChallengeResponse> fetchTeamChallenges(
            @Parameter(description = "팀 ID", example = "12") @PathVariable long id,
            @Parameter(description = "이 분석의 목표만 (선택)", example = "42")
                    @RequestParam(required = false)
                    Long analysisId,
            @AuthenticationPrincipal AuthUser user) {
        return teamChallengeService.list(id, user.id(), analysisId);
    }

    @Operation(summary = "팀 목표 삭제 (팀장)", description = "상태와 상관없이 지운다. 풀이 기록은 남는다.")
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHORIZED,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "403",
            description = "팀장이 아님 (TEAM_403)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = TEAM_NOT_FOUND + ", 없는·다른 팀의·삭제된 분석의 목표 (TEAM_013)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/api/teams/{id}/challenges/{challengeId:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeamChallenge(
            @Parameter(description = "팀 ID", example = "12") @PathVariable long id,
            @Parameter(description = "목표 ID", example = "7") @PathVariable long challengeId,
            @AuthenticationPrincipal AuthUser user) {
        teamChallengeService.delete(id, user.id(), challengeId);
    }
}
