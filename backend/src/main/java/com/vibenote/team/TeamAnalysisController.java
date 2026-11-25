package com.vibenote.team;

import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.security.AuthUser;
import com.vibenote.team.dto.ShareTeamAnalysisRequest;
import com.vibenote.team.dto.TeamAnalysisResponse;
import com.vibenote.team.dto.TeamAnalysisSharedResponse;
import com.vibenote.team.dto.TeamUnderstandingResponse;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 팀 공유 분석과 팀 이해도 (F-20b). 모든 엔드포인트는 로그인 필요. 판단 순서는 401 → 404 TEAM_404 → 400 검증 → 대상(404
 * ANALYSIS_404·TEAM_006) → 403 TEAM_010 → 409다. 본문 검증은 멤버 확인 뒤 Service가 한다.
 */
@Tag(name = TeamApiDocs.TAG, description = TeamApiDocs.TAG_DESCRIPTION)
@RestController
public class TeamAnalysisController {

    private static final String UNAUTHORIZED = "로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401)";
    private static final String TEAM_NOT_FOUND = "없는 팀이거나 내가 멤버가 아닌 팀 (TEAM_404)";

    private final TeamAnalysisService teamAnalysisService;

    public TeamAnalysisController(TeamAnalysisService teamAnalysisService) {
        this.teamAnalysisService = teamAnalysisService;
    }

    @Operation(
            summary = "팀에 분석 공유",
            description = "내 완료(DONE)·퀴즈 있는 분석을 팀에 공유한다. 팀원은 그 퀴즈만 풀 수 있다. 팀당 최대 10개.")
    @ApiResponse(responseCode = "201", description = "공유 성공")
    @ApiResponse(
            responseCode = "400",
            description = "analysisId 없음·1 미만 (COMMON_400, details.analysisId)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHORIZED,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = TEAM_NOT_FOUND + ", 없는·남의·삭제된 분석 (ANALYSIS_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "DONE 아님·퀴즈 없음 (TEAM_009), 이미 공유 (TEAM_007), 공유 10개 (TEAM_008)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/api/teams/{id}/analyses")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamAnalysisSharedResponse shareTeamAnalysis(
            @Parameter(description = "팀 ID", example = "12") @PathVariable long id,
            @AuthenticationPrincipal AuthUser user,
            @RequestBody ShareTeamAnalysisRequest request) {
        return teamAnalysisService.share(id, user.id(), request);
    }

    @Operation(
            summary = "팀 공유 분석 목록",
            description =
                    "삭제되지 않은 공유 분석을 공유 최신순으로 전부 준다 (최대 10개). solvedMemberCount는 지금 멤버 중 푼 사람 수,"
                            + " myLastAttempt는 내 최근 풀이다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHORIZED,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = TEAM_NOT_FOUND,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/api/teams/{id}/analyses")
    public List<TeamAnalysisResponse> fetchTeamAnalyses(
            @Parameter(description = "팀 ID", example = "12") @PathVariable long id,
            @AuthenticationPrincipal AuthUser user) {
        return teamAnalysisService.list(id, user.id());
    }

    @Operation(summary = "팀 분석 공유 해제 (공유한 사람·팀장)", description = "공유만 해제한다. 팀원의 풀이 기록은 남는다.")
    @ApiResponse(responseCode = "204", description = "해제 성공")
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHORIZED,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "403",
            description = "공유한 사람도 팀장도 아님 (TEAM_010)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = TEAM_NOT_FOUND + ", 팀에 공유되지 않았거나 삭제된 분석 (TEAM_006)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/api/teams/{id}/analyses/{analysisId:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unshareTeamAnalysis(
            @Parameter(description = "팀 ID", example = "12") @PathVariable long id,
            @Parameter(description = "분석 ID", example = "42") @PathVariable long analysisId,
            @AuthenticationPrincipal AuthUser user) {
        teamAnalysisService.unshare(id, user.id(), analysisId);
    }

    @Operation(
            summary = "팀 이해도 (모듈별·멤버별)",
            description =
                    "지금 멤버마다 가장 최근 풀이로 레이어(모듈)별 정답률과 멤버 × 모듈 결과를 준다. 모듈 = 문항 개념이 연결된 파일의"
                            + " 레이어(없으면 other).")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHORIZED,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = TEAM_NOT_FOUND + ", 팀에 공유되지 않았거나 삭제된 분석 (TEAM_006)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/api/teams/{id}/analyses/{analysisId:\\d+}/understanding")
    public TeamUnderstandingResponse fetchTeamUnderstanding(
            @Parameter(description = "팀 ID", example = "12") @PathVariable long id,
            @Parameter(description = "분석 ID", example = "42") @PathVariable long analysisId,
            @AuthenticationPrincipal AuthUser user) {
        return teamAnalysisService.understanding(id, user.id(), analysisId);
    }
}
