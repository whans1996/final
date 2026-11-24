# F-25 챌린지 참여·달성도 (목록·상세, 참여·취소, 자동 집계, 내 챌린지)

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-25 |
| 우선순위 | P0 |
| 영역 오너 | A 김동환 (AI·분석·학습 루프) |
| 관련 필수 요구사항 | ⑫ 추가(계획/챌린지) · ⑥ 팔로우(피드 활동) · 명세서 "추가 · 챌린지 참여·달성도" (F1312·F1313) |
| 의존 spec | F-24 챌린지 관리(`challenges`, `ChallengeStatus`, `ChallengeMapper`, 이미지 공개 경로), F-04 피드(`ActivityService`, `CHALLENGE_*` 문장·링크), F-14 퀴즈 풀이, F-15 복습 계획, F-05a 분석 완료, F-03 프로필, F-16 대시보드 |
| 관련 이슈 | #99 (FE 오늘 날짜를 Asia/Seoul로 계산하는 공용 유틸, 구현 PR에 `Closes #99`) |
| 승인 | 김동환 2026-11-24 |

## 1. 목적

회원이 관리자가 연 학습 챌린지("12월 한 달 퀴즈 10회")를 보고 참여한다. 그러면 기간 안의 복습 완료·퀴즈 풀이·분석이 **자동으로** 달성도에 쌓이고, 목표를 채우면 달성으로 표시된다. 참여와 달성은 팔로워 피드에 남는다. 회원은 대시보드와 "내 챌린지"에서 진행 상황을 보고, 프로필에서 다른 회원이 참여한 챌린지도 볼 수 있다 (UC33, SC-12·13·04·18).

**규칙 (11-24 결정)**:
- **집계 대상 기간은 챌린지 시작일 ~ 종료일**(Asia/Seoul 날짜, 양 끝 포함)이다. 참여하기 전의 활동도 기간 안이면 센다. 그래서 참여하자마자 달성할 수 있다.
- 목표 유형별 집계 (분석을 삭제해도 그 분석의 활동은 센다. `backend/CLAUDE.md` "챌린지 집계는 삭제 분석을 거르지 않는다"):
  - `REVIEW_COUNT` — 내 `review_plans` 중 `status = 'DONE'`이고 `done_at`이 기간 안인 행 수
  - `QUIZ_COUNT` — 내 `quiz_attempts` 중 `created_at`이 기간 안인 풀이의 **(퀴즈, 날짜) 조합 수**. 같은 퀴즈를 같은 날 여러 번 풀어도 1회, 다른 날 다시 풀면 1회 더 센다 (F-14 결정로그 "중복 집계는 F-25에서")
  - `ANALYSIS_COUNT` — 내 `analyses` 중 `status = 'DONE'`이고 `created_at`(요청 시각)이 기간 안인 수
