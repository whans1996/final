package com.vibenote.user.dto;

import com.vibenote.common.Level;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 내 프로필 수정 요청 (F-03). 모든 필드는 선택이며 null이면 바꾸지 않는다.
 *
 * <p>nickname·bio는 검증 전에 trim한다. bio가 빈 문자열이면 소개를 지운다. level은 허용되지 않은 값도 {@code details.level}로 알려
 * 주려고 문자열로 받는다 (F-01과 같은 방식).
 */
@Schema(description = "내 프로필 수정 요청. 보낸 필드만 바뀐다")
public record UpdateProfileRequest(
        @Schema(description = "닉네임. 2~20자의 한글·영문·숫자·_", example = "주니어개발자", nullable = true)
                @Pattern(
                        regexp = SignupRequest.NICKNAME_PATTERN,
                        message = SignupRequest.NICKNAME_MESSAGE)
                String nickname,
        @Schema(
                        description = "소개 (300자 이하). 빈 문자열이면 지운다",
                        example = "AI로 만든 Spring 프로젝트를 공부하고 있어요.",
                        nullable = true)
                @Size(max = 300, message = BIO_MESSAGE)
                String bio,
        @Schema(
                        description = "학습 수준",
                        example = "JUNIOR",
                        nullable = true,
                        allowableValues = {"BEGINNER", "JUNIOR", "SENIOR"})
                @Pattern(regexp = "BEGINNER|JUNIOR|SENIOR", message = SignupRequest.LEVEL_MESSAGE)
                String level) {

    public static final String BIO_MESSAGE = "소개는 300자 이하로 입력하세요.";

    public UpdateProfileRequest {
        nickname = nickname == null ? null : nickname.trim();
        bio = bio == null ? null : bio.trim();
    }

    /** 바꿀 수준. 보내지 않았으면 null. */
    public Level levelValue() {
        return level == null ? null : Level.valueOf(level);
    }
}
