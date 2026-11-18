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

    /** 닉네임·소개·수준을 주어진 값으로 바꾼다 (bio null이면 NULL로 저장). */
    void updateProfile(
            @Param("id") long id,
            @Param("nickname") String nickname,
            @Param("bio") String bio,
            @Param("level") Level level);

    /** 탈퇴 처리: status·withdrawn_at을 채우고 개인정보를 마스킹한다 (F-03 AC6). 활성 회원만 바꾸며 바뀐 행 수를 돌려준다. */
    int withdraw(long id);
}
