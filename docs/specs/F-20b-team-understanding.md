# F-20b 팀 공동 분석 이해도 (분석 공유 + 팀원 퀴즈 풀이 + 모듈별 이해도 + SC-24)

| 항목 | 값 |
|---|---|
| 상태 | DRAFT <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-20b (F-20 팀 챌린지의 2/3. 팀·초대는 F-20a(DONE), 기간 목표는 F-20c) |
| 우선순위 | P2 |
| 영역 오너 | B 김준섭 (카드·소셜) |
| 관련 필수 요구사항 | ⑫ 확장 (팀 챌린지. 필수 ⑫는 F-14·F-15·F-24·F-25가 충족) |
| 의존 spec | F-20a 팀(`TeamService`·`team_members`), F-14 퀴즈(`QuizService.getQuiz`·`submit`, `QuizView.vue`), F-08 개념 연결(`analysis_concepts.file_id`), F-06 레이어(`analysis_files.layer`, FE `utils/layers.js`), F-05a 내 분석 목록(`GET /api/analyses`). **A 영역 변경 2곳**: `QuizService`의 조회·제출 권한 판단, `QuizView.vue`의 돌아가기 링크 (PR에서 @whans1996 멘션) |
| 관련 이슈 | 없음 |
| 승인 | (승인한 사람, 날짜) |

## 1. 목적

같은 프로젝트를 AI로 만드는 팀이 **한 분석의 퀴즈를 함께 풀고, 어느 모듈(레이어)을 팀이 덜 이해하는지** 한눈에 보게 한다 (기획서 문제 6, 핵심 가치 3 "이해도를 팀과 공유").
팀 멤버가 자기 완료 분석을 팀에 **공유**하면, 다른 팀원은 그 분석의 **퀴즈만** 풀 수 있다 (설명·파일·개념·흐름 화면은 여전히 주인만 본다. 같은 프로젝트를 쓰는 팀이라 코드는 각자 갖고 있다는 전제). 팀 화면은 멤버마다 **가장 최근 풀이**를 모아 레이어별 정답률을 보여 준다.

**규칙 (11-23 결정)**: 공유는 팀 멤버 누구나 **자기** 분석만, 상태 `DONE`이고 퀴즈가 있어야 한다. 팀당 공유 분석 최대 **10개**. 해제는 공유한 사람(분석 주인) 또는 팀장. 공유한 사람이 팀을 떠나면(탈퇴·내보내기·회원 탈퇴) 그 사람이 그 팀에 공유한 분석은 함께 해제된다. 풀이 기록(`quiz_attempts`)은 해제·탈퇴와 상관없이 남는다.

