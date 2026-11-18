package com.vibenote.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import com.vibenote.common.Level;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.user.domain.User;
import com.vibenote.user.dto.SignupRequest;
import com.vibenote.user.dto.UpdateProfileRequest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

/** F-01 AC6·F-03 AC5: 중복 확인을 통과한 뒤 동시 저장으로 DB UNIQUE 위반이 나도 409로 응답한다. */
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

    @Test
    void AC5_수정_저장_시_닉네임_UNIQUE_위반이면_USER_409와_nickname_사유() {
        // F-03: 중복 확인을 통과한 뒤 다른 회원이 같은 닉네임을 먼저 저장한 경우
        User user = User.newMember("user@example.com", "hashed", "코딩초보", Level.BEGINNER);
        user.setId(1L);
        given(userMapper.findById(1L)).willReturn(Optional.of(user));
        willThrow(
                        new DuplicateKeyException(
                                "Duplicate entry '주니어' for key 'users.uk_users_nickname'"))
                .given(userMapper)
                .updateProfile(anyLong(), anyString(), any(), any());

        BusinessException e =
                catchThrowableOfType(
                        BusinessException.class,
                        () ->
                                userService.updateMe(
                                        1L, new UpdateProfileRequest("주니어", null, null)));

        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_409);
        assertThat(e.getDetails()).containsOnlyKeys("nickname");
    }

    @Test
    void AC8_수정_직전에_탈퇴가_커밋되어_바뀐_행이_없으면_AUTH_401() {
        // getActiveMember 통과 직후 다른 요청이 탈퇴시킨 경우: 마스킹된 행을 덮어쓰지 않는다 (UPDATE ... AND status=ACTIVE)
        User user = User.newMember("user@example.com", "hashed", "코딩초보", Level.BEGINNER);
        user.setId(1L);
        given(userMapper.findById(1L)).willReturn(Optional.of(user));
        given(userMapper.updateProfile(anyLong(), anyString(), any(), any())).willReturn(0);

        BusinessException e =
                catchThrowableOfType(
                        BusinessException.class,
                        () ->
                                userService.updateMe(
                                        1L, new UpdateProfileRequest("주니어", null, null)));

        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AUTH_401);
    }
}
