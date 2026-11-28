package com.vibenote.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.vibenote.user.UserService;
import com.vibenote.user.UserWithdrawalService;
import com.vibenote.user.dto.SignupRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Transactional;

/** NT-1 AC9. 탈퇴는 실제 {@link UserWithdrawalService}로 한다 (비밀번호 확인이 있어 가입으로 회원을 만든다). */
@Transactional
class NotificationWithdrawalTest extends NotificationTestSupport {

    private static final String PASSWORD = "vibe1234";

    @Autowired UserService userService;
    @Autowired UserWithdrawalService withdrawalService;

    @Test
    void AC9_탈퇴하면_받은_알림은_지우고_일으킨_알림은_마스킹해_남긴다() throws Exception {
        long leaver =
                userService.signup(
                        new SignupRequest("nt1leaver@nt1.test", PASSWORD, "nt1leaver", "BEGINNER"));
        long other = insertUser("nto");
        insertNotification(
                leaver, "FOLLOW", other, "회원님을 팔로우했어요", "/users/" + other, FIXED_NOW, null);
        insertNotification(
                other, "FOLLOW", leaver, "회원님을 팔로우했어요", "/users/" + leaver, FIXED_NOW, null);

        withdrawalService.withdraw(leaver, PASSWORD);

        assertThat(countFor(leaver)).isZero();
        assertThat(countFor(other)).isEqualTo(1);
        mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(jsonPath("$.items[0].actor.id").value(leaver))
                .andExpect(jsonPath("$.items[0].actor.nickname").value("#탈퇴" + leaver));
    }
}