**모듈 = 레이어**: 문항 → 문항의 개념(`quiz_questions.concept_id`) → 그 분석에서 그 개념이 연결된 파일(`analysis_concepts(analysis_id, concept_id).file_id`) → 파일의 `analysis_files.layer`. 개념이 없거나(NULL) 연결이 없거나 `file_id`가 NULL이면 `other`(기타)로 묶는다. A의 퀴즈 생성(QUIZ 단계)·스키마는 바꾸지 않는다.

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_팀_멤버가_내_완료_분석을_공유하면_201을_받는다()`
> BE 테스트는 다른 테스트의 데이터나 시드 카드에 기대지 않는다. 회원·팀·분석(`DONE`)·파일·개념·퀴즈·문항·풀이를 테스트마다 직접 넣고 id로 확인한다.
> 권한 판단 순서는 새 팀 분석 API에서 같다: **401 → 404 `TEAM_404`(없는 팀·내가 멤버가 아닌 팀) → 400 검증 → 대상 판단(404 `ANALYSIS_404`·`TEAM_006`) → 403 `TEAM_010` → 409**.

### 백엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 팀 T의 멤버 U가 자기 `DONE` 분석 A(퀴즈 있음)를 가졌을 때 | `POST /api/teams/{T}/analyses` `{analysisId: A}`를 보내면 | 201과 `{analysisId: A, sharedAt}`을 받고 `team_analyses(T, A)` 한 행이 생긴다. 같은 A를 U가 속한 다른 팀 T2에도 공유할 수 있다 (팀마다 한 행) |
| AC2 | 팀 T의 멤버 U가 | 공유 요청이 잘못되면 | `analysisId`가 없거나 1 미만 → 400 `COMMON_400`(`details.analysisId`). 없는 분석·**남의 분석**·soft delete된 분석 → 404 `ANALYSIS_404`. 내 분석이지만 `DONE`이 아니거나 퀴즈가 없음(`quizzes` 행 없음) → 409 `TEAM_009` "퀴즈가 있는 완료된 분석만 공유할 수 있습니다.". 이미 T에 공유됨 → 409 `TEAM_007` "이미 팀에 공유한 분석입니다.". T의 공유 분석이 이미 10개(soft delete된 분석은 세지 않음) → 409 `TEAM_008` "팀에 공유할 수 있는 분석은 최대 10개입니다.". 멤버가 아닌 회원은 무엇을 보내든 404 `TEAM_404`. 어느 경우든 행은 생기지 않는다. 상한은 `teams` 행을 `SELECT … FOR UPDATE`로 잠근 뒤 센다 (F-20a 가입과 같은 방식, 동시성 테스트는 범위 밖) |
| AC3 | T에 A1(U 공유, 11/24)·A2(V 공유, 11/25)·A3(W 공유 후 soft delete)이 있고 U가 A1을 1회, V가 A1을 2회 풀었을 때 | 멤버 U가 `GET /api/teams/{T}/analyses`를 요청하면 | 200과 배열 `[A2, A1]`(`shared_at DESC, analysis id DESC`)을 받고 A3은 없다. 항목은 3절 `TeamAnalysisResponse`(`analysisId, title, projectName, level, sharedBy{userId,nickname}, sharedAt, questionCount, solvedMemberCount, myLastAttempt, canUnshare`)다. A1의 `solvedMemberCount`는 **지금 멤버** 중 풀이가 1건 이상인 사람 수 2, `myLastAttempt`는 U 자신의 가장 최근 풀이(`created_at DESC, id DESC`)의 `{score,total,createdAt}`이고 안 풀었으면 `null`. `canUnshare`는 요청자가 공유한 사람이거나 팀장이면 `true`. 공유가 없으면 `[]`. 멤버가 아니면 404 `TEAM_404` |
| AC4 | T에 V가 공유한 A가 있을 때 | V 또는 팀장 U가 `DELETE /api/teams/{T}/analyses/{A}`를 보내면 / 다른 팀원 W가 보내면 / T에 공유되지 않은 분석 id를 보내면 | 204이고 `team_analyses(T, A)`만 지워진다 (`quiz_attempts`는 그대로) / 403 `TEAM_010` "공유한 사람이나 팀장만 해제할 수 있습니다." / 404 `TEAM_006` "팀에 공유된 분석을 찾을 수 없습니다.". 멤버가 아니면 404 `TEAM_404` |
| AC5 | T에 U가 공유한 A(퀴즈 Q)가 있고 W가 T의 팀원일 때 | W가 `GET /api/analyses/{A}/quiz` → `POST /api/quizzes/{Q}/attempts`를 보내면 | 조회 200(`lastAttempt`는 **W 자신**의 최근 풀이), 제출 201이고 `quiz_attempts.user_id = W`로 저장되며 W가 틀린 공개 개념은 **W의** 복습 계획(`reason=WRONG`)에 들어간다 (F-14 AC6·AC7 그대로). 판단은 `QuizService`가 "분석 주인이거나 `TeamAnalysisAccess.isSharedWith(userId, analysisId)`"로 한다. 같은 W라도 공유가 해제됐거나 W가 T를 떠났거나 A가 soft delete되면 조회 404 `ANALYSIS_404`·제출 404 `QUIZ_404`(F-14와 같음). T 멤버가 아닌 회원도 같다. **퀴즈 외 분석 API**(`GET /api/analyses/{A}`, `/status`, `/files`, `/files/{fileId}`, `/concepts`, `/flows`)는 W에게 계속 404 `ANALYSIS_404`다 |
| AC6 | A의 퀴즈가 4문항이고 q1·q2는 개념 C1(→ controller 파일), q3은 C2(→ service 파일), q4는 개념 NULL일 때. T의 멤버 U(팀장)는 처음에 0/4, 그다음 q1·q3만 맞힘(최근), V는 q1·q2·q4 맞힘, W는 안 풂 | 멤버가 `GET /api/teams/{T}/analyses/{A}/understanding`을 요청하면 | 200과 3절 `TeamUnderstandingResponse`. `questionCount` 4, `memberCount` 3, `solvedMemberCount` 2. `modules`는 레이어 순서(`controller, service, mapper, vue, js, config, domain, other`) 중 문항이 있는 것만: `controller {questionCount:2, correctRate:75}`(U 1/2 + V 2/2 = 3/4), `service {1, 50}`, `other {1, 50}`. `correctRate`는 푼 멤버들의 해당 레이어 맞힌 수 합 ÷ 문항 수 합 × 100을 반올림한 정수다. `members`는 F-20a 상세와 같은 순서(팀장 먼저, `joined_at ASC, user id ASC`)이고 U는 `lastAttempt {score:2,total:4}`·`modules [{controller,1,2},{service,1,1},{other,0,1}]`, W는 `lastAttempt null`·`modules []` |
| AC7 | AC6에서 | 경계 조건이면 | 아무도 풀지 않았으면 모든 `correctRate`가 `null`이고 `solvedMemberCount` 0이다. 개념은 있지만 그 분석의 `analysis_concepts`에 연결이 없거나 `file_id`가 NULL인 문항은 `other`다. 팀을 떠난 회원의 풀이는 집계·`members`에 없다 (지금 멤버만). 분석 주인이 아닌 멤버의 풀이도 주인의 풀이와 똑같이 센다. T에 공유되지 않았거나 soft delete된 분석이면 404 `TEAM_006`, 멤버가 아니면 404 `TEAM_404` |
| AC8 | V가 T와 T2에 A를 공유했을 때 | V가 T를 탈퇴하거나 팀장이 V를 내보내면 / 팀장이 T를 삭제하면 / V가 회원 탈퇴(F-03)하면 | V가 **T에** 공유한 행만 지워지고 T2의 공유는 남는다 (`TeamService.leave`·`removeMember`가 같은 트랜잭션에서 지움) / T의 공유 행이 모두 지워진다 (`team_analyses.team_id` FK CASCADE) / V가 공유한 모든 행이 지워진다 (`TeamService.removeAllOf`에 추가). 세 경우 모두 `quiz_attempts`는 그대로다 |
| AC9 | 토큰 없음·만료·형식 오류이거나 탈퇴한 회원의 남은 Access 토큰일 때 | 3절의 새 API 4개를 요청하면 | 401 `AUTH_401`이고 아무것도 바뀌지 않는다 |

### 프론트엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC10 | 팀 T의 멤버가 | SC-23 `/teams/{T}`를 열면 | 멤버 목록 아래 "공유 분석 {n}/10" 영역이 보인다. 항목마다 제목(없으면 `projectName`), "{닉네임} 님 공유 · YYYY-MM-DD", "{solvedMemberCount}/{memberCount}명 풀이", 내 최근 풀이 "내 점수 {score}/{total}" 또는 "아직 풀지 않음", 버튼 "퀴즈 풀기"(→ `/analyses/{A}/quiz?team={T}`)·"이해도 보기"(→ SC-24 `/teams/{T}/analyses/{A}`), `canUnshare`면 "공유 해제"가 있다. 해제는 확인 대화상자 "이 분석의 공유를 해제할까요? 팀원의 풀이 기록은 남습니다."에서 확인해야 요청하고, 성공하면 목록을 다시 불러온다. 0개면 "아직 공유한 분석이 없습니다. 내 분석을 공유해 팀원과 함께 퀴즈를 풀어 보세요.", 목록 조회가 실패하면 그 영역에 서버 `message`와 "다시 시도"(팀 정보·멤버는 그대로 보임) |
| AC11 | SC-23에서 | "분석 공유"를 누르면 | 내 분석(`fetchAnalyses({page:1,size:100})`) 중 `DONE`이고 이 팀에 아직 공유하지 않은 것만 고르는 목록이 열린다 (F-17 `AttachmentPicker`와 같은 방식). 하나를 골라 "공유"를 누르면 `shareTeamAnalysis` 후 목록을 다시 불러오고 고르기 목록을 닫는다. 고를 것이 없으면 "공유할 수 있는 완료된 분석이 없습니다." + "분석 요청하기"(→ `/analyses/new`) 링크. 공유가 10개면 "분석 공유" 버튼이 비활성이고, 요청 중에도 비활성이다. `TEAM_007`·`TEAM_008`·`TEAM_009`·`ANALYSIS_404`는 고르기 목록 아래 서버 `message`를 보여 주고 선택을 유지한다 |
| AC12 | 팀 T의 멤버가 | SC-24 `/teams/:id(\d+)/analyses/:analysisId(\d+)`를 열면 | 분석 제목, "{닉네임} 님 공유", "{solvedMemberCount}/{memberCount}명 풀이 · 객관식 {questionCount}문항", "퀴즈 풀기"(→ `/analyses/{A}/quiz?team={T}`)와 "← 팀으로"(→ `/teams/{T}`)가 보인다. **모듈별 이해도**: 모듈마다 레이어 라벨(`layerLabel`), "{questionCount}문항", 정답률 막대(너비 = `correctRate`%)와 "{correctRate}%", `null`이면 막대 없이 "풀이 없음". `correctRate`가 가장 낮은 모듈(같으면 앞 순서) 하나에 "가장 약한 모듈" 칩. **멤버별**: 행 = 멤버(닉네임 → `/users/{userId}`, 최근 점수 또는 "아직 풀지 않음"), 열 = 모듈, 칸 = "{correct}/{total}" 또는 "–". 아무도 풀지 않았으면 표 대신 "아직 이 분석의 퀴즈를 푼 팀원이 없습니다." `TEAM_006`이면 "팀에 공유된 분석을 찾을 수 없습니다." + "← 팀으로", `TEAM_404`면 "팀을 찾을 수 없습니다." + "← 내 팀으로"(SC-22), 그 밖의 실패는 서버 `message`와 "다시 시도" |
| AC13 | 팀원이 SC-08 `/analyses/{A}/quiz?team={T}`(T는 숫자)로 들어왔을 때 | 화면을 보면 | 조회 404(`ANALYSIS_404`·`QUIZ_404`)·채점 결과 화면의 "분석으로 돌아가기" 대신 "팀으로 돌아가기"(→ `/teams/{T}`)를 보여 준다. "다시 풀기"는 `team` 쿼리를 유지한다. `team`이 없거나 숫자가 아니면 F-14 그대로다 (`QuizView.vue`, A 영역). SC-23·SC-24 모두 375px 폭에서 **페이지** 가로 스크롤이 없다 (SC-24 멤버 표는 표 영역 안에서만 가로 스크롤, NF-02) |

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다 (contract-checker, CI가 검사). Swagger Tag: **`팀`**. 새 클래스는 `com.vibenote.team`: `TeamAnalysisController`(4개 엔드포인트), `TeamAnalysisService`, `TeamAnalysisAccess`(퀴즈 권한 판단용, `TeamMapper` 쿼리 1개), `TeamUnderstandingCalculator`(순수 계산, 단위 테스트), `team/dto`의 새 DTO. 모든 엔드포인트는 **로그인 필요**이고 `UserService.getActiveMember`로 탈퇴 회원을 막는다. `TeamMapper.xml`은 `analyses`·`analysis_files`·`analysis_concepts`·`quizzes`·`quiz_questions`·`quiz_attempts`·`quiz_answers`를 **읽기만** 한다.
> 기존 퀴즈 API 2개(`GET /api/analyses/{id}/quiz`, `POST /api/quizzes/{quizId}/attempts`)는 요청·응답 형식이 그대로이고, 허용 대상만 "주인 + 공유받은 팀 멤버"로 넓어진다 (Swagger 설명 문구만 고침).

### `POST /api/teams/{id}/analyses` (멤버, 내 분석 공유)

- 요청 `{ "analysisId": 42 }`
- 응답 `201`

```json
{ "analysisId": 42, "sharedAt": "2026-11-25T14:20:00+09:00" }
```

- 에러: 400 `COMMON_400`(analysisId) · 401 `AUTH_401` · 404 `TEAM_404`·`ANALYSIS_404` · 409 `TEAM_009`·`TEAM_007`·`TEAM_008` (이 순서로 판단)

### `GET /api/teams/{id}/analyses` (멤버)

- 응답 `200` (`TeamAnalysisResponse[]`, 최대 10개, 페이지 없음)

```json
[
  {
    "analysisId": 42,
    "title": "spring-realworld",
    "projectName": "spring-realworld",
    "level": "JUNIOR",
    "sharedBy": { "userId": 3, "nickname": "vibecoder" },
    "sharedAt": "2026-11-25T14:20:00+09:00",
    "questionCount": 6,
    "solvedMemberCount": 2,
    "myLastAttempt": { "score": 4, "total": 6, "createdAt": "2026-11-25T15:02:00+09:00" },
    "canUnshare": true
  }
]
```

- 에러: 401 · 404 `TEAM_404`

### `DELETE /api/teams/{id}/analyses/{analysisId}` (공유한 사람·팀장)

- 응답 `204`. 에러: 401 · 404 `TEAM_404`·`TEAM_006` · 403 `TEAM_010`

### `GET /api/teams/{id}/analyses/{analysisId}/understanding` (멤버)

- 응답 `200` (`TeamUnderstandingResponse`)

```json
{
  "analysisId": 42,
  "title": "spring-realworld",
  "sharedBy": { "userId": 3, "nickname": "vibecoder" },
  "questionCount": 4,
  "memberCount": 3,
  "solvedMemberCount": 2,
  "modules": [
    { "layer": "controller", "questionCount": 2, "correctRate": 75 },
    { "layer": "service", "questionCount": 1, "correctRate": 50 },
    { "layer": "other", "questionCount": 1, "correctRate": 50 }
  ],
  "members": [
    {
      "userId": 3, "nickname": "vibecoder", "role": "OWNER",
      "lastAttempt": { "score": 2, "total": 4, "createdAt": "2026-11-25T15:02:00+09:00" },
      "modules": [
        { "layer": "controller", "correct": 1, "total": 2 },
        { "layer": "service", "correct": 1, "total": 1 },
        { "layer": "other", "correct": 0, "total": 1 }
      ]
    },
    { "userId": 9, "nickname": "newbie", "role": "MEMBER", "lastAttempt": null, "modules": [] }
  ]
}
```

- `layer`는 `analysis_files.layer` 소문자 값(`controller|service|mapper|vue|js|config|domain|other`). `lastAttempt`는 F-14 `LastAttemptResponse`와 같은 형식이다.
- 에러: 401 · 404 `TEAM_404`·`TEAM_006`

### 에러 코드 (`ErrorCode`의 `// ── TEAM (B F-20) ──` 블록에 추가, conventions 3절 `TEAM` 행 갱신)

