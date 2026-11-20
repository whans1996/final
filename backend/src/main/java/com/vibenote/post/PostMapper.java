package com.vibenote.post;

import com.vibenote.post.domain.Post;
import java.util.List;
import java.util.Optional;
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

    /** 조회수 +1. 바뀐 행 수(없는 글이면 0)를 돌려준다. updated_at은 바꾸지 않는다. */
    int incrementViewCount(@Param("id") long id);

    /** 상세. 첨부한 분석(주인·공개·삭제 여부·제목·프로젝트명)과 카드(상태·표시 필드)를 함께 읽는다. */
    Optional<Post> findDetail(@Param("id") long id);
}
