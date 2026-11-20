package com.vibenote.concept.wish;

import com.vibenote.concept.domain.Concept;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL: resources/mapper/concept/ConceptWishMapper.xml (F-12). */
@Mapper
public interface ConceptWishMapper {

    boolean exists(@Param("userId") long userId, @Param("conceptId") long conceptId);

    /** PK (user_id, concept_id) 위반이면 DuplicateKeyException. */
    void insert(
            @Param("userId") long userId,
            @Param("conceptId") long conceptId,
            @Param("createdAt") LocalDateTime createdAt);

    void delete(@Param("userId") long userId, @Param("conceptId") long conceptId);

    /** 같은 카드의 찜 쓰기를 직렬화한다 (SELECT ... FOR UPDATE). 없는 카드면 null. */
    Long lockConcept(@Param("conceptId") long conceptId);

    /** concepts.wish_count = 그 카드의 실제 찜 행 수. */
    void refreshWishCount(@Param("conceptId") long conceptId);

    int findWishCount(@Param("conceptId") long conceptId);

    /** 회원이 찜한 PUBLISHED 카드 요약, 찜한 시각 최신순(created_at DESC, concept_id DESC). */
    List<Concept> findPublishedPageByUser(
            @Param("userId") long userId, @Param("size") int size, @Param("offset") long offset);

    long countPublishedByUser(@Param("userId") long userId);
}
