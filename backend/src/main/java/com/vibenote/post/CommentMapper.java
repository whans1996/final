package com.vibenote.post;

import com.vibenote.post.domain.Comment;
import com.vibenote.post.domain.Post;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL: resources/mapper/post/CommentMapper.xml (F-18). */
@Mapper
public interface CommentMapper {

    boolean existsPost(@Param("postId") long postId);

    /** 글 행을 공유 잠금으로 읽는다 (작성 중 글이 지워져 FK 오류가 나지 않게). 없으면 0. */
    int lockPost(@Param("postId") long postId);

    /** 알림(NT-1 AC10)에 쓸 글쓴이 id와 제목. id·userId·title만 채운다. */
    Post findPostHeader(@Param("postId") long postId);

    /** 가장 최근 limit개를 오래된순(created_at, id)으로 (#208). 작성자 닉네임·상태를 함께 읽는다. */
    List<Comment> findByPost(@Param("postId") long postId, @Param("limit") int limit);

    long countByPost(@Param("postId") long postId);

    /** 작성자 닉네임·상태를 함께 읽는다 (작성·수정 응답용). */
    Optional<Comment> findById(@Param("id") long id);

    /** 그 글에 속한 댓글을 배타 잠금으로 읽는다 (수정·삭제가 겹치지 않게). 없거나 다른 글의 댓글이면 empty. */
    Optional<Comment> findInPostForUpdate(
            @Param("postId") long postId, @Param("commentId") long commentId);

    /** id를 채운다 (useGeneratedKeys). */
    void insert(Comment comment);

    void updateContent(
            @Param("id") long id,
            @Param("content") String content,
            @Param("updatedAt") LocalDateTime updatedAt);

    void delete(@Param("id") long id);
}
