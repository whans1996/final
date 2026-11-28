package com.vibenote.user;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.vibenote.analysis.AnalysisService;
import com.vibenote.auth.AuthService;
import com.vibenote.common.Level;
import com.vibenote.follow.FollowService;
import com.vibenote.notification.NotificationService;
import com.vibenote.team.TeamService;
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
    void AC6_회원_행을_먼저_잠근_뒤_토큰과_팔로우와_팀과_분석을_지운다() {
        UserService userService = mock(UserService.class);
        User user = User.newMember("user@example.com", "hashed", "코딩초보", Level.BEGINNER);
        user.setId(1L);
        given(userService.getActiveMember(1L)).willReturn(user);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        given(encoder.matches("vibe1234", "hashed")).willReturn(true);
        AuthService authService = mock(AuthService.class);
        FollowService followService = mock(FollowService.class);
        TeamService teamService = mock(TeamService.class);
        AnalysisService analysisService = mock(AnalysisService.class);
        NotificationService notificationService = mock(NotificationService.class);

        new UserWithdrawalService(
                        userService,
                        encoder,
                        authService,
                        followService,
                        teamService,
                        analysisService,
                        notificationService)
                .withdraw(1L, "vibe1234");

        InOrder order =
                inOrder(
                        userService,
                        authService,
                        followService,
                        teamService,
                        analysisService,
                        notificationService);
        order.verify(userService).markWithdrawn(1L);
        order.verify(authService).revokeAll(1L);
        order.verify(followService).removeAllOf(1L);
        // F-20a AC10: 팀장인 팀 삭제와 멤버 행 정리도 users 행을 잠근 뒤에 한다
        order.verify(teamService).removeAllOf(1L);
        // #91: 분석 삭제도 users 행을 잠근 뒤에 한다
        order.verify(analysisService).deleteAllOf(1L);
        // NT-1 AC9: 받은 알림 삭제도 users 행을 잠근 뒤에 한다
        order.verify(notificationService).removeAllOf(1L);
    }
}
