package com.vibenote.user.dto;

import com.vibenote.common.Level;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/**
 * 회원가입 요청 (F-01). 검증 전에 email은 trim·소문자, nickname은 trim으로 정규화한다.
 *
 * <p>level은 허용되지 않은 값도 {@code details.level}로 알려 주려고 문자열로 받는다 (enum이면 본문 파싱 자체가 실패한다).
 */
@Schema(description = "회원가입 요청")
public record SignupRequest(
        @Schema(description = "이메일 (앞뒤 공백 제거, 소문자로 저장)", example = "user@example.com")
                @NotBlank(message = EMAIL_MESSAGE)
                @Email(message = EMAIL_MESSAGE)
                @Size(max = 255, message = EMAIL_MESSAGE)
                String email,
        @Schema(description = "비밀번호. 8~64자, 영문과 숫자를 각각 1자 이상", example = "vibe1234")
                @NotNull(message = PASSWORD_MESSAGE)
                @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
                String password,
        @Schema(description = "닉네임. 2~20자의 한글·영문·숫자·_", example = "코딩초보")
                @NotNull(message = NICKNAME_MESSAGE)
                @Pattern(regexp = NICKNAME_PATTERN, message = NICKNAME_MESSAGE)
                String nickname,
        @Schema(
                        description = "학습 수준",
                        example = "BEGINNER",
                        allowableValues = {"BEGINNER", "JUNIOR", "SENIOR"})
                @NotNull(message = LEVEL_MESSAGE)
                @Pattern(regexp = "BEGINNER|JUNIOR|SENIOR", message = LEVEL_MESSAGE)
                String level) {

    public static final String EMAIL_MESSAGE = "올바른 이메일 형식이 아닙니다.";
    public static final String PASSWORD_MESSAGE = "비밀번호는 8~64자이며 영문과 숫자를 모두 포함해야 합니다.";
    public static final String NICKNAME_MESSAGE = "닉네임은 2~20자의 한글·영문·숫자·_만 사용할 수 있습니다.";
    public static final String LEVEL_MESSAGE = "학습 수준을 선택하세요.";

    static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";
    static final String NICKNAME_PATTERN = "^[가-힣A-Za-z0-9_]{2,20}$";

    public SignupRequest {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        nickname = nickname == null ? null : nickname.trim();
    }

    public Level levelValue() {
        return Level.valueOf(level);
    }
}
