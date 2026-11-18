package com.vibenote.user;

import com.vibenote.auth.AuthService;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.follow.FollowService;
import com.vibenote.user.domain.User;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴 (F-03 AC6~AC8).
 *
 * <p>UserService에 두지 않은 이유: AuthService가 이미 UserService를 쓰므로 UserService가 AuthService를 부르면 순환 의존이
 * 된다. 작성물(posts·comments·concept_reviews)과 퀴즈 기록·찜은 지우지 않는다.
 */
@Service
public class UserWithdrawalService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final FollowService followService;

    public UserWithdrawalService(
            UserService userService,
            PasswordEncoder passwordEncoder,
            AuthService authService,
            FollowService followService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.followService = followService;
    }

    /** 비밀번호가 맞으면 한 트랜잭션에서 마스킹, 모든 기기의 Refresh 토큰 폐기, 팔로우 양방향 삭제를 한다. 틀리면 USER_001. */
    @Transactional
    public void withdraw(long userId, String password) {
        User user = userService.getActiveMember(userId);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(
                    ErrorCode.USER_001, Map.of("password", ErrorCode.USER_001.message()));
        }
        userService.markWithdrawn(userId);
        authService.revokeAll(userId);
        followService.removeAllOf(userId);
    }
}
