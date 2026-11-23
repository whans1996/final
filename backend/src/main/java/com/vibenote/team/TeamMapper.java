package com.vibenote.team;

import com.vibenote.team.domain.MyTeam;
import com.vibenote.team.domain.Team;
import com.vibenote.team.domain.TeamMember;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL: resources/mapper/team/TeamMapper.xml */
@Mapper
public interface TeamMapper {

    /** 코드가 이미 있으면 uk_teams_invite_code 위반(DuplicateKeyException). id를 채운다. */
    void insertTeam(Team team);

    void insertMember(
            @Param("teamId") long teamId,
            @Param("userId") long userId,
            @Param("role") TeamRole role,
            @Param("joinedAt") LocalDateTime joinedAt);

    Team findById(long teamId);

    /** 팀 행을 배타 잠금으로 읽는다. 가입의 "인원 세기 → 넣기"와 팀장 작업이 겹치지 않게 한다. 없으면 null. */
    Team findByIdForUpdate(long teamId);

    /** 코드로 팀 행을 배타 잠금으로 읽는다 (가입). 없으면 null. */
    Team findByInviteCodeForUpdate(String inviteCode);

    /** 그 회원의 역할. 멤버가 아니면 null. */
    TeamRole findRole(@Param("teamId") long teamId, @Param("userId") long userId);

    int countMembers(long teamId);

    /** 그 회원이 속한 팀 수 (만든 팀 포함). */
    int countTeamsOf(long userId);

    /** 팀장이 맨 앞, 나머지는 가입 순(joined_at, user id). */
    List<TeamMember> findMembers(long teamId);

    /** 내가 가입한 시각 최신순(joined_at DESC, team id DESC). */
    List<MyTeam> findMyTeams(long userId);

    void updateInfo(
            @Param("teamId") long teamId,
            @Param("name") String name,
            @Param("description") String description);

    /** 코드가 이미 있으면 uk_teams_invite_code 위반(DuplicateKeyException). */
    void updateInviteCode(@Param("teamId") long teamId, @Param("inviteCode") String inviteCode);

    int deleteMember(@Param("teamId") long teamId, @Param("userId") long userId);

    /** team_members는 FK CASCADE로 함께 지워진다. */
    void deleteTeam(long teamId);

    /** 그 회원이 팀장인 팀을 지운다 (멤버 행은 CASCADE). */
    void deleteTeamsOwnedBy(long userId);

    /** 그 회원의 남은 멤버 행을 지운다. */
    void deleteMembershipsOf(long userId);
}
