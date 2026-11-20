package com.vibenote.concept.review;

import com.vibenote.concept.review.domain.ConceptReview;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL: resources/mapper/concept/ConceptReviewMapper.xml (F-11). */
@Mapper
public interface ConceptReviewMapper {

    /** 최신순(created_at DESC, id DESC) 페이지. 작성자 닉네임·상태를 함께 읽는다. */
    List<ConceptReview> findPage(
            @Param("conceptId") long conceptId,
            @Param("size") int size,
            @Param("offset") long offset);

    long countByConcept(@Param("conceptId") long conceptId);

    Optional<ConceptReview> findByConceptAndUser(
            @Param("conceptId") long conceptId, @Param("userId") long userId);

    /** id를 채운다 (useGeneratedKeys). UNIQUE (concept_id, user_id) 위반이면 DuplicateKeyException. */
    void insert(ConceptReview review);

    void update(
            @Param("id") long id, @Param("rating") int rating, @Param("content") String content);

    void delete(@Param("id") long id);

    /** 같은 카드의 리뷰 쓰기를 직렬화한다 (SELECT ... FOR UPDATE). */
    Long lockConcept(@Param("conceptId") long conceptId);

    /** concepts.avg_rating = 전체 리뷰 평균(소수 1자리 반올림), 리뷰가 없으면 0.0. */
    void refreshAvgRating(@Param("conceptId") long conceptId);

    BigDecimal findAvgRating(@Param("conceptId") long conceptId);
}