| 상태 | 코드 | 메시지 | 언제 |
|---|---|---|---|
| 404 | TEAM_006 | 팀에 공유된 분석을 찾을 수 없습니다. | 그 팀에 공유되지 않은 분석, soft delete된 공유 분석 |
| 409 | TEAM_007 | 이미 팀에 공유한 분석입니다. | 같은 팀에 같은 분석을 다시 공유 |
| 409 | TEAM_008 | 팀에 공유할 수 있는 분석은 최대 10개입니다. | 공유 10개인 팀에 공유 |
| 409 | TEAM_009 | 퀴즈가 있는 완료된 분석만 공유할 수 있습니다. | `DONE`이 아니거나 퀴즈가 없는 내 분석 |
| 403 | TEAM_010 | 공유한 사람이나 팀장만 해제할 수 있습니다. | 다른 팀원이 공유 해제 |

### A 영역 변경 (퀴즈 권한)

- `QuizService.getQuiz`: `analysisService.checkOwned` 대신 "내 미삭제 분석이거나 `teamAnalysisAccess.isSharedWith(userId, analysisId)`" — 아니면 `ANALYSIS_404`.
- `QuizService.submit`: `mapper.findOwned(quizId, userId)` 대신 미삭제 분석의 퀴즈를 찾고(`QuizMapper.findActive(quizId)`, 분석 주인 id 포함) 주인이거나 공유받았으면 허용 — 아니면 `QUIZ_404`.
- `TeamAnalysisAccess.isSharedWith`: `team_analyses ta JOIN team_members tm ON tm.team_id = ta.team_id AND tm.user_id = ? JOIN analyses a ON a.id = ta.analysis_id AND a.deleted_at IS NULL WHERE ta.analysis_id = ?` 존재 여부.
- 팀원의 풀이도 그 회원의 `quiz_attempts`이므로 F-03 학습 통계, F-16 추천(오답), F-25 챌린지 `QUIZ_COUNT` 집계에 그대로 들어간다 (의도한 동작. F-25 spec 작성 때 참고).

