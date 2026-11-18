package com.vibenote.user;

import com.vibenote.user.domain.User;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

/** SQL: resources/mapper/user/UserMapper.xml */
@Mapper
public interface UserMapper {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    /** 생성된 id를 {@code user.id}에 채운다. */
    void insert(User user);

    Optional<User> findByEmail(String email);

    Optional<User> findById(long id);
}
