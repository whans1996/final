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

    /** 인증된 회원 본인. Access 토큰이 남아 있어도 탈퇴했으면 AUTH_401 (필터는 DB를 보지 않는다). */
    public User getActiveMember(long userId) {
        return userMapper
                .findById(userId)
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

    /** 보낸 필드만 바꾼다. 다른 회원의 닉네임이면 USER_409 (details.nickname). */
    @Transactional
    public MyProfileResponse updateMe(long userId, UpdateProfileRequest request) {
        User user = getActiveMember(userId);
        String nickname = request.nickname() == null ? user.getNickname() : request.nickname();
        String bio = request.bio() == null ? user.getBio() : emptyToNull(request.bio());
        Level level = request.levelValue() == null ? user.getLevel() : request.levelValue();

        if (request.nickname() != null && userMapper.existsByNicknameExceptId(nickname, userId)) {
            throw new BusinessException(ErrorCode.USER_409, Map.of("nickname", NICKNAME_TAKEN));
        }
        try {
            userMapper.updateProfile(userId, nickname, bio, level);
        } catch (DuplicateKeyException e) {
            // 중복 확인과 저장 사이에 다른 회원이 같은 닉네임을 가져간 경우 (바꾸는 UNIQUE 컬럼은 닉네임뿐이다)
            throw new BusinessException(ErrorCode.USER_409, Map.of("nickname", NICKNAME_TAKEN));
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

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    private static Map<String, String> takenField(DuplicateKeyException e) {
        String message = String.valueOf(e.getMessage());
        if (message.contains("uk_users_nickname")) {
            return Map.of("nickname", NICKNAME_TAKEN);
        }
        return Map.of("email", EMAIL_TAKEN);
    }
}
