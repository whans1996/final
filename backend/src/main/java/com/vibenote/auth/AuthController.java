package com.vibenote.auth;

import com.vibenote.auth.dto.LoginRequest;
import com.vibenote.auth.dto.LoginResponse;
import com.vibenote.common.exception.BusinessException;
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
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
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
    private final AuthService authService;
    private final RefreshTokenCookie refreshTokenCookie;

    public AuthController(
            UserService userService,
            AuthService authService,
            RefreshTokenCookie refreshTokenCookie) {
        this.userService = userService;
        this.authService = authService;
        this.refreshTokenCookie = refreshTokenCookie;
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

    @Operation(
            summary = "로그인",
            description =
                    "Access 토큰과 회원 정보를 본문으로, Refresh 토큰을 HttpOnly 쿠키 refresh_token(Path=/api/auth,"
                            + " 14일)으로 준다.")
    @ApiResponse(responseCode = "200", description = "로그인 성공 (Set-Cookie: refresh_token)")
    @ApiResponse(
            responseCode = "400",
            description = "이메일·비밀번호 누락 (COMMON_400)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "이메일 없음·비밀번호 불일치·탈퇴 회원 (AUTH_001)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthService.IssuedTokens tokens = authService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.issue(tokens.refreshToken()));
        return tokens.body();
    }

    @Operation(
            summary = "토큰 재발급",
            description =
                    "refresh_token 쿠키로 새 Access 토큰을 받고, Refresh 토큰도 새로 바꾼다(회전). 앱 시작 시 세션 복원에도 쓴다.")
    @ApiResponse(responseCode = "200", description = "재발급 성공 (Set-Cookie: 새 refresh_token)")
    @ApiResponse(
            responseCode = "401",
            description = "쿠키 없음·무효·만료·이미 사용·탈퇴 회원 (AUTH_002, 쿠키 삭제)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/refresh")
    public LoginResponse refresh(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        try {
            AuthService.IssuedTokens tokens = authService.refresh(refreshToken);
            response.addHeader(
                    HttpHeaders.SET_COOKIE, refreshTokenCookie.issue(tokens.refreshToken()));
            return tokens.body();
        } catch (BusinessException e) {
            // 쓸 수 없는 쿠키는 브라우저에서도 지운다. 본문은 GlobalExceptionHandler가 만든다
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear());
            throw e;
        }
    }

    @Operation(summary = "로그아웃", description = "이 기기의 Refresh 토큰을 폐기하고 쿠키를 지운다. 쿠키가 없어도 204.")
    @ApiResponse(responseCode = "204", description = "로그아웃 완료")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear());
    }
}
