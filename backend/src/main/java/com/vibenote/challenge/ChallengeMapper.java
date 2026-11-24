package com.vibenote.challenge;

import com.vibenote.challenge.domain.Challenge;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL: resources/mapper/challenge/ChallengeMapper.xml. 관리자(F-24)와 참여·달성도(F-25)가 함께 쓴다. */
@Mapper
public interface ChallengeMapper {

    // ── F-24 관리자 ──

    /** 관리자 목록 (start_date DESC, id DESC). status가 null이면 전체이고, 상태는 today와 기간으로 거른다. 참여자 수를 채운다. */
    List<Challenge> findForAdmin(
            @Param("status") ChallengeStatus status,
            @Param("today") LocalDate today,
            @Param("size") int size,
            @Param("offset") long offset);

    /** {@link #findForAdmin}과 같은 조건의 개수. */
    long countForAdmin(@Param("status") ChallengeStatus status, @Param("today") LocalDate today);

    /** 참여자 수를 채운다. */
    Optional<Challenge> findById(long id);

    /** 수정·삭제 전 잠금 조회 (참여자 수는 채우지 않는다). */
    Optional<Challenge> findByIdForUpdate(long id);

    void insert(Challenge challenge);

    /** 제목·설명·이미지·기간·목표 전체 교체. created_by는 바꾸지 않는다. */
    void update(Challenge challenge);

    /** 참여 기록은 FK CASCADE로 함께 지워진다. */
    int deleteById(long id);

    // ── F-25 회원 ──

    /**
     * 회원용 목록. 진행 중은 end_date ASC, 예정은 start_date ASC, 종료는 end_date DESC이고 모두 다음 기준 id DESC다. 참여자 수와
     * viewerId의 참여 여부(joined, 비로그인이면 false)를 채운다.
     */
    List<Challenge> findForMember(
            @Param("status") ChallengeStatus status,
            @Param("today") LocalDate today,
            @Param("viewerId") Long viewerId,
            @Param("size") int size,
            @Param("offset") long offset);

    /** {@link #findForMember}와 같은 조건의 개수. */
    long countForMember(@Param("status") ChallengeStatus status, @Param("today") LocalDate today);
}
