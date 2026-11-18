package com.vibenote.user;

import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.common.security.AuthUser;
import com.vibenote.user.dto.MyProfileResponse;
import com.vibenote.user.dto.UpdateProfileRequest;
import com.vibenote.user.dto.UserProfileResponse;
import com.vibenote.user.dto.UserStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 내 프로필 조회·수정, 공개 프로필·학습 통계 (F-03). 공개 GET은 SecurityConfig.PUBLIC_GET_PATHS에 있다. */
@Tag(name = "회원", description = "프로필 조회·수정, 학습 통계, 회원 탈퇴")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserStatsService userStatsService;

    public UserController(UserService userService, UserStatsService userStatsService) {
        this.userService = userService;
        this.userStatsService = userStatsService;
    }

    @Operation(summary = "내 프로필 조회", description = "email·role을 포함한 내 정보를 준다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료·탈퇴 회원 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/me")
    public MyProfileResponse getMe(@AuthenticationPrincipal AuthUser user) {
        return userService.getMe(user.id());
    }

    @Operation(
            summary = "내 프로필 수정",
            description = "닉네임·소개·학습 수준 중 보낸 필드만 바꾼다. null·생략은 그대로, 소개를 빈 문자열로 보내면 지운다.")
    @ApiResponse(responseCode = "200", description = "수정 성공. 수정된 내 프로필")
    @ApiResponse(
            responseCode = "400",
            description = "검증 실패 (COMMON_400, details에 필드별 사유)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "토큰 없음·만료·탈퇴 회원 (AUTH_401)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "다른 회원이 쓰는 닉네임 (USER_409, details.nickname)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/me")
    public MyProfileResponse updateMe(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateMe(user.id(), request);
    }

    @Operation(summary = "공개 프로필 조회", description = "로그인 없이 볼 수 있다. email·role은 주지 않는다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "404",
            description = "없는 회원 또는 탈퇴 회원 (USER_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @SecurityRequirements
    @GetMapping("/{id:\\d+}")
    public UserProfileResponse getProfile(@PathVariable long id) {
        return userService.getPublicProfile(id);
    }

    @Operation(
            summary = "학습 통계",
            description =
                    "완료한 분석 수(삭제 제외), 학습한 카드 수(퀴즈 정답 카드 ∪ 복습 완료 카드), 퀴즈 정답률(%, 풀이가 없으면 null). 로그인"
                            + " 없이 볼 수 있다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "404",
            description = "없는 회원 또는 탈퇴 회원 (USER_404)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @SecurityRequirements
    @GetMapping("/{id:\\d+}/stats")
    public UserStatsResponse getStats(@PathVariable long id) {
        return userStatsService.getStats(id);
    }
}
