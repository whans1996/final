# F-20a 팀 (생성·초대 코드·멤버 관리 + SC-22·SC-23)

| 항목 | 값 |
|---|---|
| 상태 | DONE (PR #83) <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-20a (F-20 팀 챌린지의 1/2. 공동 분석 이해도·기간 목표는 F-20b) |
| 우선순위 | P2 |
| 영역 오너 | B 김준섭 (카드·소셜) |
| 관련 필수 요구사항 | ⑫ 확장 (팀 챌린지의 기반. 필수 ⑫는 F-14·F-15·F-24·F-25가 충족) |
| 의존 spec | F-02 (로그인), F-03 (회원 탈퇴 `UserWithdrawalService`, 탈퇴 회원 차단 `UserService.getActiveMember`). A 영역 코드는 바꾸지 않는다 |
| 관련 이슈 | 없음 |
| 승인 | 김준섭 2026-11-23 |

## 1. 목적

같은 프로젝트를 AI로 만드는 사람들이 **팀**을 만들어 함께 학습할 수 있게 한다 (기획서 문제 6 "팀 안에서 지식을 공유하기 어려움", 핵심 가치 3).
이번 spec은 그 바탕인 팀·멤버만 다룬다. 팀장이 팀을 만들고 **초대 코드**를 메신저로 공유하면, 코드를 받은 사람이 입력해 바로 가입한다. 팀장은 팀 정보 수정, 코드 재발급, 멤버 내보내기, 팀 삭제를 하고, 팀원은 탈퇴한다.
공동 분석의 모듈별 이해도 공유와 기간 목표(F-20b)는 이 spec이 머지된 뒤 별도 spec으로 쓴다 (11-23 결정: F-20을 a·b로 나눔).

**팀 규칙 (11-23 결정)**: 팀당 최대 **10명**(팀장 포함), 1인당 속한 팀 최대 **5개**(만든 팀 포함). 역할은 `OWNER`(만든 사람, 팀당 1명)·`MEMBER`. 팀장 위임은 없고, 팀장은 탈퇴할 수 없으며 팀을 삭제해야 한다. 팀 정보와 멤버 목록은 그 팀 멤버만 본다.

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_팀을_만들면_201과_초대_코드를_받고_팀장이_된다()`
> BE 테스트는 다른 테스트의 데이터에 기대지 않는다. 회원·팀을 테스트마다 직접 만들고 id로 확인한다.
> 권한 판단 순서는 모든 팀 API에서 같다: **401 → 404 `TEAM_404`(없는 팀·내가 멤버가 아닌 팀) → 403 `TEAM_403`(팀장 전용) → 400 검증**. 남의 팀은 존재 여부를 드러내지 않는다.

### 백엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 로그인 회원 U가 | `POST /api/teams` `{name:"  스프링 스터디  ", description:"F-05 분석 결과를 같이 공부합니다"}`를 보내면 | 201과 `{id, inviteCode}`를 받는다. 저장된 `name`·`description`은 앞뒤 공백을 뺀 값이다. 같은 트랜잭션에서 `team_members(팀, U, OWNER)` 한 행이 생긴다. `inviteCode`는 `ABCDEFGHJKLMNPQRSTUVWXYZ23456789`(헷갈리는 I·O·0·1 제외 32자)에서 `SecureRandom`으로 뽑은 8자이고 `teams.invite_code`는 UNIQUE다. 생성한 코드가 이미 있으면(UNIQUE 위반) 새로 뽑아 최대 3회까지 다시 시도한다 (`InviteCodeGenerator`를 주입받아 테스트에서 충돌을 재현한다) |
| AC2 | 로그인 회원이 | `name`이 없음·공백만·31자이거나 `description`이 201자(길이는 앞뒤 공백 제외)이면 | 400 `COMMON_400`과 `details`의 `name`·`description`을 받고 팀·멤버는 생기지 않는다. 이름 30자·소개 200자 정확히는 성공한다. `description`이 없거나 공백만이면 `null`로 저장한다. 팀 이름은 중복을 허용한다 |
| AC3 | U가 팀 T1(팀장, 11/24 가입)·T2(팀원, 11/25 가입)에 속하고 V만 속한 T3이 있을 때 | U가 `GET /api/users/me/teams`를 요청하면 | 200과 배열 `[T2, T1]`(내 가입 시각 `joined_at DESC, team id DESC`)을 받는다. 항목은 3절 `MyTeamResponse`(`id, name, description, myRole, memberCount, ownerNickname, joinedAt`)이고 T3은 없다. 속한 팀이 없으면 `[]`다. 페이지 없이 전부 준다 (최대 5개) |
| AC4 | T의 멤버 U가 | `GET /api/teams/{T}`를 요청하면 | 200과 3절 `TeamDetailResponse`를 받는다. `members`는 팀장이 맨 앞이고 나머지는 `joined_at ASC, user id ASC`이며 항목은 `userId, nickname, level, role, joinedAt`이다. `myRole`은 요청한 사람의 역할이다. `inviteCode`는 **팀장에게만** 값이 있고 팀원에게는 `null`이다. 멤버가 아닌 회원이 요청하거나 없는 id(`999999`)면 404 `TEAM_404` "팀을 찾을 수 없습니다."다 |
| AC5 | 팀 T의 초대 코드가 `K7M2QX9P`일 때 | 멤버가 아닌 W가 `POST /api/teams/join` `{inviteCode:" k7m2qx9p "}`를 보내면 | 200과 `{teamId: T}`를 받고 `team_members(T, W, MEMBER)`가 생긴다 (코드는 앞뒤 공백을 빼고 대문자로 바꿔 비교). 없는 코드(재발급으로 무효가 된 코드 포함)면 404 `TEAM_001` "유효하지 않은 초대 코드입니다.", 이미 T의 멤버면 409 `TEAM_409` "이미 가입한 팀입니다.", `inviteCode`가 없거나 공백만이면 400 `COMMON_400`(`details.inviteCode`)이고 멤버는 바뀌지 않는다 |
| AC6 | 멤버가 10명인 팀 / 이미 5개 팀에 속한 회원 W가 | W가 가입하면 / W가 `POST /api/teams`로 팀을 만들면 | 409 `TEAM_002` "팀 인원이 가득 찼습니다. (최대 10명)" / 409 `TEAM_003` "더 이상 팀에 속할 수 없습니다. (최대 5개)"를 받고 아무것도 생기지 않는다. 두 조건이 함께면 `TEAM_003`을 먼저 본다. 가입은 `teams` 행을 `SELECT … FOR UPDATE`로 잠근 뒤 인원을 세므로, 9명인 팀에 두 회원이 **동시에** 가입하면 정확히 한 명만 200이고 다른 한 명은 `TEAM_002`다 (`TeamJoinConcurrencyTest`). 1인 5개 상한은 순차 요청 기준으로 보장한다 (같은 회원의 동시 가입 경합은 범위 밖) |
| AC7 | T의 팀장 U가 | `PUT /api/teams/{T}` `{name:"바뀐 이름", description:null}`을 보내면 / `POST /api/teams/{T}/invite-code`를 보내면 | 200과 바뀐 `TeamDetailResponse`(이름·소개를 요청 값으로 통째로 바꿈, 검증은 AC2와 같음) / 200과 `{inviteCode: 새 코드}`를 받는다. 재발급 뒤 이전 코드로 가입하면 404 `TEAM_001`이다. 팀원이 두 요청을 보내면 403 `TEAM_403` "팀장만 할 수 있습니다.", 멤버가 아니면 404 `TEAM_404`이고 팀은 바뀌지 않는다. 403·404는 검증(400)보다 먼저다 (팀원이 빈 이름을 보내도 403) |
| AC8 | T에 팀장 U와 팀원 V·W가 있을 때 | V가 `DELETE /api/teams/{T}/members/me`를 보내면 / U가 `DELETE /api/teams/{T}/members/{W}`를 보내면 | 둘 다 204이고 그 멤버 행이 지워진다 (이후 그 회원의 `GET /api/teams/{T}`는 404, 같은 코드로 다시 가입할 수 있다). 팀장이 `members/me`나 `members/{자기 id}`를 보내면 400 `TEAM_004` "팀장은 팀을 떠날 수 없습니다. 팀을 삭제해 주세요.", 팀장이 T의 멤버가 아닌 회원 id·없는 id를 보내면 404 `TEAM_005` "팀원을 찾을 수 없습니다.", 팀원이 다른 멤버를 내보내려 하면 403 `TEAM_403`이다 |
| AC9 | T의 팀장 U가 | `DELETE /api/teams/{T}`를 보내면 | 204이고 팀과 모든 멤버 행이 지워진다 (`team_members` FK CASCADE). 이후 멤버였던 회원들의 `GET /api/users/me/teams`에 T가 없고 T의 코드로 가입하면 404 `TEAM_001`이다. 팀원이 보내면 403 `TEAM_403`, 멤버가 아니면 404 `TEAM_404`다 |
| AC10 | U가 팀장인 T1(팀원 V)과 팀원인 T2가 있을 때 | U가 회원 탈퇴(`DELETE /api/users/me`, F-03)에 성공하면 | 같은 트랜잭션에서 U의 모든 멤버 행이 지워지고 U가 팀장인 팀(T1)은 삭제된다. V의 내 팀 목록에서 T1이 사라지고, T2의 멤버 목록에 U가 없다. 탈퇴가 비밀번호 불일치(`USER_001`)로 실패하면 팀·멤버는 그대로다. `UserWithdrawalService.withdraw`가 `TeamService.removeAllOf(userId)`를 부른다 (공통 파일 → PR에서 @whans1996 멘션) |
| AC11 | 토큰 없음·만료·형식 오류이거나 탈퇴한 회원의 남은 Access 토큰일 때 | 3절의 모든 팀 API를 요청하면 | 401 `AUTH_401`이고 아무것도 바뀌지 않는다 (공개 경로 없음) |

### 프론트엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC12 | 로그인 회원이 | SC-22 `/teams`를 열면 | 헤더 메뉴에 "팀"(로그인 회원에게만, 피드 다음)이 있고, 내 팀 카드 목록이 보인다. 카드는 팀 이름, 역할 칩("팀장"·"팀원"), "{memberCount}명", 소개(없으면 생략)이고 누르면 SC-23으로 간다. 0개면 "아직 속한 팀이 없습니다. 팀을 만들거나 초대 코드로 가입해 보세요.", 실패하면 서버 `message`와 "다시 시도". 비로그인은 `/login?redirect=/teams`로 간다 |
| AC13 | SC-22에서 | "팀 만들기"에 이름·소개를 입력해 "만들기"를 누르면 / "초대 코드로 가입"에 코드를 입력해 "가입"을 누르면 | 만들기는 `createTeam` 후 SC-23 `/teams/{id}`로, 가입은 `joinTeam` 후 응답 `teamId`의 SC-23으로 간다. 이름은 30자·소개는 200자·코드는 8자에서 더 입력되지 않고, 코드는 입력하는 대로 대문자로 보인다. 이름이 공백뿐이거나 코드가 비면 해당 버튼이 비활성이고, 요청 중에도 비활성이다. `/teams?code=K7M2QX9P`로 들어오면 코드 입력란을 미리 채운다 (자동 가입은 하지 않는다). 400이면 `details`의 필드 메시지를 해당 입력 아래에, `TEAM_001`·`TEAM_002`·`TEAM_003`·`TEAM_409`는 그 폼 아래에 서버 `message`를 보여 주고 입력값은 유지한다 |
| AC14 | 팀 T의 멤버가 | SC-23 `/teams/:id(\d+)`를 열면 | 팀 이름·소개, "멤버 {n}/10", 멤버 목록(닉네임 → `/users/{userId}`, 수준 배지, 팀장 칩, 가입일 `YYYY-MM-DD`)이 보인다. **팀장에게만** 초대 코드 상자(코드, "코드 복사", "초대 링크 복사"(`{location.origin}/teams?code={코드}`), "재발급")와 "수정"·"팀 삭제", 팀원 행마다 "내보내기"가 보인다. **팀원에게만** "팀 탈퇴"가 보인다. 복사가 성공하면 "복사했습니다", 클립보드를 쓸 수 없으면 "복사하지 못했습니다. 코드를 직접 복사해 주세요."를 보여 준다. 404면 "팀을 찾을 수 없습니다." + "← 내 팀으로"(SC-22) 링크, 그 밖의 실패는 서버 `message`와 "다시 시도" |
| AC15 | SC-23에서 | 팀장이 "수정"을 눌러 이름·소개를 바꿔 저장하면 / "재발급"·"내보내기"·"팀 삭제"를 누르면 / 팀원이 "팀 탈퇴"를 누르면 | 수정은 같은 화면의 입력 폼으로 바뀌고(제한·비활성 규칙은 AC13과 같음) `updateTeam` 응답으로 화면을 갱신한다. 재발급·내보내기·삭제·탈퇴는 확인 대화상자("이전 코드로는 더 가입할 수 없습니다." / "{닉네임} 님을 팀에서 내보낼까요?" / "팀과 멤버 정보가 모두 삭제됩니다." / "팀에서 탈퇴할까요?")에서 확인해야 요청하고, 취소하면 아무 요청도 보내지 않는다. 재발급은 새 코드를 보여 주고, 내보내기는 멤버 목록을 다시 불러오며, 삭제·탈퇴는 SC-22로 간다. 실패하면 서버 `message`를 보여 준다. SC-22·SC-23 모두 375px 폭에서 페이지 가로 스크롤이 없다 (NF-02) |

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다 (contract-checker, CI가 검사). Swagger Tag: **`팀`** (새 Tag, `docs/conventions.md` 8절 목록에 추가). 새 패키지 `com.vibenote.team`: `TeamController`, `TeamService`, `TeamMapper`(+`mapper/team/TeamMapper.xml`), `InviteCodeGenerator`, `team/domain`, `team/dto`. 모든 엔드포인트는 **로그인 필요**이고, 서비스는 `UserService.getActiveMember`로 탈퇴 회원을 막는다 (F-17과 같음).

### `POST /api/teams`

- 요청

```json
{ "name": "스프링 스터디", "description": "F-05 분석 결과를 같이 공부합니다" }
```

- 응답 `201`

```json
{ "id": 12, "inviteCode": "K7M2QX9P" }
```

- 에러: 400 `COMMON_400`(name·description) · 401 `AUTH_401` · 409 `TEAM_003`

### `GET /api/users/me/teams`

- 응답 `200` (`MyTeamResponse[]`)

```json
[
  {
    "id": 12,
    "name": "스프링 스터디",
    "description": "F-05 분석 결과를 같이 공부합니다",
    "myRole": "OWNER",
    "memberCount": 3,
    "ownerNickname": "vibecoder",
    "joinedAt": "2026-11-24T10:00:00+09:00"
  }
]
```

- 에러: 401 `AUTH_401`

### `GET /api/teams/{id}`

- 응답 `200` (`TeamDetailResponse`)

```json
{
  "id": 12,
  "name": "스프링 스터디",
  "description": "F-05 분석 결과를 같이 공부합니다",
  "myRole": "OWNER",
  "inviteCode": "K7M2QX9P",
  "memberCount": 3,
  "maxMembers": 10,
  "createdAt": "2026-11-24T10:00:00+09:00",
  "members": [
    { "userId": 3, "nickname": "vibecoder", "level": "JUNIOR", "role": "OWNER", "joinedAt": "2026-11-24T10:00:00+09:00" },
    { "userId": 7, "nickname": "learner", "level": "BEGINNER", "role": "MEMBER", "joinedAt": "2026-11-24T11:30:00+09:00" }
  ]
}
```

- 팀원이 요청하면 `"myRole": "MEMBER"`, `"inviteCode": null`
- 에러: 401 `AUTH_401` · 404 `TEAM_404`

### `PUT /api/teams/{id}` (팀장)

- 요청: `POST /api/teams`와 같은 형식. 응답 `200` `TeamDetailResponse`
- 에러: 400 `COMMON_400` · 401 · 403 `TEAM_403` · 404 `TEAM_404`

### `DELETE /api/teams/{id}` (팀장)

- 응답 `204`. 에러: 401 · 403 `TEAM_403` · 404 `TEAM_404`

### `POST /api/teams/{id}/invite-code` (팀장, 재발급)

- 요청 본문 없음. 응답 `200` `{ "inviteCode": "P4ZR8NWE" }`
- 에러: 401 · 403 `TEAM_403` · 404 `TEAM_404`

### `POST /api/teams/join`

- 요청 `{ "inviteCode": "K7M2QX9P" }` → 응답 `200` `{ "teamId": 12 }`
- 에러: 400 `COMMON_400`(inviteCode) · 401 · 404 `TEAM_001` · 409 `TEAM_409`·`TEAM_002`·`TEAM_003`

### `DELETE /api/teams/{id}/members/me` (팀원 탈퇴)

- 응답 `204`. 에러: 400 `TEAM_004`(팀장) · 401 · 404 `TEAM_404`

### `DELETE /api/teams/{id}/members/{userId}` (팀장, 내보내기)

- `userId`는 `\d+`라 `me`와 겹치지 않는다. 응답 `204`
- 에러: 400 `TEAM_004`(자기 자신) · 401 · 403 `TEAM_403` · 404 `TEAM_404`·`TEAM_005`

### 에러 코드 (`ErrorCode`의 `// ── TEAM (B F-20) ──` 블록, conventions 3절 표에 `TEAM` 행 추가)

| 상태 | 코드 | 메시지 | 언제 |
|---|---|---|---|
| 404 | TEAM_404 | 팀을 찾을 수 없습니다. | 없는 팀, 내가 멤버가 아닌 팀 |
| 403 | TEAM_403 | 팀장만 할 수 있습니다. | 팀원이 수정·삭제·재발급·내보내기 |
| 404 | TEAM_001 | 유효하지 않은 초대 코드입니다. | 없는 코드, 재발급·삭제로 무효가 된 코드 |
| 409 | TEAM_409 | 이미 가입한 팀입니다. | 이미 멤버인 팀에 가입 |
| 409 | TEAM_002 | 팀 인원이 가득 찼습니다. (최대 10명) | 10명인 팀에 가입 |
| 409 | TEAM_003 | 더 이상 팀에 속할 수 없습니다. (최대 5개) | 5개 팀에 속한 회원의 생성·가입 |
| 400 | TEAM_004 | 팀장은 팀을 떠날 수 없습니다. 팀을 삭제해 주세요. | 팀장의 탈퇴·자기 내보내기 |
| 404 | TEAM_005 | 팀원을 찾을 수 없습니다. | 내보낼 대상이 그 팀 멤버가 아님 |

## 4. DB 변경

- Flyway: `V{n}__teams.sql` (번호는 구현 시작 때 `git merge origin/develop` 후 `node scripts/next-migration.mjs`로. 지금 develop 기준 V8)

```sql
-- F-20a 팀: 팀장이 초대 코드로 멤버를 모은다. team_challenges는 F-20b에서 추가한다.
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
```

- `owner_id`는 조회 편의용이고 역할의 정본은 `team_members.role`이다. 둘은 생성 때 같이 쓰고 이후 바뀌지 않는다 (위임 없음).
- 사용 테이블: `teams`·`team_members`(전부), `users`(닉네임·수준·상태 조회는 `UserService`를 통해, 멤버 목록 JOIN은 `TeamMapper.xml`에서 `users`를 읽기만 함)

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | `frontend/src/router/routes/team.js`(새 파일): `/teams` → `views/team/TeamListView.vue` (SC-22, `meta.nav: {label:'팀', order: 8, show:'user'}`, `requiresAuth`), `/teams/:id(\\d+)` → `views/team/TeamDetailView.vue` (SC-23, `requiresAuth`) |
| 컴포넌트 | `components/team/TeamCard.vue`(목록 카드), `TeamForm.vue`(만들기·수정 공용: 이름·소개, 제한·비활성 규칙), `InviteCodeBox.vue`(코드·복사 2종·재발급), `TeamMemberList.vue`(멤버 행, 팀장에게 내보내기). 공용 `EmptyState` 재사용, 확인 대화상자는 기존 화면처럼 `window.confirm`(F-17 `PostDetailView`), 수준 배지는 `FollowUserItem.vue`의 표시 방식을 따른다 |
| store | 없음 (화면 로컬 상태. `auth` store로 로그인 여부만 확인) |
| API 함수 | `src/api/teams.js`: `fetchMyTeams()`, `createTeam(body)`, `fetchTeam(id)`, `updateTeam(id, body)`, `deleteTeam(id)`, `regenerateInviteCode(id)`, `joinTeam(inviteCode)`, `leaveTeam(id)`, `removeTeamMember(id, userId)` |
| 상태별 UI | 로딩 "불러오는 중…" / 빈 목록(AC12) / 404(AC14) / 그 밖의 실패는 서버 `message` + "다시 시도" |

## 6. LLM (AI 기능만)

해당 없음.

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1~AC9, AC11 | `team/TeamApiTest#AC1_…`~`AC11_…` (trim·코드 형식·OWNER 행, 검증 경계 30/31·200/201, 목록 순서·남의 팀 제외, 상세 정렬·코드 노출 범위·404, 가입 대소문자·공백·무효·중복, 상한 2종·우선순위, 수정·재발급·이전 코드 무효, 탈퇴·내보내기·TEAM_004·005, 삭제 CASCADE, 401·탈퇴 회원 토큰, 판단 순서 403 > 400) | MockMvc 통합 |
| AC1 (코드 충돌 재시도) | `team/TeamServiceTest#AC1_코드가_겹치면_다시_뽑는다` (고정 시퀀스 `InviteCodeGenerator` 스텁: 기존 코드 → 새 코드), `team/InviteCodeGeneratorTest` (8자·허용 문자만) | 통합·단위 |
| AC6 (동시 가입) | `team/TeamJoinConcurrencyTest` (9명 팀에 두 스레드 동시 가입 → 성공 1·`TEAM_002` 1, 최종 10명) | 통합 |
| AC10 | `user/UserWithdrawalApiTest`(또는 기존 F-03 탈퇴 테스트 클래스)에 `AC10_F20a_탈퇴하면_팀장인_팀은_삭제되고_멤버_행이_지워진다`, 비밀번호 불일치면 그대로 | MockMvc 통합 |
| AC12, AC13 | `src/views/team/__tests__/TeamListView.spec.js` (목록·빈 상태·실패, 만들기·가입 이동, 버튼 비활성, `?code=` 미리 채움·대문자, 에러 위치·입력 유지) | Vitest |
| AC14, AC15 | `src/views/team/__tests__/TeamDetailView.spec.js` (팀장·팀원별 보이는 버튼, 복사 성공·실패, 404, 확인 취소 시 요청 없음, 재발급·내보내기·삭제·탈퇴 후 동작), `src/components/team/__tests__/TeamForm.spec.js` | Vitest |
| AC12~AC15 (화면) | ui-verifier: SC-22·SC-23 데스크톱 1280·모바일 375 | Playwright |

## 8. 범위 밖

- **F-20b**: 팀에 분석 공유, 팀원의 공유 분석 열람·퀴즈 풀이(A 영역 `quiz`·`analysis` 권한 변경), 모듈(레이어)별 이해도 집계, 기간 목표(`team_challenges`)
- 팀장 위임, 닉네임으로 초대·수락 대기, 초대 코드 만료 시간, 공개 팀 검색
- 팀 활동의 피드 기록(`activities`), 프로필에 소속 팀 표시, 알림
- 같은 회원이 동시에 여러 팀에 가입할 때의 5개 상한 경합 (AC6)

## 9. 결정 필요

- 없음 (11-23 사용자 결정: a·b 분할, 초대 코드 방식, 기본 팀 규칙)

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/F-20a-team`
- [x] Flyway 마이그레이션
- [x] AC별 실패 테스트 작성 (red 확인)
- [x] BE 구현 (green)
- [x] FE 구현 (green)
- [x] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [x] `docs/conventions.md` 3절 `TEAM` 행·8절 `팀` Tag, `.github/CODEOWNERS`에 `team` 패키지·mapper·FE 폴더·`api/teams.js`·`routes/team.js` (B)
- [x] contract-checker 통과
- [x] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일)
- [x] reviewer 지적 사항 반영 (반드시 수정 0, 권장 3건 b41b717, Codex P2 1건 → #84)
- [x] 산출물 반영: 화면 설계서 SC-22·SC-23, ERD(`teams`·`team_members`), 요구사항 정의서 F-20 행(F-20a·F-20b)
- [x] PR CI 초록 (공통 파일 `UserWithdrawalService`·`ErrorCode`·conventions 변경 → @whans1996 멘션)
- [x] 상태 DONE (PR #83)
