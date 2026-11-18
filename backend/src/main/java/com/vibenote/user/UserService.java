package com.vibenote.user;

import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.user.domain.User;
import com.vibenote.user.dto.SignupRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원가입, 회원 조회 (F-01). 다른 도메인(auth 등)은 UserMapper 대신 이 Service를 쓴다. */
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

    private static Map<String, String> takenField(DuplicateKeyException e) {
        String message = String.valueOf(e.getMessage());
        if (message.contains("uk_users_nickname")) {
            return Map.of("nickname", NICKNAME_TAKEN);
        }
        return Map.of("email", EMAIL_TAKEN);
    }
}
