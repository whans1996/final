package com.vibenote.follow;

import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import com.vibenote.common.security.AuthUser;
import com.vibenote.follow.dto.FollowStatusResponse;
import com.vibenote.follow.dto.FollowUserResponse;
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

/** 팔로우 (F-04). 팔로우·언팔로우는 로그인 필요, 상태와 팔로워·팔로잉 목록은 공개. */
@Tag(name = "팔로우", description = "회원 팔로우·언팔로우와 팔로워·팔로잉 조회 (F-04)")
@RestController
@RequestMapping("/api/users/{id}")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @Operation(summary = "팔로우", description = "회원을 팔로우한다. 이미 팔로우 중이어도 200(멱등)이다. 수는 대상 회원 기준이다.")
    @ApiResponse(responseCode = "200", description = "팔로우 상태")
    @ApiResponse(
            responseCode = "400",
            description = "자기 자신 (USER_002)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없거나 탈퇴한 회원 (USER_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/follow")
    public FollowStatusResponse follow(
            @Parameter(description = "팔로우할 회원 ID", example = "7") @PathVariable long id,
            @AuthenticationPrincipal AuthUser user) {
        return followService.follow(id, user.id());
    }

    @Operation(summary = "언팔로우", description = "팔로우를 푼다. 팔로우하지 않은 회원이어도 200(멱등)이다. 수는 대상 회원 기준이다.")
    @ApiResponse(responseCode = "200", description = "팔로우 상태")
    @ApiResponse(
            responseCode = "400",
            description = "자기 자신 (USER_002)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "로그인 필요, 만료·깨진 토큰, 탈퇴한 회원의 남은 토큰 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없거나 탈퇴한 회원 (USER_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/follow")
    public FollowStatusResponse unfollow(
            @Parameter(description = "언팔로우할 회원 ID", example = "7") @PathVariable long id,
            @AuthenticationPrincipal AuthUser user) {
        return followService.unfollow(id, user.id());
    }

    @Operation(
            summary = "팔로우 상태",
            description =
                    "회원의 팔로워·팔로잉 수와 로그인한 내가 팔로우 중인지 조회한다. 비로그인도 쓸 수 있고, 비로그인·내 프로필이면"
                            + " following은 false다. 토큰이 깨졌거나 만료되면 비로그인과 같다.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "404",
            description = "없거나 탈퇴한 회원 (USER_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/follow")
    public FollowStatusResponse status(
            @Parameter(description = "회원 ID", example = "7") @PathVariable long id,
            @AuthenticationPrincipal AuthUser user) {
        return followService.getStatus(id, user == null ? null : user.id());
    }

    @Operation(
            summary = "팔로워 목록",
            description = "회원을 팔로우하는 사람을 팔로우한 시각 최신순(같으면 회원 id 내림차순)으로 페이지 조회한다. 비로그인도 쓸 수 있다.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "400",
            description = "페이지 값 오류 (COMMON_400)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없거나 탈퇴한 회원 (USER_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/followers")
    public PageResponse<FollowUserResponse> followers(
            @Parameter(description = "회원 ID", example = "7") @PathVariable long id,
            @Valid @ParameterObject PageQuery page) {
        return followService.followers(id, page);
    }

    @Operation(
            summary = "팔로잉 목록",
            description = "회원이 팔로우하는 사람을 팔로우한 시각 최신순(같으면 회원 id 내림차순)으로 페이지 조회한다. 비로그인도 쓸 수 있다.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "400",
            description = "페이지 값 오류 (COMMON_400)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "없거나 탈퇴한 회원 (USER_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/followings")
    public PageResponse<FollowUserResponse> followings(
            @Parameter(description = "회원 ID", example = "7") @PathVariable long id,
            @Valid @ParameterObject PageQuery page) {
        return followService.followings(id, page);
    }
}