## 4. DB 변경

- Flyway: `V{n}__team_analyses.sql` (번호는 구현 시작 때 `git merge origin/develop` 후 `node scripts/next-migration.mjs`로. 지금 develop 기준 V9)

```sql
-- F-20b 팀 공유 분석: 멤버가 자기 완료 분석을 팀에 공유하면 팀원이 그 퀴즈를 풀 수 있다.
-- 공유한 사람 = 분석 주인(analyses.user_id). team_challenges는 F-20c에서 추가한다.
CREATE TABLE team_analyses (
    team_id      BIGINT   NOT NULL,
    analysis_id  BIGINT   NOT NULL,
    shared_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (team_id, analysis_id),
    KEY idx_team_analyses_analysis (analysis_id),
    CONSTRAINT fk_team_analyses_team     FOREIGN KEY (team_id)     REFERENCES teams (id)    ON DELETE CASCADE,
    CONSTRAINT fk_team_analyses_analysis FOREIGN KEY (analysis_id) REFERENCES analyses (id) ON DELETE CASCADE
);
```

- 공유한 사람은 별도 컬럼 없이 `analyses.user_id`로 안다 (자기 분석만 공유하므로 항상 같다).
- soft delete된 분석의 행은 지우지 않고 조회·권한·상한에서 `a.deleted_at IS NULL`로 거른다 (F-19 구현이 팀 테이블을 몰라도 된다).
- 사용 테이블: `team_analyses`(전부), `teams`·`team_members`(F-20a), 읽기 전용 `analyses`·`analysis_files`·`analysis_concepts`·`quizzes`·`quiz_questions`·`quiz_attempts`·`quiz_answers`·`users`

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | SC-23 `TeamDetailView.vue`에 공유 분석 영역 추가. `router/routes/team.js`에 `/teams/:id(\\d+)/analyses/:analysisId(\\d+)` → `views/team/TeamUnderstandingView.vue` (SC-24 팀 이해도, `requiresAuth`). SC-08 `views/QuizView.vue`(A)에 `?team=` 돌아가기 링크 |
| 컴포넌트 | `components/team/TeamAnalysisList.vue`(공유 분석 목록·해제), `TeamAnalysisPicker.vue`(내 완료 분석 고르기·공유), `ModuleUnderstandingBars.vue`(모듈별 막대·가장 약한 모듈 칩), `MemberUnderstandingTable.vue`(멤버 × 모듈 표). 레이어 라벨·순서는 `utils/layers.js`(`layerLabel`) 재사용, 빈 상태는 `EmptyState`, 확인은 `window.confirm` |
| store | 없음 (화면 로컬 상태) |
| API 함수 | `src/api/teams.js`에 추가: `fetchTeamAnalyses(id)`, `shareTeamAnalysis(id, analysisId)`, `unshareTeamAnalysis(id, analysisId)`, `fetchTeamUnderstanding(id, analysisId)`. 고르기 목록은 기존 `api/analyses.js`의 `fetchAnalyses` |
| 상태별 UI | 로딩 "불러오는 중…" / 빈 공유 목록(AC10) / 고를 분석 없음(AC11) / 풀이 없음(AC12) / 404 2종(AC12) / 그 밖의 실패는 서버 `message` + "다시 시도" |

