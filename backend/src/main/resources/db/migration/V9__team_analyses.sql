-- F-20b 팀 공유 분석: 멤버가 자기 완료 분석을 팀에 공유하면 팀원이 그 퀴즈를 풀 수 있다.
-- 공유한 사람 = 분석 주인(analyses.user_id). team_challenges는 F-20c에서 추가한다.
-- soft delete된 분석의 행은 지우지 않고 조회·권한·상한에서 analyses.deleted_at IS NULL로 거른다.
CREATE TABLE team_analyses (
    team_id      BIGINT   NOT NULL,
    analysis_id  BIGINT   NOT NULL,
    shared_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (team_id, analysis_id),
    KEY idx_team_analyses_analysis (analysis_id),
    CONSTRAINT fk_team_analyses_team     FOREIGN KEY (team_id)     REFERENCES teams (id)    ON DELETE CASCADE,
    CONSTRAINT fk_team_analyses_analysis FOREIGN KEY (analysis_id) REFERENCES analyses (id) ON DELETE CASCADE
);
