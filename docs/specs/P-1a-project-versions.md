# P-1a 프로젝트·버전 묶음·새 커밋 배지

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | P-1a (로드맵 트랙 1, 11/27~12/02, 넘겨주기 12/02 A→A: T-1·I-1b·I-2, 결정로그 11-27) |
| 우선순위 | T0 (시연 줄거리, 기획서 7.4b) |
| 영역 오너 | A 김동환 (AI·분석·학습 루프) |
| 관련 필수 요구사항 | ⑥ 분석 관리(CRUD) |
| 의존 spec | F-05a ZIP 분석, F-05b 저장소 URL 분석, F-19 분석 관리, L-1a 실패 분석 재실행 |
| 관련 이슈 | 없음 |
| 승인 | 김동환 2026-11-27 |

## 1. 목적

지금은 분석이 서로 독립된 스냅샷이다. 같은 저장소를 커밋할 때마다 다시 분석하면 목록에 흩어지고, 팀·면접 화면(T-1·I-1)이 기준으로 삼을 "이 프로젝트의 최신 분석"이 없다 (문제 1·5·6·7, 기획서 7.4a P-1).
P-1a는 분석을 **프로젝트(저장소 단위) → 버전(커밋) → 수준별 분석**으로 묶는다. 프로젝트를 열면 기본 브랜치에 새 커밋이 있는지 배지로 알려 주고, 사용자는 버튼 하나로 최신 커밋을 새 버전으로 분석한다.
증분 재분석과 "이번 버전에서 바뀐 부분"은 P-1b에서 한다. P-1a는 그 바탕이 되는 묶음·계약·화면을 12/02까지 넘긴다.