## 6. LLM (AI 기능만)

해당 없음 (LLM을 부르지 않는다. 기존 퀴즈 문항을 집계만 한다).

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1~AC4, AC6~AC9 | `team/TeamAnalysisApiTest#AC1_…`~`AC9_…` (공유 성공·다른 팀 중복 허용, 검증·남의 분석·미완료·퀴즈 없음·중복·상한 10·판단 순서, 목록 순서·삭제 분석 제외·solvedMemberCount·myLastAttempt·canUnshare, 해제 권한 3종, 이해도 예시 데이터·빈 풀이·연결 없음 → other·떠난 멤버 제외·404 2종, 탈퇴·내보내기·팀 삭제·회원 탈퇴 정리, 401) | MockMvc 통합 |
| AC5 | `quiz/QuizTeamAccessApiTest` (팀원 조회·제출·복습 계획, 해제·탈퇴·soft delete 뒤 404, 비멤버 404, 퀴즈 외 분석 API 6개 404). 기존 `QuizApiTest`가 그대로 통과해야 한다 | MockMvc 통합 |
| AC6, AC7 (계산) | `team/TeamUnderstandingCalculatorTest` (레이어 매핑·순서, 반올림 66.7 → 67, 풀이 없음 null) | 단위 |
| AC10, AC11 | `src/views/team/__tests__/TeamDetailView.spec.js`에 추가 (공유 목록·빈 상태·영역 실패, 링크 `?team=`, 해제 확인·취소), `src/components/team/__tests__/TeamAnalysisPicker.spec.js` (DONE·미공유만, 없음 안내, 10개 비활성, 에러 위치·선택 유지) | Vitest |
| AC12 | `src/views/team/__tests__/TeamUnderstandingView.spec.js` (막대·null·가장 약한 모듈, 표 칸, 풀이 없음, 404 2종, 실패) | Vitest |
| AC13 | `src/views/__tests__/QuizView.spec.js`에 추가 (`?team=` 돌아가기 링크·다시 풀기 유지, 숫자가 아니면 기존 링크) | Vitest |
| AC10~AC13 (화면) | ui-verifier: SC-23(공유 영역)·SC-24·SC-08(`?team=`) 데스크톱 1280·모바일 375 | Playwright |

