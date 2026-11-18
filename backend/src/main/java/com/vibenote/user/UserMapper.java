package com.vibenote.user;

import com.vibenote.common.Level;
import com.vibenote.user.domain.User;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL: resources/mapper/user/UserMapper.xml */
@Mapper
public interface UserMapper {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    /** 그 회원을 뺀 다른 회원이 이 닉네임을 쓰는지 (대소문자 무시, F-03 수정). */
    boolean existsByNicknameExceptId(@Param("nickname") String nickname, @Param("id") long id);

    /** 생성된 id를 {@code user.id}에 채운다. */
    void insert(User user);

    Optional<User> findByEmail(String email);

    Optional<User> findById(long id);

    /** findById와 같지만 공유 잠금(FOR SHARE)으로 커밋된 최신 행을 읽는다. 잠금 순서를 users 먼저로 맞출 때 쓴다 (F-03). */
    Optional<User> findByIdForShare(long id);

    /**
     * null이 아닌 필드만 바꾼다 (bio ''는 NULL로 지움). 모두 null이면 호출하지 않는다(SET 절이 빈다). 활성 회원만 바꾸며 바뀐 행 수를 돌려준다
     * (탈퇴 직후의 수정이 마스킹을 덮어쓰지 않게).
     */
    int updateProfile(
            @Param("id") long id,
            @Param("nickname") String nickname,
            @Param("bio") String bio,
            @Param("level") Level level);

    /** 탈퇴 처리: status·withdrawn_at을 채우고 개인정보를 마스킹한다 (F-03 AC6). 활성 회원만 바꾸며 바뀐 행 수를 돌려준다. */
    int withdraw(long id);
}
