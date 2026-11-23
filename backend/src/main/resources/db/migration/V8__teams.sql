-- F-20a 팀: 팀장이 초대 코드로 멤버를 모은다. team_challenges는 F-20b에서 추가한다.
-- owner_id는 조회 편의용이고 역할의 정본은 team_members.role이다 (위임 없음, 생성 후 바뀌지 않음).
CREATE TABLE teams (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(30)  NOT NULL,
    description  VARCHAR(200) NULL,
    owner_id     BIGINT       NOT NULL,
    invite_code  CHAR(8)      NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_teams_invite_code (invite_code),
    CONSTRAINT fk_teams_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE team_members (
    team_id    BIGINT                  NOT NULL,
    user_id    BIGINT                  NOT NULL,
    role       ENUM('OWNER','MEMBER')  NOT NULL,
    joined_at  DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (team_id, user_id),
    KEY idx_team_members_user (user_id),
    CONSTRAINT fk_team_members_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
    CONSTRAINT fk_team_members_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