## 8. 범위 밖

- **F-20c**: 팀 기간 목표(`team_challenges`: 기간·목표 정답률·달성 판정)
- 팀원의 공유 분석 설명·파일·개념·실행 흐름 열람 (SC-07, 공개 분석의 타인 조회는 F-19와 함께 다시 본다)
- 레이어보다 세밀한 모듈(파일·패키지 단위), 문항에 파일을 직접 연결하는 퀴즈 생성 변경
- 공유·풀이의 피드 활동(`activities`), 알림, 팀 이해도의 시간 추이 그래프
- 같은 팀에 동시에 공유할 때 10개 상한 경합 테스트 (잠금은 하되 동시성 테스트는 쓰지 않음)

## 9. 결정 필요

- 없음 (11-23 사용자 결정: b·c 분할, 팀원은 퀴즈만, 멤버별 최근 풀이 기준, 멤버 누구나 자기 분석 공유·공유자/팀장 해제)

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [ ] 브랜치 `feature/F-20b-team-understanding`
- [ ] Flyway 마이그레이션
- [ ] AC별 실패 테스트 작성 (red 확인)
- [ ] BE 구현 (green)
- [ ] FE 구현 (green)
- [ ] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [ ] `docs/conventions.md` 3절 `TEAM` 행에 006~010
- [ ] contract-checker 통과
- [ ] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일)
- [ ] reviewer 지적 사항 반영
- [ ] 산출물 반영: 화면 설계서 SC-23(공유 영역)·SC-24 추가·SC-08(`?team=`), ERD(`team_analyses`), 요구사항 정의서 F-20 행(F-20b·F-20c)
- [ ] PR CI 초록 (A 영역 `QuizService`·`QuizMapper`·`QuizView.vue`, 공통 `ErrorCode`·conventions 변경 → @whans1996 멘션)
- [ ] 상태 DONE (PR 머지 후)
