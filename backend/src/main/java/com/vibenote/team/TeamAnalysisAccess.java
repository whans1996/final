package com.vibenote.team;

import org.springframework.stereotype.Component;

/**
 * 팀에 공유된 분석의 퀴즈 권한 판단 (F-20b AC5). {@code QuizService}가 "분석 주인이거나 공유받은 팀 멤버"인지 볼 때 쓴다. 퀴즈 외 분석 API는
 * 이 판단을 쓰지 않는다.
 */
@Component
public class TeamAnalysisAccess {

    private final TeamMapper teamMapper;

    public TeamAnalysisAccess(TeamMapper teamMapper) {
        this.teamMapper = teamMapper;
    }

    /** userId가 멤버인 팀 중 하나에 그 분석이 공유됐고 분석이 삭제되지 않았으면 true. */
    public boolean isSharedWith(long userId, long analysisId) {
        return teamMapper.isSharedWith(userId, analysisId);
    }
}