시나리오: 입문 팀원이 `https://github.com/octo/todo-app`을 분석한다 → 프로젝트 "todo-app" v1이 생긴다 → 다음 날 팀원이 로그인 기능을 푸시한다 → 프로젝트 화면에 "새 커밋이 있어요 · feat: JWT 로그인 (3시간 전)" 배지가 보인다 → "최신 커밋 분석"을 누르면 v2가 생긴다.

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_처음_분석하는_저장소면_프로젝트를_만든다()`

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 로그인한 회원이 | 저장소 주소로 분석을 요청하면 (`projectId` 없음) | 정규화한 주소가 같은 내 프로젝트가 있으면 그 프로젝트에, 없으면 새 프로젝트(이름 = 저장소 이름, 예 `todo-app`)에 분석이 붙는다. 202 응답에 `projectId`가 있다. 다른 회원의 같은 주소 프로젝트와는 섞이지 않는다 |
| AC2 | 저장소 주소 분석이 FETCHING 단계에 들어가면 | 기본 브랜치 최신 커밋을 조회해 | **그 커밋으로** 아카이브를 받고(GitHub `zipball/{sha}`, GitLab `archive.zip?sha=`), 같은 커밋 버전이 그 프로젝트에 있으면 그 버전에, 없으면 새 버전(번호 = 그 프로젝트의 최대 번호 + 1)에 분석을 붙인다. 버전에는 커밋 SHA·메시지 첫 줄(200자까지)·커밋 시각을 저장한다. 같은 커밋을 다른 수준으로 분석하면 같은 버전 아래 분석이 둘이 된다 |
| AC3 | 커밋 조회가 실패하면 (rate limit·5xx·시간 초과·빈 저장소) | FETCHING이 진행될 때 | 분석을 실패시키지 않고 지금처럼 기본 브랜치 아카이브를 받는다. 커밋 정보가 없는 새 버전에 붙는다. 아카이브 받기 자체가 실패하면 지금과 같은 에러 코드로 FAILED가 되고 버전을 만들지 않는다 |
| AC4 | 로그인한 회원이 | ZIP으로 분석을 요청하면 | `projectId`가 없으면 새 프로젝트(이름 = 파일 이름, 지금 `project_name` 규칙)와 버전 1이 생긴다. 내 ZIP 프로젝트의 `projectId`를 보내면 그 프로젝트의 새 버전(최대 번호 + 1)이 생긴다. ZIP 버전에는 커밋 정보가 없다 |
| AC5 | 잘못된 `projectId`로 | 분석을 요청하면 | 없거나 남의 프로젝트면 404 `PROJECT_404`. 숫자가 아니거나, ZIP을 저장소 프로젝트에 올리거나, 저장소 주소가 그 프로젝트 주소와 다르거나, 저장소 주소를 ZIP 프로젝트에 보내면 400 `COMMON_400` `details.projectId`. 실패하면 프로젝트·버전·분석이 하나도 생기지 않고 오늘 한도도 쓰지 않는다 |
| AC6 | 로그인한 회원이 | `GET /api/projects`를 부르면 | 삭제하지 않은 분석이 하나 이상 있는 **내** 프로젝트만 마지막 분석 요청이 최신인 순으로 페이지(`PageQuery`, 기본 size 20)로 받는다. 항목마다 버전 수, 최신 버전(번호·커밋), 분석한 수준 목록이 있다. 분석을 모두 삭제한 프로젝트는 목록에 없다 |
| AC7 | 내 프로젝트 id로 | `GET /api/projects/{id}`를 부르면 | 프로젝트 정보, 버전 목록(번호 내림차순), 삭제하지 않은 분석 목록(버전 id·수준·상태·퀴즈 상태·요청 시각, 아직 버전이 없는 FETCHING·FAILED 분석은 `versionId: null`)을 받는다. 없거나 남의 프로젝트거나 분석을 모두 삭제한 프로젝트면 404 `PROJECT_404` (존재를 드러내지 않는다) |
| AC8 | 내 프로젝트에 | `PATCH /api/projects/{id}`로 이름을 바꾸면 | 앞뒤 공백을 뗀 1~100자면 200과 바뀐 이름을 받는다. 비거나 100자를 넘으면 400 `COMMON_400` `details.name`. 남의 프로젝트면 404. 분석의 `title`은 바뀌지 않는다 |
| AC9 | 내 저장소 프로젝트에서 | `GET /api/projects/{id}/latest-commit`을 부르면 | 기본 브랜치 최신 커밋이 분석한 최신 커밋(커밋 정보가 있고 삭제하지 않은 분석이 붙은 버전 중 번호가 가장 큰 것)과 다르면 `NEW_COMMIT`, 같으면 `UP_TO_DATE`. 분석한 커밋이 없거나 조회에 실패하면 `UNKNOWN`. 모두 200이다. ZIP 프로젝트는 외부 요청 없이 `NOT_SUPPORTED`. 남의 프로젝트면 404 |
| AC10 | 5분 안에 같은 프로젝트를 다시 열면 | latest-commit을 부를 때 | 외부 요청 없이 저장해 둔 조회 결과를 쓴다 (`checkedAt` 그대로). 조회 실패도 5분 동안 저장해 재시도가 몰리지 않는다. 설정값 `analysis.project.latest-commit-ttl` (기본 5m) |
| AC11 | 서버에 GitHub 토큰이 설정되어 있으면 | 커밋을 조회할 때 (AC2·AC9) | F-05b처럼 공개 저장소인지 먼저 확인한다. 비공개로 바뀌었으면 AC2는 지금처럼 `ANALYSIS_004` FAILED, AC9는 커밋 정보 없이 `UNKNOWN`이다 |
| AC12 | 분석 상세·목록·재실행에서 | 분석을 조회하거나 재실행하면 | `GET /api/analyses/{id}`에 `project`(id·이름)와 `version`(id·번호·커밋, 없으면 null)이, `GET /api/analyses` 항목에 `projectId`·`versionNumber`가 있다. L-1a 재실행으로 생긴 새 분석은 원본과 같은 프로젝트에 붙고 버전은 FETCHING에서 다시 정한다 |
| AC13 | V15 마이그레이션 전에 분석이 있으면 | 마이그레이션을 적용할 때 | 저장소 분석은 (회원, 주소)마다 프로젝트 하나, ZIP 분석은 분석마다 프로젝트 하나가 생긴다. 삭제하지 않은 분석마다 커밋 정보가 없는 버전이 요청 순서대로 번호를 받는다. 삭제한 분석은 프로젝트에만 붙고 버전은 없다. 이후 모든 분석의 `project_id`는 NOT NULL이다 |
| AC14 | 회원이 FE에서 | `/projects`를 열면 | 내 프로젝트 카드(이름, 저장소·ZIP 표시, 버전 수, 최신 버전, 수준 배지)를 본다. 로딩·빈 목록("아직 분석한 프로젝트가 없어요" + 분석 요청 링크)·에러를 표시한다 |
| AC15 | 회원이 FE에서 | `/projects/:id`를 열면 | 버전별로 묶인 분석(수준·상태, 누르면 분석 상세)을 보고, 저장소 프로젝트는 배지(`NEW_COMMIT`: "새 커밋이 있어요 · 메시지 · 상대 시각", `UP_TO_DATE`: "최신 커밋을 분석했어요", `UNKNOWN`: 최신 커밋 정보가 있으면 그것만)를 본다. "최신 커밋 분석"(저장소)·"ZIP 다시 올리기"(ZIP)는 `/analyses/new?projectId=`로 가고, 요청 화면은 입력 방식·주소를 그 프로젝트로 고정해 `projectId`를 보낸다. 이름 바꾸기가 있다. 404면 "프로젝트를 찾을 수 없어요" |
| AC16 | 분석 주인이 FE에서 | 분석 상세를 열면 | 제목 근처에 "프로젝트 이름 · v번호" 링크(→ `/projects/:id`)가 보인다. 주인이 아닌 사람(공개·팀 공유)에게는 링크 없이 이름만 보인다 |

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다 (contract-checker, CI가 검사). Swagger Tag: `분석` (프로젝트는 분석을 묶는 단위라 같은 Tag. conventions 8절을 고치지 않는다). 패키지 `com.vibenote.analysis.project`.

### `POST /api/analyses` (변경: 선택 파트 `projectId`, 응답 `projectId`)

- 권한: 로그인 필요
- 요청 multipart: 기존 `file`|`sourceUrl`, `level`, `consent` + **`projectId`** (선택, 숫자 문자열)
- 응답 `202`

```json
{ "analysisId": 128, "status": "FETCHING", "projectId": 12 }
```

- 에러 (기존 + 추가)

| 상태 | 코드 | 언제 |
|---|---|---|
| 400 | COMMON_400 | `details.projectId`: 숫자가 아님 · "저장소 주소 프로젝트에는 ZIP을 올릴 수 없습니다." · "이 프로젝트의 저장소 주소와 다릅니다." · "ZIP 프로젝트는 ZIP 파일로만 새 버전을 만들 수 있습니다." |
| 404 | PROJECT_404 | 없거나 남의 프로젝트 ("프로젝트를 찾을 수 없습니다.") |

### `GET /api/projects`

- 권한: 로그인 필요 · 쿼리 `page`(1~), `size`(1~100, 기본 20) — `PageQuery`
- 응답 `200`

```json
{
  "items": [
    {
      "id": 12,
      "name": "todo-app",
      "sourceType": "URL",
      "sourceUrl": "https://github.com/octo/todo-app",
      "versionCount": 2,
      "latestVersion": {
        "id": 31,
        "number": 2,
        "commitSha": "9f2c4e1a7b3d5f60812c9e4a1b2c3d4e5f607182",
        "commitMessage": "feat: JWT 로그인",
        "committedAt": "2026-11-28T14:02:11+09:00",
        "createdAt": "2026-11-28T17:30:00+09:00"
      },
      "levels": ["BEGINNER", "JUNIOR"],
      "lastAnalyzedAt": "2026-11-28T17:30:00+09:00"
    }
  ],
  "page": 1, "size": 20, "total": 1
}
```

`latestVersion`은 버전이 없으면(첫 분석이 FETCHING 중·실패) null. `levels`는 삭제하지 않은 분석의 수준(중복 없이 BEGINNER→SENIOR 순). 페이지는 기존 `PageQuery`·`PageResponse` 그대로다 (page 1부터, size 최대 100).

### `GET /api/projects/{id}`

- 권한: 로그인 필요, 주인만
- 응답 `200`

```json
{
  "id": 12,
  "name": "todo-app",
  "sourceType": "URL",
  "sourceUrl": "https://github.com/octo/todo-app",
  "createdAt": "2026-11-27T10:00:00+09:00",
  "versions": [
    { "id": 31, "number": 2, "commitSha": "9f2c4e1a…", "commitMessage": "feat: JWT 로그인", "committedAt": "2026-11-28T14:02:11+09:00", "createdAt": "2026-11-28T17:30:05+09:00" },
    { "id": 30, "number": 1, "commitSha": null, "commitMessage": null, "committedAt": null, "createdAt": "2026-11-27T10:00:04+09:00" }
  ],
  "analyses": [
    { "id": 128, "versionId": 31, "level": "BEGINNER", "status": "DONE", "quizStatus": "READY", "title": "todo-app", "createdAt": "2026-11-28T17:30:00+09:00" },
    { "id": 120, "versionId": 30, "level": "JUNIOR", "status": "DONE", "quizStatus": "READY", "title": "todo-app", "createdAt": "2026-11-27T10:00:00+09:00" }
  ]
}
```

(예시의 `9f2c4e1a…`는 줄임 표시다. 실제 응답은 40자.) 버전에 삭제하지 않은 분석이 없으면 그 버전은 `versions`에서 뺀다 (번호는 그대로라 v1·v3처럼 빌 수 있다).

- 에러: 401 `AUTH_401` · 404 `PROJECT_404` (없음·남의 것·분석을 모두 삭제함)

### `PATCH /api/projects/{id}`

- 권한: 로그인 필요, 주인만 · 요청 `{ "name": "할 일 앱" }` · 응답 `200` `{ "id": 12, "name": "할 일 앱" }`
- 에러: 400 `COMMON_400` `details.name` ("프로젝트 이름은 1~100자로 입력하세요.") · 401 · 404 `PROJECT_404`

### `GET /api/projects/{id}/latest-commit`

- 권한: 로그인 필요, 주인만
- 응답 `200`

```json
{
  "status": "NEW_COMMIT",
  "latest": { "sha": "a1b2c3d4e5f60718293a4b5c6d7e8f9012345678", "message": "feat: 비밀번호 재설정", "committedAt": "2026-11-29T09:12:40+09:00" },
  "analyzedSha": "9f2c4e1a7b3d5f60812c9e4a1b2c3d4e5f607182",
  "checkedAt": "2026-11-29T10:00:00+09:00"
}
```

| status | 언제 | `latest` | `analyzedSha` |
|---|---|---|---|
| `NEW_COMMIT` | 최신 커밋 ≠ 분석한 최신 커밋 | 있음 | 있음 |
| `UP_TO_DATE` | 같음 | 있음 | 있음 |
| `UNKNOWN` | 조회 실패·비공개로 바뀜·분석한 커밋 없음(백필·AC3 버전뿐) | 조회됐으면 있음, 아니면 null | 있거나 null |
| `NOT_SUPPORTED` | ZIP 프로젝트 | null | null |

`checkedAt`은 외부 조회 시각(`NOT_SUPPORTED`면 null). 에러: 401 · 404 `PROJECT_404`.

### 기존 응답에 붙는 필드

- `GET /api/analyses/{id}`: `"project": { "id": 12, "name": "todo-app" }`, `"version": { "id": 31, "number": 2, "commitSha": "9f2c…", "commitMessage": "feat: JWT 로그인", "committedAt": "…" }` (버전 없으면 null). 공개·팀 공유로 보는 사람에게도 같은 값을 준다 (이름·커밋 메시지는 분석 내용과 같은 공개 범위).
- `GET /api/analyses` 항목: `"projectId": 12`, `"versionNumber": 2` (없으면 null)

### 외부 API (서버가 부름, 고정 base + 검증된 경로 조각만)

| 서비스 | 최신 커밋 | 그 커밋 아카이브 |
|---|---|---|
| GitHub | `GET {githubApiBase}/repos/{owner}/{repo}/commits?per_page=1` → `[0].sha`, `[0].commit.message`, `[0].commit.committer.date` | `GET …/repos/{owner}/{repo}/zipball/{sha}` |
| GitLab | `GET {gitlabApiBase}/projects/{id}/repository/commits?per_page=1` → `[0].id`, `[0].title`, `[0].committed_date` | `GET …/projects/{id}/repository/archive.zip?sha={sha}` |

응답 본문은 기존 `MAX_METADATA_BYTES`만 읽는다. SHA는 `^[0-9a-f]{40}$`가 아니면 조회 실패로 본다 (경로에 넣기 전 검증).

## 4. DB 변경

- Flyway: `V15__projects.sql` (`node scripts/next-migration.mjs`로 구현 직전에 다시 확인)

```sql
CREATE TABLE projects (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT       NOT NULL,
    source_type             ENUM('URL','ZIP') NOT NULL,
    source_url              VARCHAR(500) NULL COMMENT '정규화한 저장소 주소. ZIP은 NULL',
    name                    VARCHAR(100) NOT NULL,
    latest_commit_sha       CHAR(40)     NULL COMMENT '배지 조회 결과 (AC10 캐시)',
    latest_commit_message   VARCHAR(200) NULL,
    latest_committed_at     DATETIME     NULL,
    latest_checked_at       DATETIME     NULL,
    created_at / updated_at,
    PRIMARY KEY (id),
    UNIQUE KEY uk_projects_user_url (user_id, source_url),   -- ZIP은 NULL이라 여럿 가능
    FK user_id → users ON DELETE CASCADE
);
CREATE TABLE project_versions (
    id, project_id, number INT NOT NULL,
    commit_sha CHAR(40) NULL, commit_message VARCHAR(200) NULL, committed_at DATETIME NULL, created_at,
    UNIQUE (project_id, number), UNIQUE (project_id, commit_sha),
    FK project_id → projects ON DELETE CASCADE
);
ALTER TABLE analyses ADD COLUMN project_id BIGINT NULL, ADD COLUMN project_version_id BIGINT NULL, + FK·인덱스 (project_id, created_at);
-- 백필 (AC13): URL은 (user_id, source_url) GROUP BY로 프로젝트, ZIP은 분석마다 프로젝트(임시 열 legacy_analysis_id로 연결 후 DROP),
-- 삭제하지 않은 분석마다 버전 (ROW_NUMBER() OVER (PARTITION BY project_id ORDER BY created_at, id)), URL 이름은 SUBSTRING_INDEX(source_url,'/',-1)
ALTER TABLE analyses MODIFY project_id BIGINT NOT NULL;
```

- 동시성: 저장소 프로젝트 찾기·만들기는 요청 트랜잭션의 회원 행 잠금(`lockActiveMember`) 안에서 한다. 버전 번호는 FETCHING의 짧은 트랜잭션에서 `projects` 행을 `FOR UPDATE`로 잠그고 정한다 (같은 커밋을 두 수준으로 동시에 분석해도 버전은 하나). LLM·외부 요청은 트랜잭션 밖이다.
- 사용 테이블: `projects`, `project_versions`, `analyses`(project_id·project_version_id), `users`(잠금)
- 산출물: `docs/산출물/03-ERD.md`에 두 테이블과 관계 추가 (같은 PR)

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | `/projects` → `views/project/ProjectListView.vue` (SC-25 새로), `/projects/:id(\\d+)` → `views/project/ProjectDetailView.vue` (SC-26 새로). `router/routes/project.js` (로그인 필요) |
| 바꾸는 화면 | `AnalysisRequestView.vue`: `?projectId=`가 있으면 프로젝트를 불러와 입력 방식·주소를 고정하고 `projectId`를 보낸다. `AnalysisDetailView.vue`: 프로젝트·버전 링크 (AC16) |
| 컴포넌트 | `components/project/LatestCommitBadge.vue` (status 4가지), `components/project/ProjectVersionList.vue` (버전별 분석 묶음) |
| API 함수 | `src/api/projects.js`: `fetchProjects({page,size})`, `fetchProject(id)`, `fetchLatestCommit(id)`, `updateProject(id,{name})`. `src/api/analyses.js` `createAnalysis`에 `projectId` |
| 늦은 응답 | 새 화면의 조회는 `useLatestRequest`(V-4)를 쓴다. 배지는 상세와 따로 불러와 실패해도 상세를 가리지 않는다 |
| 상태별 UI | 로딩 / 빈 목록 / 404 / 에러 메시지 / 배지 조회 중 |
| 진입 | 앱 셸 내비에 "프로젝트" 항목 추가 (공통 파일이라 PR에서 @JoonseobKim 멘션), 내 분석 목록 머리에 "프로젝트별로 보기" 링크 |
| 산출물 | `docs/산출물/06-화면설계서.md`에 SC-25·SC-26 행과 흐름도, 스크린샷 (같은 PR) |

스타일은 F-29 공통 토큰·컴포넌트만 쓴다.

## 6. LLM (AI 기능만)

해당 없음. P-1a는 LLM 호출을 늘리지 않는다 (새 버전 분석은 지금처럼 전체 분석이고, 증분 재분석은 P-1b).

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1, AC4, AC5 | `analysis/AnalysisControllerTest#AC1_…`·`#AC4_…`·`#AC5_…` (기존 파일에 추가) | MockMvc 통합 |
| AC2, AC3, AC11 | `analysis/fetch/RepositoryArchiveFetcherTest` (가짜 GitHub·GitLab 서버로 커밋 조회·`zipball/{sha}`·실패 대체) + `analysis/pipeline/…FetchStageTest`(버전 찾기·만들기, 같은 커밋 두 수준) | 단위 + 통합 |
| AC6~AC10 | `analysis/project/ProjectControllerTest#AC6_…`~`#AC10_…` (가짜 서버 + 고정 `Clock`으로 5분 캐시) | MockMvc 통합 |
| AC12 | `AnalysisControllerTest#AC12_…`, `AnalysisRerunTest`(L-1a)에 프로젝트 유지 단언 추가 | MockMvc 통합 |
| AC13 | `analysis/project/ProjectBackfillMigrationTest` (별도 스키마에 Flyway target 14 → 데이터 넣기 → 15 적용 → 단언) | Testcontainers |
| AC14, AC15 | `views/project/__tests__/ProjectListView.spec.js`, `ProjectDetailView.spec.js`, `components/project/__tests__/LatestCommitBadge.spec.js`, `api/__tests__/projects.spec.js` | Vitest |
| AC15, AC16 | `views/__tests__/AnalysisRequestView.spec.js`(projectId 고정·전송), `AnalysisDetailView.spec.js`(링크·주인 아님) | Vitest |
| 화면 | ui-verifier `/projects`, Chrome으로 로그인 상태 `/projects/:id` (데스크톱·모바일) | 화면 점검 |

