package com.vibenote.auth;

import com.vibenote.common.exception.ErrorResponse;
import com.vibenote.user.UserService;
import com.vibenote.user.dto.SignupRequest;
import com.vibenote.user.dto.SignupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 회원가입(F-01), 로그인·재발급·로그아웃(F-02). 모두 공개 API다. */
@Tag(name = "인증", description = "회원가입, 로그인, 토큰 재발급, 로그아웃")
@RestController
@RequestMapping("/api/auth")
@SecurityRequirements
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "회원가입", description = "이메일·비밀번호·닉네임·학습 수준으로 가입한다. 로그인은 따로 호출한다.")
    @ApiResponse(responseCode = "201", description = "가입 성공")
    @ApiResponse(
            responseCode = "400",
            description = "검증 실패 (COMMON_400, details에 필드별 사유)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "이메일 또는 닉네임 중복 (USER_409, details에 해당 필드)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
        return new SignupResponse(userService.signup(request));
    }
}
