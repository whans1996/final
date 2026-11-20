package com.vibenote.reviewplan;

import com.vibenote.reviewplan.domain.ReviewPlan;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SQL: resources/mapper/reviewplan/ReviewPlanMapper.xml. review_plans (F-14, F-15). 카드 공개 여부는 이
 * Mapper가 보지 않는다 (호출자가 ConceptQueryService로 거른다).
 */
@Mapper
public interface ReviewPlanMapper {

    /** 그 회원에게 그 개념의 TODO 계획이 날짜와 상관없이 있는지. */
    boolean existsTodo(@Param("userId") long userId, @Param("conceptId") long conceptId);

    /** 그 회원에게 {@code planId}가 아닌 그 개념의 TODO 계획이 있는지 (F-15 AC6 DONE→TODO). */
    boolean existsOtherTodo(
            @Param("userId") long userId,
            @Param("conceptId") long conceptId,
            @Param("planId") long planId);

    /** TODO 계획을 넣는다. 같은 (회원, 개념, 날짜) 행이 이미 있으면 넣지 않는다. @return 넣었으면 1, 아니면 0 */
    int insertIgnoringDuplicate(
            @Param("userId") long userId,
            @Param("conceptId") long conceptId,
            @Param("planDate") LocalDate planDate,
            @Param("reason") String reason,
            @Param("createdAt") LocalDateTime createdAt);

    /** {@code from ≤ plan_date ≤ to}인 내 계획 (TODO·DONE), plan_date·id 오름차순 (F-15 AC1). */
    List<ReviewPlan> findInRange(
            @Param("userId") long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** {@code plan_date < today}인 내 TODO 계획, plan_date·id 오름차순 (F-15 AC2). */
    List<ReviewPlan> findOverdue(@Param("userId") long userId, @Param("today") LocalDate today);

    /** 내 계획 하나. 다른 회원의 계획이면 비어 있다. */
    Optional<ReviewPlan> findOwned(@Param("userId") long userId, @Param("planId") long planId);

    /** (회원, 개념, 날짜)로 계획 하나 (UNIQUE V2). 방금 넣은 행을 다시 읽을 때 쓴다. */
    Optional<ReviewPlan> findByDate(
            @Param("userId") long userId,
            @Param("conceptId") long conceptId,
            @Param("planDate") LocalDate planDate);

    int updateStatus(
            @Param("planId") long planId,
            @Param("status") ReviewPlanStatus status,
            @Param("doneAt") LocalDateTime doneAt);

    /**
     * @return 지웠으면 1
     */
    int deleteOwned(@Param("userId") long userId, @Param("planId") long planId);
}
