package com.vibenote.user;

import com.vibenote.common.Level;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.user.domain.User;
import com.vibenote.user.dto.MyProfileResponse;
import com.vibenote.user.dto.SignupRequest;
import com.vibenote.user.dto.UpdateProfileRequest;
import com.vibenote.user.dto.UserProfileResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원가입(F-01), 회원 조회, 프로필 조회·수정(F-03). 다른 도메인(auth 등)은 UserMapper 대신 이 Service를 쓴다. */
@Service
public class UserService {

    static final String EMAIL_TAKEN = "이미 사용 중인 이메일입니다.";
    static final String NICKNAME_TAKEN = "이미 사용 중인 닉네임입니다.";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /** 가입하고 새 회원 ID를 돌려준다. 이메일·닉네임이 이미 있으면 USER_409 (details에 해당 필드). */
    @Transactional
    public long signup(SignupRequest request) {
        Map<String, String> taken = new LinkedHashMap<>();
        if (userMapper.existsByEmail(request.email())) {
            taken.put("email", EMAIL_TAKEN);
        }
        if (userMapper.existsByNickname(request.nickname())) {
            taken.put("nickname", NICKNAME_TAKEN);
        }
        if (!taken.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_409, taken);
        }

        User user =
                User.newMember(
                        request.email(),
                        passwordEncoder.encode(request.password()),
                        request.nickname(),
                        request.levelValue());
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 중복 확인과 저장 사이에 같은 값으로 먼저 가입한 경우 (동시 가입)
            throw new BusinessException(ErrorCode.USER_409, takenField(e));
        }
        return user.getId();
    }

    public Optional<User> findByEmail(String email) {
        return userMapper.findByEmail(email);
    }

    public Optional<User> findById(long id) {
        return userMapper.findById(id);
    }

    /**
     * 회원 행을 공유 잠금으로 읽는다. users → refresh_tokens 순서로 잠가 탈퇴(users X 잠금 후 토큰 삭제)와 데드락이 나지 않게 할 때 쓴다
     * (F-03, Codex 리뷰).
     */
    public Optional<User> findByIdForShare(long id) {
        return userMapper.findByIdForShare(id);
    }

    /** 활성 회원의 수준. userId가 null이거나 없거나 탈퇴한 회원이면 null (공개 API가 토큰 주인을 확인할 때, K-1). */
    public Level findActiveLevel(Long userId) {
        return userId == null
                ? null
                : userMapper
                        .findById(userId)
                        .filter(User::isActive)
                        .map(User::getLevel)
                        .orElse(null);
    }

    /** 인증된 회원 본인. Access 토큰이 남아 있어도 탈퇴했으면 AUTH_401 (필터는 DB를 보지 않는다). */
    public User getActiveMember(long userId) {
        return userMapper
                .findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_401));
    }

    /**
     * 활성 회원 행을 배타 잠금으로 읽는다. 호출자의 트랜잭션이 끝날 때까지 같은 회원의 다른 요청이 기다리므로 "세고 → 넣기"가 겹치지 않는다 (F-05a 일일 한도).
     * 탈퇴했으면 AUTH_401.
     */
    public User lockActiveMember(long userId) {
        return userMapper
                .findByIdForUpdate(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_401));
    }

    /** 공개 프로필 대상. 없거나 탈퇴했으면 USER_404. */
    public User getPublicMember(long id) {
        return userMapper
                .findById(id)
                .filter(User::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_404));
    }

    public MyProfileResponse getMe(long userId) {
        return MyProfileResponse.from(getActiveMember(userId));
    }

    public UserProfileResponse getPublicProfile(long id) {
        return UserProfileResponse.from(getPublicMember(id));
    }

    /**
     * 보낸 필드만 바꾼다 (null은 SQL이 건드리지 않아 동시에 온 다른 필드 수정을 덮어쓰지 않는다. bio ''는 지움). 다른 회원의 닉네임이면 USER_409
     * (details.nickname).
     */
    @Transactional
    public MyProfileResponse updateMe(long userId, UpdateProfileRequest request) {
        getActiveMember(userId);
        String nickname = request.nickname();
        if (nickname == null && request.bio() == null && request.levelValue() == null) {
            return getMe(userId);
        }
        if (nickname != null && userMapper.existsByNicknameExceptId(nickname, userId)) {
            throw new BusinessException(ErrorCode.USER_409, Map.of("nickname", NICKNAME_TAKEN));
        }
        int updated;
        try {
            updated =
                    userMapper.updateProfile(userId, nickname, request.bio(), request.levelValue());
        } catch (DuplicateKeyException e) {
            // 중복 확인과 저장 사이에 다른 회원이 같은 닉네임을 가져간 경우 (바꾸는 UNIQUE 컬럼은 닉네임뿐이다)
            throw new BusinessException(ErrorCode.USER_409, Map.of("nickname", NICKNAME_TAKEN));
        }
        if (updated == 0) {
            // 확인과 저장 사이에 탈퇴가 커밋됨: 마스킹된 행을 덮어쓰지 않았다
            throw new BusinessException(ErrorCode.AUTH_401);
        }
        return getMe(userId);
    }

    /** 탈퇴 마스킹 (F-03). 그 사이 다른 요청이 먼저 탈퇴시켰으면 AUTH_401. 다른 데이터 정리는 UserWithdrawalService가 한다. */
    @Transactional
    public void markWithdrawn(long userId) {
        if (userMapper.withdraw(userId) == 0) {
            throw new BusinessException(ErrorCode.AUTH_401);
        }
    }

    private static Map<String, String> takenField(DuplicateKeyException e) {
        String message = String.valueOf(e.getMessage());
        if (message.contains("uk_users_nickname")) {
            return Map.of("nickname", NICKNAME_TAKEN);
        }
        return Map.of("email", EMAIL_TAKEN);
    }
}
