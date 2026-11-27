-- P-1a 프로젝트·버전 묶음 (docs/specs/P-1a-project-versions.md 4절)
-- 분석을 프로젝트(저장소 단위) → 버전(커밋) → 수준별 분석으로 묶는다.
-- 저장소 프로젝트는 (회원, 정규화한 주소)마다 하나다. ZIP 프로젝트는 source_url이 NULL이라 유니크 키에 걸리지 않고 여럿 생긴다.
CREATE TABLE projects (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    user_id                BIGINT       NOT NULL,
    source_type            ENUM('URL','ZIP') NOT NULL,
    source_url             VARCHAR(500) NULL COMMENT '정규화한 저장소 주소. ZIP은 NULL',
    name                   VARCHAR(100) NOT NULL,
    latest_commit_sha      CHAR(40)     NULL COMMENT '새 커밋 배지 조회 결과 (AC10 캐시). 조회 실패면 NULL',
    latest_commit_message  VARCHAR(200) NULL,
    latest_committed_at    DATETIME     NULL,
    latest_checked_at      DATETIME     NULL COMMENT '배지 외부 조회 시각. NULL이면 아직 조회하지 않음',
    legacy_analysis_id     BIGINT       NULL COMMENT '백필 임시 열 (이 파일 끝에서 지운다)',
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_projects_user_url (user_id, source_url),
    CONSTRAINT fk_projects_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 버전 번호는 프로젝트 안에서 1부터 늘어난다. 커밋 정보가 없는 버전(ZIP·커밋 조회 실패·백필)은 commit_sha가 NULL이라 여럿 있을 수 있다.
CREATE TABLE project_versions (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    project_id          BIGINT       NOT NULL,
    number              INT          NOT NULL,
    commit_sha          CHAR(40)     NULL,
    commit_message      VARCHAR(200) NULL COMMENT '커밋 메시지 첫 줄',
    committed_at        DATETIME     NULL,
    legacy_analysis_id  BIGINT       NULL COMMENT '백필 임시 열 (이 파일 끝에서 지운다)',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_versions_number (project_id, number),
    UNIQUE KEY uk_project_versions_commit (project_id, commit_sha),
    CONSTRAINT fk_project_versions_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE
);

ALTER TABLE analyses
    ADD COLUMN project_id         BIGINT NULL AFTER user_id,
    ADD COLUMN project_version_id BIGINT NULL COMMENT '저장소 분석은 FETCHING에서 정한다. 그 전·수집 실패면 NULL' AFTER project_id;

-- 백필 (AC13) 1) 저장소 분석: (회원, 주소)마다 프로젝트 하나. 이름은 주소의 마지막 조각
INSERT INTO projects (user_id, source_type, source_url, name, created_at, updated_at)
SELECT user_id, 'URL', source_url, LEFT(SUBSTRING_INDEX(source_url, '/', -1), 100), MIN(created_at), MIN(created_at)
FROM analyses
WHERE source_type = 'URL'
GROUP BY user_id, source_url;

UPDATE analyses a
JOIN projects p ON p.user_id = a.user_id AND p.source_url = a.source_url
SET a.project_id = p.id
WHERE a.source_type = 'URL';

-- 2) ZIP 분석: 분석마다 프로젝트 하나. 이름은 지금 project_name (비었으면 "project")
INSERT INTO projects (user_id, source_type, source_url, name, legacy_analysis_id, created_at, updated_at)
SELECT user_id, 'ZIP', NULL, LEFT(COALESCE(NULLIF(TRIM(project_name), ''), 'project'), 100), id, created_at, created_at
FROM analyses
WHERE source_type = 'ZIP';

UPDATE analyses a
JOIN projects p ON p.legacy_analysis_id = a.id
SET a.project_id = p.id;

-- 3) 삭제하지 않은 분석마다 커밋 정보 없는 버전. 프로젝트 안에서 요청 순서대로 번호를 준다. 삭제한 분석은 버전 없이 프로젝트에만 붙는다
INSERT INTO project_versions (project_id, number, legacy_analysis_id, created_at)
SELECT project_id, ROW_NUMBER() OVER (PARTITION BY project_id ORDER BY created_at, id), id, created_at
FROM analyses
WHERE deleted_at IS NULL;

UPDATE analyses a
JOIN project_versions v ON v.legacy_analysis_id = a.id
SET a.project_version_id = v.id;

ALTER TABLE projects DROP COLUMN legacy_analysis_id;
ALTER TABLE project_versions DROP COLUMN legacy_analysis_id;

-- 회원·프로젝트를 지우면(테스트 정리, 회원 행은 실제로 지우지 않는다) 분석도 함께 지워진다
ALTER TABLE analyses
    MODIFY COLUMN project_id BIGINT NOT NULL,
    ADD KEY idx_analyses_project_created (project_id, created_at),
    ADD CONSTRAINT fk_analyses_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_analyses_project_version FOREIGN KEY (project_version_id) REFERENCES project_versions (id) ON DELETE CASCADE;
