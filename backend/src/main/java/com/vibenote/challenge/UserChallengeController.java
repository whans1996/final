package com.vibenote.challenge;

import com.vibenote.challenge.dto.JoinedChallengeSearchRequest;
import com.vibenote.challenge.dto.MyChallengeResponse;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** 참여한 챌린지 목록 (F-25 AC10·AC11). 회원 확인(404)을 조건 검증(400)보다 먼저 하려고 조건은 Service가 검증한다. */
@Tag(name = "챌린지", description = "학습 챌린지 목록·상세, 참여·취소, 참여한 챌린지와 달성도")
@RestController
public class UserChallengeController {

    private final ChallengeService challengeService;

    public UserChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @Operation(
            summary = "내 챌린지",
            description = "내가 참여한 챌린지와 달성도를 참여 시각 최신순으로 페이지 조회한다. 상태가 없으면 전체다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "400",
            description = "상태·페이지 값 오류 (COMMON_400, details에 필드별 사유)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요, 만료·깨진 토큰 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/api/users/me/challenges")
    public PageResponse<MyChallengeResponse> listMine(
            @ParameterObject JoinedChallengeSearchRequest search,
            @AuthenticationPrincipal AuthUser user) {
        return challengeService.listMine(user.id(), search);
    }

    @Operation(
            summary = "회원의 참여 챌린지",
            description = "회원이 참여한 챌린지와 달성도를 참여 시각 최신순으로 페이지 조회한다. 상태가 없으면 전체다. 비로그인도 쓸 수 있다.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "400",
            description = "상태·페이지 값 오류 (COMMON_400, details에 필드별 사유)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없거나 탈퇴한 회원 (USER_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/api/users/{userId:\\d+}/challenges")
    public PageResponse<MyChallengeResponse> listByUser(
            @Parameter(description = "회원 ID", example = "3") @PathVariable long userId,
            @ParameterObject JoinedChallengeSearchRequest search) {
        return challengeService.listByUser(userId, search);
    }
}
