package com.vibenote.team;

import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.security.AuthUser;
import com.vibenote.team.dto.InviteCodeResponse;
import com.vibenote.team.dto.JoinTeamRequest;
import com.vibenote.team.dto.JoinTeamResponse;
import com.vibenote.team.dto.MyTeamResponse;
import com.vibenote.team.dto.TeamCreatedResponse;
import com.vibenote.team.dto.TeamDetailResponse;
import com.vibenote.team.dto.TeamRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 팀 (F-20a). 모든 엔드포인트는 로그인 필요. 판단 순서는 401 → 404 TEAM_404(없는 팀·내가 멤버가 아닌 팀) → 403 TEAM_403(팀장 전용) →
 * 400 검증이다.
 */
@Tag(name = "팀", description = "팀 생성, 초대 코드 가입, 멤버 관리 (F-20a), 분석 공유·팀 이해도 (F-20b)")
@RestController
public class TeamController {

    private static final String UNAUTHORIZED = "로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401)";
    private static final String NOT_FOUND = "없는 팀이거나 내가 멤버가 아닌 팀 (TEAM_404)";
    private static final String FORBIDDEN = "팀장이 아님 (TEAM_403)";

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @Operation(summary = "팀 만들기", description = "팀을 만들고 팀장이 된다. 초대 코드 8자를 함께 돌려준다.")
    @ApiResponse(responseCode = "201", description = "생성 성공")
    @ApiResponse(
            responseCode = "400",
            description = "이름·소개 형식 오류 (COMMON_400, details.name·description)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHORIZED,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "이미 5개 팀에 속함 (TEAM_003)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/api/teams")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamCreatedResponse createTeam(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody TeamRequest request) {
        return teamService.create(user.id(), request);
    }

    @Operation(
            summary = "내 팀 목록",
            description = "내가 속한 팀을 내가 가입한 시각 최신순(같으면 팀 id 내림차순)으로 전부 준다 (최대 5개).")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHORIZED,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/api/users/me/teams")
    public List<MyTeamResponse> fetchMyTeams(@AuthenticationPrincipal AuthUser user) {
        return teamService.myTeams(user.id());
    }

    @Operation(
            summary = "팀 상세",
            description = "팀 정보와 멤버(팀장이 맨 앞, 나머지는 가입 순)를 준다. 초대 코드는 팀장에게만 값이 있다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHORIZED,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = NOT_FOUND,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/api/teams/{id}")
    public TeamDetailResponse fetchTeam(
            @Parameter(description = "팀 ID", example = "12") @PathVariable long id,
            @AuthenticationPrincipal AuthUser user) {
        return teamService.getDetail(id, user.id());
    }

    @Operation(summary = "팀 정보 수정 (팀장)", description = "이름·소개를 보낸 값으로 통째로 바꾼다. 검증은 팀장 확인 뒤에 한다.")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(
            responseCode = "400",
            description = "이름·소개 형식 오류 (COMMON_400)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHORIZED,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "403",
            description = FORBIDDEN,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = NOT_FOUND,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/api/teams/{id}")
    public TeamDetailResponse updateTeam(
            @Parameter(description = "팀 ID", example = "12") @PathVariable long id,
            @AuthenticationPrincipal AuthUser user,
            @RequestBody TeamRequest request) {
        return teamService.update(id, user.id(), request);
    }

    @Operation(summary = "팀 삭제 (팀장)", description = "팀과 모든 멤버 정보를 지운다. 초대 코드도 무효가 된다.")
    @ApiResponse(responseCode = "204", description = "삭제 성공", content = @Content)
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHORIZED,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "403",
            description = FORBIDDEN,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = NOT_FOUND,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/api/teams/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeam(
            @Parameter(description = "팀 ID", example = "12") @PathVariable long id,
            @AuthenticationPrincipal AuthUser user) {
        teamService.delete(id, user.id());
    }

    @Operation(summary = "초대 코드 재발급 (팀장)", description = "새 코드를 발급한다. 이전 코드로는 더 가입할 수 없다.")
    @ApiResponse(responseCode = "200", description = "재발급 성공")
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHORIZED,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "403",
            description = FORBIDDEN,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = NOT_FOUND,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/api/teams/{id}/invite-code")
    public InviteCodeResponse regenerateInviteCode(
            @Parameter(description = "팀 ID", example = "12") @PathVariable long id,
            @AuthenticationPrincipal AuthUser user) {
        return teamService.regenerateInviteCode(id, user.id());
    }

    @Operation(
            summary = "초대 코드로 가입",
            description = "코드는 앞뒤 공백을 빼고 대문자로 바꿔 비교한다. 팀당 10명, 1인당 5개 팀까지다.")
    @ApiResponse(responseCode = "200", description = "가입 성공")
    @ApiResponse(
            responseCode = "400",
            description = "코드 없음·공백뿐 (COMMON_400, details.inviteCode)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHORIZED,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "유효하지 않은 초대 코드 (TEAM_001)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "이미 가입함 (TEAM_409), 팀 인원 가득 참 (TEAM_002), 이미 5개 팀에 속함 (TEAM_003)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/api/teams/join")
    public JoinTeamResponse joinTeam(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody JoinTeamRequest request) {
        return teamService.join(user.id(), request.inviteCode());
    }

    @Operation(summary = "팀 탈퇴 (팀원)", description = "내 멤버 행을 지운다. 팀장은 탈퇴할 수 없고 팀을 삭제해야 한다.")
    @ApiResponse(responseCode = "204", description = "탈퇴 성공", content = @Content)
    @ApiResponse(
            responseCode = "400",
            description = "팀장 (TEAM_004)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHORIZED,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = NOT_FOUND,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/api/teams/{id}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveTeam(
            @Parameter(description = "팀 ID", example = "12") @PathVariable long id,
            @AuthenticationPrincipal AuthUser user) {
        teamService.leave(id, user.id());
    }

    @Operation(summary = "멤버 내보내기 (팀장)", description = "팀원의 멤버 행을 지운다. 그 회원은 같은 코드로 다시 가입할 수 있다.")
    @ApiResponse(responseCode = "204", description = "내보내기 성공", content = @Content)
    @ApiResponse(
            responseCode = "400",
            description = "자기 자신 (TEAM_004)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = UNAUTHORIZED,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "403",
            description = FORBIDDEN,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = NOT_FOUND + ", 대상이 그 팀 멤버가 아님 (TEAM_005)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/api/teams/{id}/members/{userId:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTeamMember(
            @Parameter(description = "팀 ID", example = "12") @PathVariable long id,
            @Parameter(description = "내보낼 회원 ID", example = "7") @PathVariable long userId,
            @AuthenticationPrincipal AuthUser user) {
        teamService.removeMember(id, user.id(), userId);
    }
}