- **달성도는 활동이 생길 때 다시 세어 저장한다** (`challenge_participants.progress`, `completed_at`). 조회 API는 저장값만 읽는다. 다시 셀 때 `progress = min(센 수, goal_value)`이고, `progress = goal_value`가 되는 순간 `completed_at`을 채우고 `CHALLENGE_COMPLETE` 활동을 남긴다. 이렇게 하면 피드의 달성 시각이 실제 달성 시각이 된다.
- **한 번 달성하면 다시 세지 않는다.** 복습을 되돌리거나 분석을 지워 센 수가 줄어도 달성과 `progress`는 그대로다. 달성하기 전에는 줄어든 수가 그대로 반영된다.
- 참여·취소는 **예정·진행 중** 챌린지만 할 수 있다. 종료된 챌린지는 409 `CHALLENGE_002`다. **달성한 챌린지는 취소할 수 없다** (409 `CHALLENGE_003`).
- 취소하면 참여 행과 그 챌린지의 내 `CHALLENGE_JOIN` 활동을 지운다 (피드에 "참여했어요"가 남지 않는다). 다시 참여할 수 있고, 그때 시작일부터 다시 세며 `CHALLENGE_JOIN`을 새로 남긴다.
- 참여·취소는 **멱등**이다 (F-12 찜·F-04 팔로우와 같다). 이미 참여한 상태의 참여, 참여하지 않은 상태의 취소는 200이고 아무것도 바꾸지 않는다.
- 참여한 챌린지 목록은 **공개**다 (프로필 SC-18). 피드에 이미 참여·달성이 공개되므로 범위가 같다.

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC3_진행_중_챌린지에_참여하면_참여_행과_JOIN_활동이_생긴다()`
> BE 테스트는 다른 테스트 데이터에 기대지 않는다. 회원·챌린지·참여·활동 행을 테스트마다 직접 넣는다. 오늘은 `@TestBean Clock`으로 **2026-11-24(Asia/Seoul)**에 고정한다.
> 판단 순서: **401 → 404 `CHALLENGE_404`(·`USER_404`) → 400 검증 → 409**.

### 백엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 챌린지 C1(11-01~11-10 종료), C2(11-20~11-30 진행 중, 참여 2명), C3(11-10~11-25 진행 중), C4(12-01~12-31 예정), C5(12-05~12-20 예정)가 있을 때 | **비로그인**이 `GET /api/challenges`를 요청하면 | 200 `PageResponse`. `status`를 빼면 `ONGOING`이고 `items`는 `[C3, C2]`(종료일 오름차순, 같으면 `id DESC`)다. `?status=UPCOMING`은 `[C4, C5]`(시작일 오름차순), `?status=ENDED`는 `[C1]`(종료일 내림차순)이다. 항목마다 `status`, `participantCount`(C2 = 2)가 있고 `joined`는 `false`다. 회원 U가 C2에 참여한 상태로 로그인해 요청하면 C2만 `joined: true`다. `?status=DONE`, `page=0`, `size=51`은 400이다. `size`는 기본 12, 최대 50이다 |
| AC2 | 진행 중 챌린지 C가 있을 때 | `GET /api/challenges/{C}` | 200과 3절 `ChallengeDetailResponse`(설명 포함)를 받는다. 비로그인이거나 참여하지 않은 회원이면 `participation`이 `null`이다. 참여한 회원이면 `{progress, goalValue, completedAt, joinedAt}`이다. 없는 id → 404 `CHALLENGE_404` |
| AC3 | 회원 U와 진행 중 C, 예정 F, 종료 E가 있을 때 | `POST /api/challenges/{C}/participation` | 200 `{joined: true, participantCount: 1, participation: {...}}`. `challenge_participants` 행과 `CHALLENGE_JOIN`(target = C) 활동이 1건 생긴다. **한 번 더 보내도** 200이고 행·활동이 늘지 않는다. F도 참여할 수 있다(`progress` 0). E는 409 `CHALLENGE_002`이고 행이 생기지 않는다. 없는 id → 404, 비로그인 → 401 |
| AC4 | U가 C(11-20~11-30, `QUIZ_COUNT` 2)의 기간 안에 서로 다른 퀴즈 2개를 이미 풀었을 때 | C에 참여하면 | 응답 `participation.progress` 2, `completedAt`이 오늘 시각이다. `CHALLENGE_JOIN`과 `CHALLENGE_COMPLETE` 활동이 하나씩 생긴다. 11-19(시작 전)에 푼 풀이는 세지 않는다 |
| AC5 | U가 C(11-20~11-30, `REVIEW_COUNT` 3)에 참여했을 때 | 복습 계획을 DONE으로 바꾸면 (`PATCH /api/review-plans/{id}`) | `progress`가 1씩 오른다. TODO로 되돌리거나 DONE 계획을 지우면 1 줄어든다. 기간 밖(11-19 이전)에 끝낸 계획과 TODO 계획은 세지 않는다. 세 번째 DONE에서 `completedAt`이 채워지고 `CHALLENGE_COMPLETE`가 1건 생긴다 |
| AC6 | U가 C(진행 중, `QUIZ_COUNT` 10)에 참여했을 때 | 퀴즈를 제출하면 (`POST /api/quizzes/{id}/attempts`) | 퀴즈 Q1을 오늘 3번 풀면 1, Q1을 다른 날(기간 안) 풀었던 기록이 있으면 +1, Q2를 풀면 +1이다. 삭제한 분석의 퀴즈 풀이도 센다 |
| AC7 | U가 C(11-20~11-30, `ANALYSIS_COUNT` 5)에 참여했을 때 | 분석이 DONE이 되면 (`AnalysisService.complete`) | 기간 안에 요청해 DONE이 된 분석만 센다. FAILED·진행 중인 분석은 세지 않고, 삭제한 DONE 분석은 센다. 11-30 23:59에 요청해 12-01에 DONE이 된 분석도 C에 들어간다(요청 날짜 기준) |
| AC8 | U가 같은 기간의 `REVIEW_COUNT` 챌린지 R(목표 1)과 `QUIZ_COUNT` 챌린지 Q, 예정 챌린지 F(`REVIEW_COUNT`)에 참여했을 때 | 복습 1건을 DONE으로 바꾸면 | R만 달성되고 Q·F의 `progress`는 그대로다(F는 아직 기간 전이라 0). 그 뒤 그 복습을 TODO로 되돌려도 R의 `completedAt`·`progress` 1은 그대로이고, 다시 DONE으로 바꿔도 `CHALLENGE_COMPLETE`가 늘지 않는다 |
| AC9 | U가 진행 중 C에 참여해 있을 때 | `DELETE /api/challenges/{C}/participation` | 200 `{joined: false, participantCount: 0, participation: null}`. 참여 행과 C를 가리키는 U의 `CHALLENGE_JOIN` 활동이 지워진다(다른 회원·다른 챌린지 활동은 남는다). 참여하지 않은 상태에서 보내도 200이다. 달성한 챌린지는 409 `CHALLENGE_003`, 종료 챌린지는 409 `CHALLENGE_002`이고 아무것도 지워지지 않는다. 취소 뒤 다시 참여하면 시작일부터 다시 센 `progress`와 새 `CHALLENGE_JOIN`이 생긴다 |
| AC10 | U가 진행 중 2개(하나 달성), 예정 1개, 종료 1개에 참여했을 때 | `GET /api/users/me/challenges?status=&page=&size=` | 200 `PageResponse<MyChallengeResponse>`. `status`를 빼면 전체이고 참여 시각 최신순이다. `?status=ONGOING`이면 진행 중 2개만이다. 항목마다 챌린지 요약과 `progress`·`goalValue`·`completedAt`·`joinedAt`이 있다. 비로그인 → 401. 잘못된 `status`·`page`·`size` → 400 |
| AC11 | 회원 U가 챌린지 2개에 참여했을 때 | **비로그인**이 `GET /api/users/{U}/challenges` | AC10과 같은 응답을 받는다. 없는 회원·탈퇴한 회원 → 404 `USER_404` |

### 프론트엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC12 | 누구나 | `/challenges?status=&page=`(SC-12)를 열면 | 상태 탭(진행 중·예정·종료, 로그인했으면 "내 챌린지" 추가. 쿼리에 반영)과 `ChallengeCard` 격자(대표 이미지 또는 목표 유형별 기본 그림 · 제목 · 기간 "2026.12.01 ~ 12.31" · 목표 "퀴즈 10회" · 참여자 n명 · 참여 중이면 "참여 중" 배지)가 보인다. "내 챌린지" 탭은 `/api/users/me/challenges`로 달성도 바와 "달성" 배지를 보여 준다. 빈 목록 `EmptyState`(탭별 문구), 로딩·에러, `PageNav`. 헤더 nav에 "챌린지"(order 9, 비로그인에도 보임) |
| AC13 | 누구나 | `/challenges/:id`(SC-13)를 열면 | 이미지, 제목, 상태 배지, 기간과 "D-n"(진행 중은 종료까지, 예정은 시작까지. Asia/Seoul 오늘 기준), 목표, 참여자 수, 설명이 보인다. 버튼: 비로그인 "참여하기" → 로그인 후 이 화면으로 돌아옴 / 미참여 "참여하기" / 참여 중 달성도 바 "3 / 10회"와 "참여 취소"(화면 안 확인 모달) / 달성 "달성 완료 · 11월 24일" 배지(취소 버튼 없음) / 종료 미참여는 버튼 없이 "종료된 챌린지입니다". 요청 중 버튼 비활성, 409·그 밖의 에러는 버튼 아래 서버 메시지, 없는 id는 "챌린지를 찾을 수 없습니다" |
| AC14 | 로그인 회원이 | 대시보드(SC-04)를 열면 | "참여 중 챌린지" 절이 진행 중 참여 챌린지 최대 3개(제목·D-n·달성도 바, 누르면 SC-13)와 "전체 보기"(`/challenges?status=MY`)를 보여 준다. 없으면 "참여 중인 챌린지가 없습니다." + "챌린지 둘러보기". 이 절만 실패하면 절 안에 메시지와 "다시 시도"가 보이고 다른 절은 그대로다 |
| AC15 | 누구나 | 프로필(SC-18)을 열면 | "참여 챌린지 N" 절이 참여 시각 최신순 6개(제목·기간·달성도 바, 달성이면 "달성" 배지, 누르면 SC-13)와 "더 보기"를 보여 준다. 0건이면 "아직 참여한 챌린지가 없습니다."(내 프로필이면 "챌린지 둘러보기" 링크). 이 절만 실패하면 절 안에 서버 메시지를 보이고 프로필은 그대로다 |
| AC16 | 브라우저 시간이 UTC 2026-11-24 15:00:01일 때 (#99) | `todayInSeoul()`을 부르면 | `'2026-11-25'`를 돌려준다. 관리자 챌린지 폼의 "종료일 ≥ 오늘" 검증과 SC-13의 D-n이 이 함수를 쓴다 |

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다 (contract-checker, CI가 검사). Swagger Tag: `챌린지`. 공개 GET은 `@SecurityRequirements()`.
> 날짜는 `yyyy-MM-dd`, 시각은 F-00b 공통 형식이다. `status` 값 `UPCOMING`·`ONGOING`·`ENDED`(`ChallengeStatus`), `goalType` 값 `REVIEW_COUNT`·`QUIZ_COUNT`·`ANALYSIS_COUNT`(`ChallengeGoalType`).

### `GET /api/challenges?status=&page=&size=`

- 권한: 공개 (로그인했으면 `joined` 계산)
- `status`: `ONGOING`(기본)·`UPCOMING`·`ENDED`. 정렬: 진행 중 `end_date ASC`, 예정 `start_date ASC`, 종료 `end_date DESC`, 모두 다음 기준 `id DESC`
- 응답 `200`

```json
{
  "items": [
    {
      "id": 7,
      "title": "12월 퀴즈 챌린지",
      "imageUrl": "/uploads/challenges/3f2b9c1e-8a4d-4f7e-9b1a-2c5d6e7f8a90.png",
      "startDate": "2026-11-20",
      "endDate": "2026-11-30",
      "goalType": "QUIZ_COUNT",
      "goalValue": 10,
      "status": "ONGOING",
      "participantCount": 2,
      "joined": true
    }
  ],
  "page": 1,
  "size": 12,
  "total": 1
}
```

### `GET /api/challenges/{id}`

- 권한: 공개
- 응답 `200` `ChallengeDetailResponse`

```json
{
  "id": 7,
  "title": "12월 퀴즈 챌린지",
  "description": "분석한 프로젝트의 퀴즈를 기간 안에 10번 풀어 보세요.",
  "imageUrl": "/uploads/challenges/3f2b9c1e-8a4d-4f7e-9b1a-2c5d6e7f8a90.png",
  "startDate": "2026-11-20",
  "endDate": "2026-11-30",
  "goalType": "QUIZ_COUNT",
  "goalValue": 10,
  "status": "ONGOING",
  "participantCount": 2,
  "participation": {
    "progress": 3,
    "goalValue": 10,
    "completedAt": null,
    "joinedAt": "2026-11-22T21:05:00+09:00"
  }
}
```

### `POST /api/challenges/{id}/participation` · `DELETE /api/challenges/{id}/participation`

- 권한: 로그인 필요. 본문 없음. 둘 다 멱등.
- 응답 `200` `ParticipationResponse` (POST 예시. DELETE 성공은 `joined: false`, `participation: null`)

```json
{
  "joined": true,
  "participantCount": 3,
  "participation": {
    "progress": 2,
    "goalValue": 2,
    "completedAt": "2026-11-24T10:15:00+09:00",
    "joinedAt": "2026-11-24T10:15:00+09:00"
  }
}
```

### `GET /api/users/me/challenges?status=&page=&size=` · `GET /api/users/{id}/challenges?status=&page=&size=`

- 권한: `me`는 로그인 필요, `{id}`(숫자)는 공개
- `status`: 없음(전체, 기본)·`ONGOING`·`UPCOMING`·`ENDED`. 정렬 `joined_at DESC, challenge_id DESC`. `size` 기본 12, 최대 50
- 응답 `200` `PageResponse<MyChallengeResponse>`

```json
{
  "items": [
    {
      "id": 7,
      "title": "12월 퀴즈 챌린지",
      "imageUrl": null,
      "startDate": "2026-11-20",
      "endDate": "2026-11-30",
      "goalType": "QUIZ_COUNT",
      "goalValue": 10,
      "status": "ONGOING",
      "progress": 3,
      "completedAt": null,
      "joinedAt": "2026-11-22T21:05:00+09:00"
    }
  ],
  "page": 1,
  "size": 12,
  "total": 1
}
```

### 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 400 | COMMON_400 | 잘못된 `status`·`page`·`size` |
| 401 | AUTH_401 | 참여·취소·`/users/me/challenges`에 토큰 없음·만료 |
| 404 | CHALLENGE_404 | 없는 챌린지 (상세·참여·취소) |
| 404 | USER_404 | `/users/{id}/challenges`의 없는·탈퇴 회원 |
| 409 | CHALLENGE_002 | 종료된 챌린지 참여·취소 — **새 코드** "종료된 챌린지는 참여하거나 취소할 수 없습니다." |
| 409 | CHALLENGE_003 | 달성한 챌린지 취소 — **새 코드** "달성한 챌린지는 참여를 취소할 수 없습니다." |

> `docs/conventions.md` 3절 `CHALLENGE` 행에 두 코드를 더한다.

## 4. DB 변경

- Flyway: **없음** (`challenges`, `challenge_participants(progress, completed_at, joined_at, PK(challenge_id, user_id), KEY(user_id))`는 V1, `activities.type`의 `CHALLENGE_JOIN`·`CHALLENGE_COMPLETE`는 V2에 있다)
- 사용 테이블: `challenges`(읽기, `FOR UPDATE` 잠금), `challenge_participants`(읽기·쓰기), `activities`(JOIN·COMPLETE 기록, JOIN 삭제), `review_plans`·`quiz_attempts`·`analyses`(집계 읽기), `users`(공개 목록의 회원 확인)
- 기간 조건은 `TeamChallengeMapper.xml`과 같이 `created_at >= start_date AND created_at < end_date + INTERVAL 1 DAY`로 쓴다 (DB 세션 +09:00 고정, #17).

## 5. 구현 메모

### 백엔드 (`com.vibenote.challenge`)

| 항목 | 내용 |
|---|---|
| 회원 API | `ChallengeController`(`/api/challenges`, Tag `챌린지`), `UserChallengeController`(`/api/users/me/challenges`, `/api/users/{id}/challenges`), `ChallengeService`. 선택 인증은 `ConceptController.detail`처럼 `@AuthenticationPrincipal AuthUser user`(null 허용) |
| 공개 경로 | `SecurityConfig.PUBLIC_GET_PATHS`의 `CHALLENGE (A F-25)` 블록에 `/api/challenges`, `/api/challenges/{id:\\d+}`, `/api/users/{id:\\d+}/challenges` |
| 집계 | `ChallengeProgressService.refresh(userId, goalType, activityDate)`: `activityDate`가 기간 안이고 **미달성**인 그 유형의 참여 행을 `FOR UPDATE`로 잡고, 유형별 COUNT로 `progress = min(count, goal)`을 쓴다. `UPDATE … SET completed_at = ? WHERE … AND completed_at IS NULL AND progress >= goal_value`가 1행이면 `CHALLENGE_COMPLETE` 기록. 참여 직후에는 그 챌린지 한 건만 같은 방식으로 센다 |
| 집계 호출 | 같은 트랜잭션에서: `ReviewPlanService.changeStatus`(DONE이면 오늘, TODO로 되돌리면 이전 `done_at` 날짜) · `delete`(DONE 계획이면 그 `done_at` 날짜) · `QuizService.submit`(오늘) · `AnalysisService.complete`(분석 `created_at` 날짜). 셋 다 A 영역 |
| Mapper | `ChallengeParticipantMapper`(+ `mapper/challenge/ChallengeParticipantMapper.xml`): insert(`INSERT IGNORE`, 영향 행 수로 첫 참여 판단), delete, find, 목록·COUNT, 유형별 집계 COUNT 3개, progress·completed 갱신. 공개 목록·상세는 기존 `ChallengeMapper`에 회원용 쿼리를 더한다. 집계 SQL이 `review_plans` 등 다른 테이블을 읽지만 다른 도메인 Mapper를 부르지는 않는다 (FeedMapper와 같은 방식) |
| 동시성 | 참여·취소는 챌린지 행 `FOR UPDATE`로 직렬화한다. 같은 사람의 두 활동이 동시에 달성을 만들어도 `completed_at IS NULL` 조건부 UPDATE라 COMPLETE는 1건이다 |
| 활동 삭제 | `feed.ActivityService`에 `delete(userId, type, targetId)` 추가 (공통 feed 패키지. PR 때 `node scripts/owners.mjs --diff origin/develop`로 @멘션 여부 확인) |
| 에러 코드 | `ErrorCode` CHALLENGE 블록에 `CHALLENGE_002`, `CHALLENGE_003` |
| 시각 | `Clock` 빈(Asia/Seoul). 상태는 `ChallengeStatus.of` |

### 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | `routes/challenge.js` 회원 배열: `/challenges`(`challenge-list`, `meta.nav {label:'챌린지', order:9}`, 공개) → `views/challenge/ChallengeListView.vue`, `/challenges/:id(\\d+)`(`challenge-detail`, 공개) → `ChallengeDetailView.vue`. "내 챌린지" 탭은 `?status=MY`(비로그인이면 진행 중 탭으로) |
| 컴포넌트 | `components/challenge/ChallengeCard.vue`, `ChallengeProgressBar.vue`, `ChallengeJoinPanel.vue`(버튼·모달), `MyChallengeList.vue`(대시보드·프로필 절 공용). `components/challenge/admin/labels.js`를 `components/challenge/labels.js`로 옮겨 관리자·회원 화면이 같이 쓴다 |
| 공용 유틸 (#99) | `utils/seoulDate.js`: `todayInSeoul(now = new Date())`(`Intl.DateTimeFormat('en-CA', {timeZone:'Asia/Seoul'})`), `daysBetween(a, b)`. `components/challenge/admin/validation.js`의 `todayIso`를 이것으로 바꾼다 |
| 대시보드·프로필 | `views/dashboard/DashboardView.vue`에 "참여 중 챌린지" 절, `views/UserProfileView.vue`에 "참여 챌린지" 절 (각자 따로 불러오고 실패를 절 안에 가둔다) |
| store | 없음 (화면 로컬 상태. 로그인 여부는 기존 auth store) |
| API 함수 | `src/api/challenges.js`: `fetchChallenges(params)`, `fetchChallenge(id)`, `joinChallenge(id)`, `cancelChallenge(id)`, `fetchMyChallenges(params)`, `fetchUserChallenges(userId, params)` |
| 상태별 UI | 로딩 / 빈 목록 / 에러 메시지 / 요청 중 버튼 비활성 / 종료·달성 상태 |

## 6. LLM (AI 기능만)

해당 없음.

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1·AC2 | `challenge/ChallengeApiTest` | MockMvc 통합 |
| AC3·AC4·AC9 | `challenge/ChallengeParticipationApiTest` | MockMvc 통합 |
| AC5~AC8 | `challenge/ChallengeProgressTest` (실제 `PATCH /api/review-plans`·`POST /api/quizzes/{id}/attempts`·`AnalysisService.complete` 경로로 활동을 만든다) | 통합 |
| AC10·AC11 | `challenge/UserChallengeApiTest` | MockMvc 통합 |
| 공개 경로 | `common/config/SecurityConfigTest`(비로그인 GET 3개 200, POST·DELETE·`/users/me/challenges` 401) | MockMvc 통합 |
| AC12 | `views/challenge/__tests__/ChallengeListView.spec.js`, `components/challenge/__tests__/ChallengeCard.spec.js`, `utils/__tests__/navItems.spec.js` | Vitest |
| AC13 | `views/challenge/__tests__/ChallengeDetailView.spec.js`, `components/challenge/__tests__/ChallengeJoinPanel.spec.js` | Vitest |
| AC14·AC15 | `views/dashboard/__tests__/DashboardView.spec.js`, `views/__tests__/UserProfileView.spec.js`, `components/challenge/__tests__/MyChallengeList.spec.js` | Vitest |
| AC16 | `utils/__tests__/seoulDate.spec.js`, 기존 `AdminChallengeForm.spec.js` | Vitest |
| API 모듈 | `api/__tests__/challenges.spec.js` | Vitest |

검증: `node scripts/verify.mjs all`, contract-checker(spec ↔ openapi.json ↔ `api/challenges.js`), ui-verifier(SC-12·13 비로그인, 데스크톱·모바일). 로그인이 필요한 참여·대시보드·프로필 절은 연결된 Chrome으로 확인하고 스크린샷을 `docs/산출물/screens/SC-12-*.png`·`SC-13-*.png`로 남긴다.

## 8. 범위 밖

- 랭킹·배지·포인트, 알림, 챌린지 댓글·공유, 참여자 목록 화면, 관리자 화면의 참여자별 달성도
- 서버 배치(스케줄러)로 달성도 재계산, 지난 활동 소급 재집계 도구
- 시연용 챌린지 시드 데이터 (로컬 DB에 관리자 화면으로 만든다)
- 팀 기간 목표(`team_challenges`) → F-20c(B)

## 9. 결정 필요

- 없음 (11-24 사용자 결정: 집계는 챌린지 시작일부터 · 퀴즈는 (퀴즈, 날짜)당 1회 · 미달성·종료 전만 취소하고 JOIN 활동 삭제 · 참여 챌린지 목록 공개)

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [ ] 브랜치 `feature/F-25-challenge-participation`
- [ ] Flyway 마이그레이션 (없음 확인)
- [ ] AC별 실패 테스트 작성 (red 확인)
- [ ] BE 구현 (green)
- [ ] FE 구현 (green)
- [ ] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [ ] `docs/conventions.md` 3절 CHALLENGE 에러 코드
- [ ] contract-checker 통과
- [ ] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일)
- [ ] reviewer 지적 사항 반영
- [ ] 산출물 반영 (화면 설계서 SC-12·13 절·스크린샷, SC-04·18 챌린지 절, 유스케이스 UC33 "기획 기준" 해제·대안 흐름)
- [ ] PR CI 초록
- [ ] 상태 DONE (PR 머지 후)
