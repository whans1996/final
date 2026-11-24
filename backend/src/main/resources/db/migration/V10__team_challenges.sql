-- F-20c 팀 기간 목표: 팀장이 공유 분석 하나에 기간과 목표 정답률을 정한다. 달성은 조회 때 계산하고 저장하지 않는다.
-- 공유 해제·팀 삭제·분석 실제 삭제는 team_analyses를 거쳐 CASCADE로 지워진다.
-- soft delete된 분석의 행은 지우지 않고 조회·상한·삭제에서 analyses.deleted_at IS NULL로 거른다.
CREATE TABLE team_challenges (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    team_id      BIGINT   NOT NULL,
    analysis_id  BIGINT   NOT NULL,
    start_date   DATE     NOT NULL,
    end_date     DATE     NOT NULL,
    target_rate  INT      NOT NULL COMMENT '목표 정답률 1~100 (%)',
    created_by   BIGINT   NOT NULL COMMENT '만든 팀장',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_team_challenges_team_analysis (team_id, analysis_id, end_date),
    CONSTRAINT fk_team_challenges_share FOREIGN KEY (team_id, analysis_id)
        REFERENCES team_analyses (team_id, analysis_id) ON DELETE CASCADE,
    CONSTRAINT fk_team_challenges_creator FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE CASCADE
);
