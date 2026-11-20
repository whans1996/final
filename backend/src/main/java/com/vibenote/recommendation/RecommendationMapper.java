package com.vibenote.recommendation;

import com.vibenote.recommendation.domain.ConceptCount;
import com.vibenote.recommendation.domain.RecommendationReason;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SQL: resources/mapper/recommendation/RecommendationMapper.xml. 추천 신호(퀴즈 오답·내 분석·복습 계획, 모두 A 테이블)와
 * 이유 캐시 {@code recommendation_reasons} (F-16).
 */
@Mapper
public interface RecommendationMapper {

    /** 내 풀이의 오답 중 카드를 다룬 문항 수, 카드별. 삭제된 분석의 퀴즈는 세지 않는다. */
    List<ConceptCount> countWrongByConcept(long userId);

    /** 삭제되지 않은 내 분석에 나온 카드 id. */
    List<Long> findMyConceptIds(long userId);

    /** 복습을 끝낸 카드: DONE 계획이 있고 TODO 계획은 없다. */
    List<Long> findReviewedConceptIds(long userId);

    /** 오늘 복습할 카드: {@code plan_date ≤ today}인 TODO 계획이 있다 (밀린 복습 포함). */
    List<Long> findDueTodoConceptIds(@Param("userId") long userId, @Param("today") LocalDate today);

    /** 저장된 추천 이유. conceptIds가 비면 호출하지 않는다. */
    List<RecommendationReason> findReasons(
            @Param("userId") long userId, @Param("conceptIds") Collection<Long> conceptIds);

    /** (회원, 카드)마다 이유·신호 키·생성 시각을 덮어쓴다. reasons가 비면 호출하지 않는다. */
    void upsertReasons(
            @Param("userId") long userId,
            @Param("reasons") List<RecommendationReason> reasons,
            @Param("createdAt") LocalDateTime createdAt);
}