## 8. 범위 밖

- 증분 재분석(파일 해시 비교·설명 복사), 파일 원문 해시 공유 저장, "이번 버전에서 바뀐 부분" 보기, 면접 질문 갱신 → **P-1b**
- 팀 프로젝트(팀에 저장소 등록, 같은 버전·수준 분석 공유, `team_analyses` 대체) → **T-1**
- 같은 커밋·같은 수준 중복 분석 막기 (P-1a에서는 허용하고 오늘 한도 1회로 센다)
- 기본 브랜치 외 브랜치·태그 선택, 웹훅·폴링 자동 갱신, 사설 GitLab
- 프로젝트 삭제 API (분석을 모두 지우면 목록에서 사라진다), 분석을 다른 프로젝트로 옮기기, 프로젝트 공개
- 대시보드(SC-04) "최근 분석"을 프로젝트 기준으로 바꾸기

## 9. 결정 필요

- 없음 (11-27 사용자 선택: FE 최소 화면 포함, ZIP은 요청 때 프로젝트 선택)
- 구현 메모 (11-27): F-05a·F-05b 테스트의 AC 번호와 섞이지 않게 AC1·4·5는 `AnalysisProjectRequestTest`, AC2·3·11 수집기는 `fetch/RepositoryCommitFetchTest`로 나눈다. 커밋 조회 시간 상한은 `analysis.fetch.commit-timeout`(5s)
- 구현 메모 (11-27, reviewer): 실패한 내 ZIP 분석(SC-07)의 "ZIP 다시 올리기"도 SC-26 버튼처럼 `?projectId=`를 붙여 같은 프로젝트의 새 버전으로 보낸다 (안 붙이면 AC4대로 같은 이름의 ZIP 프로젝트가 하나 더 생긴다). 같은 커밋 동시 분석 잠금 테스트는 #257, 분석 api `signal` 전달은 #250(AM-1)

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/P-1a-project-versions`
- [x] Flyway `V15__projects.sql` + 백필 테스트
- [x] AC별 실패 테스트 작성 (red 확인)
- [x] BE 구현 (green): 요청 묶기(AC1·4·5 ✅) → FETCHING 커밋 조회·버전(AC2·3·11 ✅) → 프로젝트 API·배지(AC6~10 ✅) → AC12 ✅ (BE 끝)
- [x] FE 구현 (green): AC14·15·16 ✅, 고친 화면(요청·상세·목록) Tailwind·`useLatestRequest` 이전(#250)
- [x] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [x] contract-checker 통과 (경미 3건 반영)
- [x] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일) — 7개 라우트 문제 0건
- [x] reviewer 지적 사항 반영 (반드시 수정 0, 권장 5건 반영·2건 #257·#250)
- [x] 산출물 반영 (ERD 03, 화면 설계서 06 SC-25·26, 요구사항 정의서 01 P-1a 행, 스크린샷 SC-05·06·07·25·26)
- [ ] PR CI 초록, 셸 내비 변경 @JoonseobKim 멘션
- [ ] 상태 DONE (PR 머지 후)
