package com.vibenote.post;

import com.vibenote.post.domain.Post;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL: resources/mapper/post/PostMapper.xml (F-17). */
@Mapper
public interface PostMapper {

    /**
     * 최신순(created_at DESC, id DESC) 페이지. 작성자 닉네임·상태와 댓글 수를 함께 읽는다. boardType·keyword가 null이면 그 조건이
     * 없다. keyword는 LIKE 이스케이프({@code !})를 마친 값이다.
     */
    List<Post> findPage(
            @Param("boardType") BoardType boardType,
            @Param("keyword") String keyword,
            @Param("size") int size,
            @Param("offset") long offset);

    long count(@Param("boardType") BoardType boardType, @Param("keyword") String keyword);
}
