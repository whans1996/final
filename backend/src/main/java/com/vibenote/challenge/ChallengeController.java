package com.vibenote.challenge;

import com.vibenote.challenge.dto.ChallengeDetailResponse;
import com.vibenote.challenge.dto.ChallengeSearchRequest;
import com.vibenote.challenge.dto.ChallengeSummaryResponse;
import com.vibenote.challenge.dto.ParticipationResponse;
import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.page.PageResponse;
import com.vibenote.common.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 학습 챌린지 목록·상세(공개)와 참여·취소 (F-25). */
@Tag(name = "챌린지", description = "학습 챌린지 목록·상세, 참여·취소, 참여한 챌린지와 달성도")
@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {

    private final ChallengeService challengeService;

    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @Operation(
            summary = "챌린지 목록",
            description =
                    "상태(오늘 기준, 기본 진행 중)로 거른다. 진행 중은 종료일 오름차순, 예정은 시작일 오름차순, 종료는 종료일 내림차순이고 모두 다음"
                            + " 기준 id 역순이다. 비로그인도 쓸 수 있고, 로그인했으면 항목마다 참여 여부(joined)를 채운다.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "400",
            description = "상태·페이지 값 오류 (COMMON_400, details에 필드별 사유)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public PageResponse<ChallengeSummaryResponse> list(
            @Valid @ParameterObject ChallengeSearchRequest search,
            @AuthenticationPrincipal AuthUser user) {
        return challengeService.search(search, user == null ? null : user.id());
    }

    @Operation(
            summary = "챌린지 상세",
            description = "설명·상태·참여자 수와, 로그인해 참여한 회원이면 내 달성도(participation)를 준다. 비로그인도 쓸 수 있다.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "404",
            description = "없는 챌린지 (CHALLENGE_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public ChallengeDetailResponse detail(
            @Parameter(description = "챌린지 ID", example = "7") @PathVariable long id,
            @AuthenticationPrincipal AuthUser user) {
        return challengeService.getDetail(id, user == null ? null : user.id());
    }

    @Operation(
            summary = "챌린지 참여",
            description =
                    "예정·진행 중 챌린지에 참여한다. 이미 참여했어도 200(멱등)이다. 처음 참여하면 피드 활동을 남기고, 시작일부터의 활동을 바로 세어"
                            + " 목표에 닿았으면 달성 처리한다.")
    @ApiResponse(responseCode = "200", description = "참여 상태")
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 챌린지 (CHALLENGE_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "종료된 챌린지 (CHALLENGE_002)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{id}/participation")
    public ParticipationResponse join(
            @Parameter(description = "챌린지 ID", example = "7") @PathVariable long id,
            @AuthenticationPrincipal AuthUser user) {
        return challengeService.join(id, user.id());
    }

    @Operation(
            summary = "챌린지 참여 취소",
            description = "참여를 취소하고 피드의 참여 활동을 지운다. 참여하지 않았어도 200(멱등)이다. 달성했거나 종료된 챌린지는 취소할 수 없다.")
    @ApiResponse(responseCode = "200", description = "참여 상태 (joined false)")
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없는 챌린지 (CHALLENGE_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "종료된 챌린지 (CHALLENGE_002), 달성한 챌린지 (CHALLENGE_003)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}/participation")
    public ParticipationResponse cancel(
            @Parameter(description = "챌린지 ID", example = "7") @PathVariable long id,
            @AuthenticationPrincipal AuthUser user) {
        return challengeService.cancel(id, user.id());
    }
}
