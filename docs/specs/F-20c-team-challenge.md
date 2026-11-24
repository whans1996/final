# F-20c 팀 기간 목표 (공유 분석 퀴즈 정답률 목표 + 달성 판정)

| 항목 | 값 |
|---|---|
| 상태 | DONE (PR #98) <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-20c (F-20 팀 챌린지의 3/3. 팀·초대 F-20a(DONE), 공동 분석 이해도 F-20b(DONE)) |
| 우선순위 | P2 |
| 영역 오너 | B 김준섭 (카드·소셜) |
| 관련 필수 요구사항 | ⑫ 확장 (팀 챌린지. 필수 ⑫는 F-14·F-15·F-24·F-25가 충족) |
| 의존 spec | F-20a 팀(`TeamService`·`team_members`·`TEAM_403`), F-20b 공유 분석(`team_analyses`·`TeamAnalysisService`·SC-23 `TeamAnalysisList`·SC-24 `TeamUnderstandingView`), F-14 퀴즈(`quiz_attempts`). **A 영역 변경 없음** (`quiz_attempts`·`quizzes`·`analyses`는 읽기만) |
| 관련 이슈 | #89 (SC-24 제목이 없을 때 SC-23과 다른 대체 문구) → AC10·AC13. 구현 PR에 `Closes #89` |
| 승인 | 김준섭 2026-11-24 |

## 1. 목적

F-20b로 팀은 공유 분석의 모듈별 이해도를 본다. F-20c는 여기에 **기간과 목표**를 붙여 "이번 주 안에 팀 전원이 spring-realworld 퀴즈 80% 이상" 같은 팀 목표를 세우고, 누가 달성했고 누가 남았는지 보게 한다 (기획서 핵심 가치 3, 학습 정착).

**규칙 (11-24 결정)**:
- 목표 = **팀에 공유된 분석 1개 + 기간(시작일~종료일, Asia/Seoul 날짜, 양끝 포함) + 목표 정답률(1~100%)**. **팀장만** 만들고 지운다. 수정은 없다 (지우고 다시 만든다).
- 멤버의 성적 = 그 분석의 퀴즈를 **기간 안에** 푼 풀이 중 가장 최근 것(`created_at DESC, id DESC`). 멤버 달성 = 그 풀이의 `score × 100 ≥ 목표 정답률 × total` (반올림하지 않는다).
- **팀 달성 = 지금 멤버 전원이 달성**. 기간 중에 들어온 멤버도 세고, 떠난 멤버는 세지 않는다. 결과는 저장하지 않고 조회할 때마다 계산한다 (종료 뒤 멤버가 바뀌면 결과도 바뀐다).
- 상태: 오늘 < 시작일 `UPCOMING`(예정), 시작일 ≤ 오늘 ≤ 종료일 `ONGOING`(진행 중), 오늘 > 종료일 `ENDED`(종료). 진행 중에 전원이 달성하면 그때부터 `achieved: true`다.
- 같은 분석에는 **끝나지 않은(예정·진행 중) 목표가 하나**만 있을 수 있다 (공유 분석 상한 10개가 곧 동시 목표 상한). 종료된 목표는 기록으로 남고, 팀당 목표는 **전체 20개**까지다.
- 공유가 해제되면(해제·공유자의 탈퇴·내보내기·회원 탈퇴) 그 팀의 그 분석 목표도 함께 지워진다 (FK CASCADE). 분석이 soft delete되면 행은 남기고 조회·상한에서 거른다 (F-20b와 같다).

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_팀장이_공유_분석에_목표를_만들면_201을_받는다()`
> BE 테스트는 다른 테스트의 데이터나 시드 카드에 기대지 않는다. 회원·팀·분석(`DONE`)·퀴즈·공유·풀이를 테스트마다 직접 넣고 id로 확인한다. 오늘은 `@TestBean Clock`으로 **2026-11-24(Asia/Seoul)**에 고정한다 (`ReviewPlanTestSupport`와 같은 방식). 풀이 시각은 `quiz_attempts.created_at`을 직접 넣는다.
> 권한 판단 순서는 새 API에서 같다: **401 → 404 `TEAM_404`(없는 팀·내가 멤버가 아닌 팀) → 403 `TEAM_403`(팀장 전용) → 400 검증 → 대상 판단(404 `TEAM_006`·`TEAM_013`) → 409** (F-20a 순서에 F-20b 대상 판단을 이었다).

### 백엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 팀 T(팀장 U, 멤버 V)에 공유 분석 A가 있을 때 | U가 `POST /api/teams/{T}/challenges` `{analysisId: A, startDate: "2026-11-25", endDate: "2026-12-01", targetRate: 80}`을 보내면 | 201과 3절 `TeamChallengeResponse`를 받는다: `status` `UPCOMING`, `memberCount` 2, `solvedMemberCount` 0, `achievedMemberCount` 0, `achieved` false, `members` 2명 모두 `attempt: null`·`achieved: false`. `team_challenges` 한 행이 생긴다. `startDate`가 오늘(11-24)이면 `status`는 `ONGOING`이다 |
| AC2 | 팀장 U가 | 요청 본문이 잘못되면 | 400 `COMMON_400`이고 `details`에 필드별 사유가 있다: `analysisId` 없음·1 미만, `startDate`·`endDate` 없음·형식 오류, `targetRate` 없음·0·101, `startDate`가 오늘보다 앞(11-23), `endDate`가 `startDate`보다 앞, 기간이 30일 초과(`endDate` = `startDate` + 30일). 경계값 `targetRate` 1·100, `startDate` = 오늘, 기간 정확히 30일(`endDate` = `startDate` + 29일)은 통과한다. 행은 생기지 않는다 |
| AC3 | 팀 T에서 | 대상·상한이 맞지 않으면 | T에 공유되지 않은 분석·soft delete된 공유 분석 → 404 `TEAM_006`. A에 끝나지 않은 목표(종료일 ≥ 오늘)가 이미 있음 → 409 `TEAM_011` "이 분석에는 이미 예정되거나 진행 중인 팀 목표가 있습니다.". A의 목표가 모두 종료됐으면 새로 만들 수 있다. T의 목표가 이미 20개(soft delete된 분석의 목표는 세지 않음) → 409 `TEAM_012` "팀 목표는 최대 20개입니다. 종료된 목표를 지워 주세요.". 멤버 V가 보내면 본문이 잘못돼도 403 `TEAM_403`, 멤버가 아니면 404 `TEAM_404`. 어느 경우든 행은 생기지 않는다. 상한·중복은 `teams` 행을 `SELECT … FOR UPDATE`로 잠근 뒤 센다 (동시성 테스트는 범위 밖) |
| AC4 | T에 목표 G1(A1, 11-20~11-23, 종료), G2(A2, 11-24~11-30, 진행 중), G3(A1, 11-26~11-28, 예정), G4(A3, 11-22~11-26, 진행 중), G5(A4 soft delete)가 있을 때 | 멤버가 `GET /api/teams/{T}/challenges`를 요청하면 | 200과 배열 `[G4, G2, G3, G1]`을 받는다 (진행 중은 종료일 ASC → 예정은 시작일 ASC → 종료는 종료일 DESC, 같으면 목표 id DESC). G5는 없다. `?analysisId=A1`이면 `[G3, G1]`, 목표가 없으면 `[]`. `analysisId`가 숫자가 아니거나 1 미만이면 400 `COMMON_400`. 멤버가 아니면 404 `TEAM_404` |
| AC5 | 진행 중인 목표 G(A, 11-20~11-24, 목표 80%, 문항 5개)와 T의 지금 멤버 U(팀장)·V·W, 떠난 회원 X가 있을 때. U: 11-19 5/5, 11-21 3/5(기간 안 최근) · V: 11-20 00:00:00 4/5 · W: 풀이 없음 · X: 11-22 5/5 | 목록을 요청하면 | G의 `memberCount` 3, `solvedMemberCount` 2(U·V), `achievedMemberCount` 1(V, 80% 경계 포함), `achieved` false. `members`는 F-20a 상세와 같은 순서(팀장 먼저, `joined_at ASC, user id ASC`)이고 U `attempt {score:3,total:5,…}`·`achieved` false, V `achieved` true, W `attempt null`·`achieved` false, X는 없다. U가 11-24 23:59:59에 5/5를 풀면 U가 달성되고(종료일 끝까지 포함), 11-25 00:00:00 풀이는 세지 않는다. W도 기간 안에 4/5 이상이면 `achieved` true. 분석 주인이 아닌 멤버의 풀이도 똑같이 센다 |
| AC6 | 목표 기간이 11-25~11-30 / 11-24~11-24 / 11-17~11-23일 때 (오늘 11-24) | 목록을 요청하면 | `status`가 `UPCOMING` / `ONGOING` / `ENDED`다. `ENDED` 목표도 기간 안의 풀이로 같은 방식으로 `achieved`를 계산한다 (전원 달성이면 true). `UPCOMING`은 기간 안의 풀이가 없어 항상 `achieved` false다 |
| AC7 | T에 목표 G가 있을 때 | 팀장 U가 `DELETE /api/teams/{T}/challenges/{G}`를 보내면 / 멤버 V가 보내면 / 없는 목표·다른 팀의 목표·soft delete된 분석의 목표 id면 | 상태와 상관없이 204이고 `team_challenges`의 G만 지워진다 (`quiz_attempts` 그대로) / 403 `TEAM_403` / 404 `TEAM_013` "팀 목표를 찾을 수 없습니다.". 멤버가 아니면 404 `TEAM_404` |
| AC8 | 회원 V가 T에 공유한 A에 목표 G1이 있고, T의 다른 분석 B에 G2, V가 T2에도 A를 공유해 목표 G3이 있을 때 | A의 공유를 해제하면 / V가 T를 탈퇴하거나 내보내지면 / 팀장이 T를 삭제하면 / V가 회원 탈퇴하면 / A가 soft delete되면 | G1만 지워진다 / G1만 지워진다(T2의 G3은 남음) / T의 목표가 모두 지워진다 / G1·G3이 지워진다 (모두 `team_challenges(team_id, analysis_id)` → `team_analyses` FK CASCADE, 서비스 코드 추가 없음) / 행은 남고 목록·상한·삭제(404 `TEAM_013`)에서 빠진다 |
| AC9 | 토큰 없음·만료·형식 오류이거나 탈퇴한 회원의 남은 Access 토큰일 때 | 3절의 새 API 3개를 요청하면 | 401 `AUTH_401`이고 아무것도 바뀌지 않는다 |
| AC10 | (#89) 공유 분석 A의 제목이 없을 때 | `GET /api/teams/{T}/analyses/{A}/understanding`을 요청하면 | 응답에 `projectName`이 있다 (`title` null). 기존 필드는 그대로다 |

### 프론트엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC11 | 팀 T의 멤버가 | SC-23 `/teams/{T}`를 열면 | 멤버 목록과 공유 분석 영역 사이에 "팀 목표" 영역이 보인다. 항목마다 "{분석 제목 또는 projectName} · 정답률 {targetRate}% 이상", "MM-DD ~ MM-DD", 상태 칩(예정 "D-{n}" / 진행 중 "{n}일 남음"(종료일이면 "오늘 마감") / 종료), 진행 "{achievedMemberCount}/{memberCount}명 달성", `achieved`면 "달성" 칩, 종료이고 미달성이면 "미달성" 칩, "이해도 보기"(→ SC-24). 팀장에게는 "삭제"가 있고 확인 대화상자 "이 팀 목표를 삭제할까요?"에서 확인해야 요청하고 성공하면 목록을 다시 불러온다. 0개면 "아직 팀 목표가 없습니다." (팀장에게는 "공유 분석의 이해도 화면에서 목표를 만들 수 있습니다."를 덧붙임). 조회가 실패하면 그 영역에만 서버 `message`와 "다시 시도". 공유 해제(F-20b AC10) 확인 문구는 "이 분석의 공유를 해제할까요? 팀원의 풀이 기록은 남고, 이 분석의 팀 목표는 함께 삭제됩니다."로 바꾸고, 해제에 성공하면 팀 목표 목록도 다시 불러온다 |
| AC12 | 팀 T의 멤버가 | SC-24 `/teams/{T}/analyses/{A}`를 열면 | 이해도 위에 "팀 목표" 영역이 보인다 (`fetchTeamChallenges(T, {analysisId: A})`). 예정·진행 중 목표가 있으면 그것을, 없으면 가장 최근 종료 목표를 보여 준다: "정답률 {targetRate}% 이상 · MM-DD ~ MM-DD", 상태 칩, "{achievedMemberCount}/{memberCount}명 달성", 멤버마다 닉네임·"{score}/{total} ({정답률}%)" 또는 "기간 안 풀이 없음"·달성 표시. 목표가 없으면 "이 분석에 팀 목표가 없습니다." **팀장**이고 예정·진행 중 목표가 없으면 "목표 만들기" 폼(시작일 기본 오늘, 종료일 기본 오늘+6일, 목표 정답률 기본 80, 숫자 1~100)이 보이고, 제출하면 `createTeamChallenge` 후 목표 영역을 다시 불러온다. 요청 중에는 제출 버튼이 비활성이다. 400은 `details`를 필드 아래에, `TEAM_011`·`TEAM_012`·`TEAM_006`은 폼 아래에 서버 `message`로 보여 주고 입력을 유지한다. 팀원에게는 폼이 없다. 목표 조회가 실패하면 그 영역에만 서버 `message`와 "다시 시도"(이해도는 그대로 보임) |
| AC13 | SC-24에서 | 화면을 보면 | 분석 제목이 없으면 `projectName`을 보여 준다 (#89, SC-23 목록과 같은 문구). SC-23·SC-24 모두 375px 폭에서 **페이지** 가로 스크롤이 없다 (NF-02) |

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다 (contract-checker, CI가 검사). Swagger Tag: **`팀`**. 새 클래스는 `com.vibenote.team`: `TeamChallengeController`(3개 엔드포인트), `TeamChallengeService`, `TeamChallengeCalculator`(상태·멤버 달성·팀 달성 순수 계산, 단위 테스트), `TeamChallengeMapper`(+ `mapper/team/TeamChallengeMapper.xml`), `team/dto`의 새 DTO. 모든 엔드포인트는 **로그인 필요**이고 `UserService.getActiveMember`로 탈퇴 회원을 막는다. 팀 역할 판단은 `TeamMapper.findRole`을 재사용한다. `quizzes`·`quiz_attempts`·`analyses`는 **읽기만** 한다.

### `POST /api/teams/{id}/challenges` (팀장)

- 요청

```json
{ "analysisId": 42, "startDate": "2026-11-25", "endDate": "2026-12-01", "targetRate": 80 }
```

- 응답 `201` (`TeamChallengeResponse`, 아래 목록 항목과 같은 형식)
- 에러: 400 `COMMON_400`(analysisId·startDate·endDate·targetRate) · 401 `AUTH_401` · 403 `TEAM_403` · 404 `TEAM_404`·`TEAM_006` · 409 `TEAM_011`·`TEAM_012` (이 순서로 판단)

### `GET /api/teams/{id}/challenges?analysisId=` (멤버)

- `analysisId`는 선택. 주면 그 분석의 목표만.
- 응답 `200` (`TeamChallengeResponse[]`, 최대 20개, 페이지 없음. 정렬은 AC4)

```json
[
  {
    "challengeId": 7,
    "analysisId": 42,
    "title": "spring-realworld",
    "projectName": "spring-realworld",
    "startDate": "2026-11-24",
    "endDate": "2026-11-30",
    "targetRate": 80,
    "status": "ONGOING",
    "memberCount": 3,
    "solvedMemberCount": 2,
    "achievedMemberCount": 1,
    "achieved": false,
    "members": [
      {
        "userId": 3, "nickname": "vibecoder", "role": "OWNER",
        "attempt": { "attemptId": 16, "score": 3, "total": 5, "createdAt": "2026-11-24T15:02:00+09:00" },
        "achieved": false
      },
      {
        "userId": 5, "nickname": "jsk", "role": "MEMBER",
        "attempt": { "attemptId": 18, "score": 4, "total": 5, "createdAt": "2026-11-24T16:40:00+09:00" },
        "achieved": true
      },
      { "userId": 9, "nickname": "newbie", "role": "MEMBER", "attempt": null, "achieved": false }
    ],
    "createdAt": "2026-11-24T10:00:00+09:00"
  }
]
```

- `status`: `UPCOMING|ONGOING|ENDED`. `attempt`는 F-14 `LastAttemptResponse`와 같은 형식이고 **기간 안**의 가장 최근 풀이다.
- 에러: 400 `COMMON_400`(analysisId) · 401 · 404 `TEAM_404`

### `DELETE /api/teams/{id}/challenges/{challengeId}` (팀장)

- 응답 `204`. 에러: 401 · 404 `TEAM_404`·`TEAM_013` · 403 `TEAM_403`

### 변경: `GET /api/teams/{id}/analyses/{analysisId}/understanding` (#89)

- `TeamUnderstandingResponse`에 `"projectName": "spring-realworld"`를 `title` 다음에 추가한다 (하위 호환).

### 에러 코드 (`ErrorCode`의 `// ── TEAM (B F-20) ──` 블록에 추가, conventions 3절 `TEAM` 행 갱신)

| 상태 | 코드 | 메시지 | 언제 |
|---|---|---|---|
| 409 | TEAM_011 | 이 분석에는 이미 예정되거나 진행 중인 팀 목표가 있습니다. | 같은 분석에 종료일 ≥ 오늘인 목표가 있는데 또 만듦 |
| 409 | TEAM_012 | 팀 목표는 최대 20개입니다. 종료된 목표를 지워 주세요. | 목표 20개인 팀에 만듦 |
| 404 | TEAM_013 | 팀 목표를 찾을 수 없습니다. | 없는 목표, 다른 팀의 목표, soft delete된 분석의 목표 |

## 4. DB 변경

- Flyway: `V{n}__team_challenges.sql` (번호는 구현 시작 때 `git merge origin/develop` 후 `node scripts/next-migration.mjs`로. 지금 develop 기준 V10)

```sql
-- F-20c 팀 기간 목표: 팀장이 공유 분석 하나에 기간과 목표 정답률을 정한다. 달성은 조회 때 계산하고 저장하지 않는다.
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
```

- 공유 해제·팀 삭제·분석 실제 삭제는 `team_analyses`를 거쳐 CASCADE로 지워진다. 팀장이 탈퇴하면 F-20a대로 팀이 지워지므로 `created_by` CASCADE는 같은 결과다.
- 기간 안 풀이: `qa.created_at >= start_date AND qa.created_at < end_date + INTERVAL 1 DAY` (DB 세션 시간대 `+09:00`, backend/CLAUDE.md "시각").
- 사용 테이블: `team_challenges`(전부), `teams`·`team_members`·`team_analyses`(F-20a·b), 읽기 전용 `analyses`·`quizzes`·`quiz_attempts`·`users`
- `FlywayMigrationTest` 테이블 목록에 `team_challenges` 추가

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | 새 라우트 없음. SC-23 `TeamDetailView.vue`에 팀 목표 영역(멤버와 공유 분석 사이), SC-24 `TeamUnderstandingView.vue`에 팀 목표 영역(이해도 위)·#89 제목 |
| 컴포넌트 | `components/team/TeamChallengeList.vue`(SC-23 목록·삭제), `TeamChallengeProgress.vue`(SC-24 목표 하나의 멤버별 진행), `TeamChallengeForm.vue`(SC-24 팀장 만들기 폼), 상태 칩·남은 날 문구는 `teamLabels.js`에 `challengeStatusLabel` 추가. 빈 상태는 `EmptyState`, 확인은 `window.confirm`. `TeamAnalysisList.vue`는 해제 확인 문구와 해제 성공 이벤트(`unshared`)만 바꾼다 |
| store | 없음 (화면 로컬 상태) |
| API 함수 | `src/api/teams.js`에 추가: `fetchTeamChallenges(id, { analysisId } = {})`, `createTeamChallenge(id, body)`, `deleteTeamChallenge(id, challengeId)` |
| 상태별 UI | 로딩 "불러오는 중…" / 목표 없음(AC11·AC12) / 영역 실패는 서버 `message` + "다시 시도" / 폼 검증·409 메시지(AC12) |

남은 날 계산의 "오늘"은 브라우저 날짜(Asia/Seoul 사용자 전제)다. 상태 자체는 서버 `status`를 그대로 쓴다.

## 6. LLM (AI 기능만)

해당 없음 (LLM을 부르지 않는다. 기존 퀴즈 풀이를 집계만 한다).

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1~AC4, AC7~AC9 | `team/TeamChallengeApiTest#AC1_…`~`AC9_…` (생성 UPCOMING·ONGOING, 필드별 검증·경계값, TEAM_006·soft delete, TEAM_011·종료 목표 뒤 재생성, TEAM_012 상한, 판단 순서 403 > 400, 목록 정렬·필터·soft delete 제외·analysisId 400, 삭제 권한 3종, 해제·탈퇴·내보내기·팀 삭제·회원 탈퇴 CASCADE, 401) | MockMvc 통합 |
| AC5, AC6 | `TeamChallengeApiTest#AC5_…`·`AC6_…` (기간 경계 00:00:00·23:59:59·다음 날, 떠난 회원 제외, 80% 경계, 전원 달성, 상태 3종·종료 후 판정) | MockMvc 통합 |
| AC5, AC6 (계산) | `team/TeamChallengeCalculatorTest` (상태 경계, `score×100 ≥ rate×total` 경계 4/5·80, 풀이 없음, 전원 달성) | 단위 |
| AC10 | `team/TeamAnalysisApiTest`에 `AC10_…projectName` 추가 (F-20c 번호) | MockMvc 통합 |
| AC11 | `src/views/team/__tests__/TeamDetailView.spec.js`에 추가, `src/components/team/__tests__/TeamChallengeList.spec.js` (항목 문구·칩 3종·달성/미달성, 팀장만 삭제·확인 취소, 빈 상태 팀장/팀원, 영역 실패, 해제 뒤 목표 목록 다시 불러옴·새 확인 문구) | Vitest |
| AC12, AC13 | `src/views/team/__tests__/TeamUnderstandingView.spec.js`에 추가, `TeamChallengeProgress.spec.js`, `TeamChallengeForm.spec.js` (예정·진행 중 우선·최근 종료, 멤버 문구, 팀장만 폼·기본값, 400 details·409 위치·입력 유지, 요청 중 비활성, 목표 영역 실패, 제목 없음 → projectName) | Vitest |
| AC11~AC13 (화면) | ui-verifier: SC-23(팀 목표 영역)·SC-24(목표·폼) 데스크톱 1280·모바일 375, 팀장·팀원 | Playwright |

## 8. 범위 밖

- 목표 수정 (지우고 다시 만든다), 목표 유형 추가(풀이 횟수·복습 횟수 등은 F-24·F-25 학습 챌린지)
- 모듈(레이어)별 목표, 여러 분석을 묶은 목표
- 달성 결과 저장·종료 시점 고정, 달성 배지·랭킹, 피드 활동(`activities`)·알림
- 프로필의 팀 목표 표시, SC-22 팀 카드의 목표 요약
- 같은 팀에 동시에 목표를 만들 때 상한 경합 테스트 (잠금은 하되 동시성 테스트는 쓰지 않음)

## 9. 결정 필요

- 없음 (11-24 사용자 결정: 공유 분석 1개 + 목표 정답률, 지금 멤버 전원 달성, 분석마다 끝나지 않은 목표 1개·팀당 20개, #89 흡수)

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/F-20c-team-challenge`
- [x] Flyway 마이그레이션 (+ `FlywayMigrationTest` 테이블 목록)
- [x] AC별 실패 테스트 작성 (red 확인)
- [x] BE 구현 (green)
- [x] FE 구현 (green)
- [x] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [x] `docs/conventions.md` 3절 `TEAM` 행에 011~013
- [x] contract-checker 통과
- [x] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일)
- [x] reviewer 지적 사항 반영 (권장 1 수정, 권장 2·3 → #95·#96)
- [x] 산출물 반영: 화면 설계서 SC-23(팀 목표 영역·해제 문구)·SC-24(목표·폼), ERD(`team_challenges`), 요구사항 정의서 F-20 행(F-20c)
- [x] PR CI 초록 (공통 `ErrorCode`·conventions·`FlywayMigrationTest` 변경 → @whans1996 멘션), `Closes #89`
- [x] 상태 DONE (PR #98, Codex ✅ 53c1047)
