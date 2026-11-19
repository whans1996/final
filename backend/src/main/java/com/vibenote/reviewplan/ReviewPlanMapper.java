package com.vibenote.reviewplan;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL: resources/mapper/reviewplan/ReviewPlanMapper.xml. review_plans (F-14, F-15가 확장). */
@Mapper
public interface ReviewPlanMapper {

    /** 그 회원에게 그 개념의 TODO 계획이 날짜와 상관없이 있는지. */
    boolean existsTodo(@Param("userId") long userId, @Param("conceptId") long conceptId);

    /** TODO 계획을 넣는다. 같은 (회원, 개념, 날짜) 행이 이미 있으면 넣지 않는다. @return 넣었으면 1, 아니면 0 */
    int insertIgnoringDuplicate(
            @Param("userId") long userId,
            @Param("conceptId") long conceptId,
            @Param("planDate") LocalDate planDate,
            @Param("reason") String reason,
            @Param("createdAt") LocalDateTime createdAt);
}
