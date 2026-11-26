package com.vibenote.post;

import com.vibenote.post.domain.Post;
import java.time.LocalDateTime;
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

    /** id를 채운다 (useGeneratedKeys). */
    void insert(Post post);

    /** 게시판·제목·본문·첨부를 통째로 바꾸고 updated_at을 채운다. 조회수·작성 시각은 그대로. */
    void update(
            @Param("id") long id,
            @Param("boardType") BoardType boardType,
            @Param("title") String title,
            @Param("content") String content,
            @Param("analysisId") Long analysisId,
            @Param("conceptId") Long conceptId,
            @Param("updatedAt") LocalDateTime updatedAt);

    void delete(@Param("id") long id);

    /** 작성자 id (행 잠금, 수정·삭제가 겹치지 않게). 없는 글이면 null. */
    Long findAuthorIdForUpdate(@Param("id") long id);

    /** 지금 첨부된 분석 id (없으면 null). 수정할 때 첨부를 그대로 두는지 본다 (#206). */
    Long findAnalysisId(@Param("id") long id);

    /**
     * 첨부할 수 있는 분석인지: 그 회원의 DONE이고 삭제되지 않은 분석. {@code requirePublic}이면 공개 분석만 (#206, 수정 때 기존 첨부를 그대로
     * 두면 false). 표시용 읽기와 같은 이유로 analyses를 이 Mapper에서 직접 본다 (분석 도메인에 같은 조건의 조회가 없다).
     */
    boolean existsAttachableAnalysis(
            @Param("analysisId") long analysisId,
            @Param("userId") long userId,
            @Param("requirePublic") boolean requirePublic);

    boolean existsPublishedConcept(@Param("conceptId") long conceptId);
}
