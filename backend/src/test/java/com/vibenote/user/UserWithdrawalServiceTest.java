package com.vibenote.user;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.vibenote.auth.AuthService;
import com.vibenote.common.Level;
import com.vibenote.follow.FollowService;
import com.vibenote.user.domain.User;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * F-03 AC6 잠금 순서 (Codex 리뷰): 탈퇴는 users 행을 먼저 잠그고(마스킹 UPDATE) 그다음 refresh_tokens·follows를 지운다.
 * 로그인·재발급도 users를 먼저 잠그므로 순서가 엇갈려 데드락이 나지 않는다.
 */
class UserWithdrawalServiceTest {

    @Test
    void AC6_회원_행을_먼저_잠근_뒤_토큰과_팔로우를_지운다() {
        UserService userService = mock(UserService.class);
        User user = User.newMember("user@example.com", "hashed", "코딩초보", Level.BEGINNER);
        user.setId(1L);
        given(userService.getActiveMember(1L)).willReturn(user);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        given(encoder.matches("vibe1234", "hashed")).willReturn(true);
        AuthService authService = mock(AuthService.class);
        FollowService followService = mock(FollowService.class);

        new UserWithdrawalService(userService, encoder, authService, followService)
                .withdraw(1L, "vibe1234");

        InOrder order = inOrder(userService, authService, followService);
        order.verify(userService).markWithdrawn(1L);
        order.verify(authService).revokeAll(1L);
        order.verify(followService).removeAllOf(1L);
    }
}
