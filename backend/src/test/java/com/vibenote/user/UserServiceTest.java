package com.vibenote.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.user.domain.User;
import com.vibenote.user.dto.SignupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

/** F-01 AC6: 중복 확인을 통과한 뒤 동시 가입으로 DB UNIQUE 위반이 나도 409로 응답한다. */
class UserServiceTest {

    private UserMapper userMapper;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        given(encoder.encode(anyString())).willReturn("hashed");
        userService = new UserService(userMapper, encoder);
    }

    private static SignupRequest request() {
        return new SignupRequest("user@example.com", "vibe1234", "코딩초보", "BEGINNER");
    }

    @Test
    void AC6_이메일_UNIQUE_위반이면_USER_409와_email_사유() {
        willThrow(
                        new DuplicateKeyException(
                                "Duplicate entry 'user@example.com' for key"
                                        + " 'users.uk_users_email'"))
                .given(userMapper)
                .insert(any(User.class));

        BusinessException e =
                catchThrowableOfType(BusinessException.class, () -> userService.signup(request()));

        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_409);
        assertThat(e.getDetails()).containsOnlyKeys("email");
    }

    @Test
    void AC6_닉네임_UNIQUE_위반이면_USER_409와_nickname_사유() {
        willThrow(
                        new DuplicateKeyException(
                                "Duplicate entry '코딩초보' for key 'users.uk_users_nickname'"))
                .given(userMapper)
                .insert(any(User.class));

        BusinessException e =
                catchThrowableOfType(BusinessException.class, () -> userService.signup(request()));

        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_409);
        assertThat(e.getDetails()).containsOnlyKeys("nickname");
    }
}
