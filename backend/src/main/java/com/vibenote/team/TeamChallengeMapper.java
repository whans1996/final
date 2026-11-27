package com.vibenote.team;

import com.vibenote.team.domain.PeriodAttempt;
import com.vibenote.team.domain.TeamChallenge;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 팀 목표 SQL (F-20c): resources/mapper/team/TeamChallengeMapper.xml. analyses·quizzes·quiz_attempts는
 * 읽기만 한다. soft delete된 분석의 목표는 모든 조회·상한·삭제에서 거른다.
 */
@Mapper
public interface TeamChallengeMapper {

    /**
     * 그 팀에 미삭제 분석이 공유됐는가. 공유 행을 공유 잠금으로 읽어 목표 insert까지 공유 해제·탈퇴가 그 행을 지우지 못하게 한다 (#168, insert의 FK
     * 검사가 스냅샷과 다른 최신 행을 봐 500이 되던 경합).
     */
    boolean isShared(@Param("teamId") long teamId, @Param("analysisId") long analysisId);

    /** 그 분석에 종료일이 today 이후(포함)인 목표가 있는가. */
    boolean existsOpen(
            @Param("teamId") long teamId,
            @Param("analysisId") long analysisId,
            @Param("today") LocalDate today);

    /** 미삭제 분석의 목표 수. teams FOR UPDATE 뒤에 부른다. */
    int countByTeam(long teamId);

    void insert(
            @Param("teamId") long teamId,
            @Param("analysisId") long analysisId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("targetRate") int targetRate,
            @Param("createdBy") long createdBy,
            @Param("createdAt") LocalDateTime createdAt);

    /** 같은 연결에서 방금 넣은 행의 id. */
    @Select("SELECT LAST_INSERT_ID()")
    long lastInsertId();

    /** 미삭제 분석의 목표. analysisId·challengeId가 null이면 거르지 않는다. 정렬은 Service가 한다. */
    List<TeamChallenge> findChallenges(
            @Param("teamId") long teamId,
            @Param("analysisId") Long analysisId,
            @Param("challengeId") Long challengeId);

    /** 목표마다 지금 멤버의 기간 안 가장 최근 풀이(created_at DESC, id DESC). 조건은 findChallenges와 같다. */
    List<PeriodAttempt> findPeriodAttempts(
            @Param("teamId") long teamId,
            @Param("analysisId") Long analysisId,
            @Param("challengeId") Long challengeId);

    /** 그 팀의 미삭제 분석 목표 하나를 지운다. 지운 행 수. */
    int delete(@Param("teamId") long teamId, @Param("challengeId") long challengeId);
}
