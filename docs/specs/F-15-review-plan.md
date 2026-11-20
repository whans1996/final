# F-15 복습 계획 (목록·직접 추가·완료 체크·삭제 + 찜 자동 추가 메서드 + SC-11)

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-15 |
| 우선순위 | P0 |
| 영역 오너 | A 김동환 (AI·분석·학습 루프) |
| 관련 필수 요구사항 | ⑫ 계획/챌린지(복습 계획). 명세서 "추가 · 챌린지"의 달성도 재료(F-25가 `review_plans` 완료를 센다) |
| 의존 spec | F-14 코드 이해 퀴즈(`reviewplan` 패키지, `ReviewPlanService.addWrongAnswers`, 오답 자동 추가 WRONG), F-00b 공통 기반(`ConceptQueryService`·`ConceptSummaryResponse`·`ConceptCard.vue`·`EmptyState.vue`·route 메타 nav), F-10 카드 상세(SC-10 `ConceptDetailView.vue`) — 모두 DONE. F-12 찜(B)이 이 기능의 `addWish`를 부른다. F-03 학습 통계·F-16 추천·F-25 챌린지가 이 기능의 `DONE` 기록을 읽는다 |
| 관련 이슈 | 없음 |
| 승인 | whans1996 2026-11-20 |

## 1. 목적

퀴즈에서 틀린 개념(F-14)과 찜한 카드(F-12)가 **날짜별 복습 목록**에 쌓인다. 회원은 SC-11에서 오늘과 앞으로 7일의 복습 카드, 그리고 지난 날짜에 끝내지 못한 **밀린 복습**을 보고, 카드를 다시 읽은 뒤 "완료"를 체크한다(UC31). 카드 상세(SC-10)의 "복습에 추가"로 직접 담을 수도 있다. 완료 기록은 학습 통계(F-03), 추천 제외(F-16), 챌린지 달성도(F-25)의 재료가 된다.

**규칙(F-14에서 이어짐)**: 한 회원의 한 카드에 대한 `TODO` 계획은 날짜와 상관없이 **하나만** 둔다. 오답(WRONG), 찜(WISH), 직접(MANUAL) 추가 모두 이 규칙을 따르고, `(user_id, concept_id, plan_date)` UNIQUE(V2)가 같은 날짜의 중복을 막는다. "오늘"은 서비스 시계(`ClockConfig.ZONE` = Asia/Seoul)의 날짜다.

**보이는 계획**: 카드가 `PUBLISHED`인 계획만 존재하는 것으로 본다. 관리자가 카드를 숨기면(`DRAFT`·`HIDDEN`) 그 카드의 계획은 목록에서 빠지고, 조회·수정·삭제 API에서 `REVIEW_PLAN_404`로 답한다 (행은 지우지 않는다. 다시 공개하면 돌아온다).

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_기간을_주지_않으면_오늘부터_7일을_돌려준다()`. BE 테스트는 고정 Clock(2026-11-20T10:00 Asia/Seoul)으로 쓴다.

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 회원에게 여러 날짜의 계획(TODO·DONE, WRONG·WISH·MANUAL)이 있고 다른 회원의 계획도 있을 때 | `GET /api/review-plans?from=&to=` 하면 | 200. `today`(오늘), `from`·`to`(쿼리를 생략하면 `from`=오늘, `to`=오늘+6일), `items` = 내 계획 중 `from ≤ planDate ≤ to`인 것(TODO·DONE 모두)을 `planDate` 오름차순, 같은 날짜는 `id` 오름차순으로 준다. 항목은 `id, planDate, status, reason, doneAt(DONE일 때 +09:00 시각, 아니면 null), concept(ConceptSummaryResponse)`. 다른 회원의 계획과 `PUBLISHED`가 아닌 카드의 계획은 넣지 않는다 |
| AC2 | 오늘보다 이전 날짜의 TODO 계획(밀린 복습)이 있을 때 | `GET /api/review-plans` 하면 | 응답 `overdue`에 `planDate < today`이고 `TODO`인 내 계획(`PUBLISHED` 카드만)을 `from`·`to`와 상관없이 `planDate`·`id` 오름차순으로 모두 담는다. 지난 날짜의 DONE은 `overdue`에 넣지 않는다. 없으면 `[]` |
| AC3 | 잘못된 조회가 | `GET /api/review-plans`로 오면 | `from`·`to`가 `YYYY-MM-DD`가 아님, `from > to`, 기간이 31일 초과(`to - from > 30일`), 한쪽만 줌 → 400 `COMMON_400`(`details`에 필드). 토큰 없으면 401 `AUTH_401` |
| AC4 | 회원이 `PUBLISHED` 카드를 | `POST /api/review-plans`로 `{conceptId, planDate?}`를 보내면 | 201. `reason`=`MANUAL`, `status`=`TODO`, `planDate` 생략 시 내일, 준 경우 그 날짜로 저장하고 AC1의 항목 모양으로 돌려준다 |
| AC5 | 잘못된 추가가 | `POST /api/review-plans`로 오면 | `conceptId` 없음·0 이하, `planDate`가 오늘보다 이전이거나 오늘+365일보다 뒤 → 400 `COMMON_400`(`details`에 필드). 없는 카드·`PUBLISHED`가 아닌 카드 → 404 `CONCEPT_404`. 그 카드의 TODO 계획이 날짜와 상관없이 이미 있음, 또는 같은 날짜의 계획(DONE 포함)이 있음 → 409 `REVIEW_PLAN_409`. 토큰 없으면 401. 에러 때는 아무것도 저장하지 않는다 |
| AC6 | 회원이 자기 계획을 | `PATCH /api/review-plans/{id}`로 `{status}`를 보내면 | 200, AC1의 항목 모양. `TODO→DONE`이면 `doneAt`=지금(서비스 시계), `DONE→TODO`이면 `doneAt`=null. 이미 같은 상태면 바꾸지 않고(`doneAt` 유지) 200. `DONE→TODO`인데 그 카드의 다른 TODO 계획이 있으면 409 `REVIEW_PLAN_409`. `status`가 없거나 `TODO`·`DONE`이 아니면 400 `COMMON_400` |
| AC7 | 계획 id가 | `PATCH`·`DELETE /api/review-plans/{id}`로 오면 | 내 계획이면 `DELETE`는 204로 지운다(다시 조회하면 없다). 없는 id, 다른 회원의 계획, `PUBLISHED`가 아닌 카드의 계획 → 404 `REVIEW_PLAN_404`(다른 회원 계획은 바꾸거나 지우지 않는다). 토큰 없으면 401 |
| AC8 | F-12(찜)가 회원의 찜을 처리하며 | `ReviewPlanService.addWish(userId, conceptId)`를 부르면 | 내일 날짜, `reason`=`WISH`, `TODO`로 넣고 `Optional<Added>`를 돌려준다. 그 카드의 TODO가 이미 있거나 내일 날짜의 계획이 있으면 넣지 않고 `Optional.empty()`. 호출자의 트랜잭션에 참여한다(호출자가 롤백하면 함께 롤백). 카드가 `PUBLISHED`인지는 호출자가 확인한다 (F-14 `addWrongAnswers`와 같은 계약) |
| AC9 | 회원이 SC-11(`/review-plans`)을 열면 | 처음 화면에서 | 제목 "복습 계획", 기간 "11월 20일 ~ 11월 26일"과 "◀ 이전 7일"·"오늘"·"다음 7일 ▶" 버튼. 기간이 오늘부터 시작할 때만 맨 위에 "밀린 복습 {n}개" 절(overdue가 비면 숨김). 그 아래 항목이 있는 날짜마다 "11월 20일 (금)" 머리글(오늘이면 " · 오늘")과 항목: 완료 체크박스, `ConceptCard`(누르면 `/concepts/{id}`), 이유 배지(오답·찜·직접), "삭제" 버튼. DONE 항목은 흐리게 보이고 체크되어 있다. 밀린 복습과 기간 안에 항목이 하나도 없으면 `EmptyState` "이 기간에 복습할 카드가 없습니다."와 "개념 카드 둘러보기"(`/concepts`) 링크. 조회 중에는 "불러오는 중…", 실패하면 서버 `message`와 "다시 시도". 375px에서 페이지 가로 스크롤이 없다 |
| AC10 | SC-11에서 | 체크박스를 누르거나 "삭제"를 누르면 | 요청 중에는 그 항목의 체크박스·버튼을 끄고, 성공하면 응답으로 그 항목을 바꾼다(밀린 복습 항목을 완료하면 밀린 복습 절에서 빠지고 개수가 줄어든다) · 삭제 성공 시 목록에서 뺀다. 실패하면 체크 상태를 되돌리고 서버 `message`를 그 항목 아래에 보여 준다. 확인 대화상자는 띄우지 않는다 |
| AC11 | 로그인한 회원이 SC-10 카드 상세에서 | "복습에 추가"를 누르면 | `POST /api/review-plans`(`planDate` 생략)를 보내고, 요청 중 버튼을 끈다. 성공하면 "{M월 D일} 복습 계획에 추가했습니다."와 "복습 계획 보기"(`/review-plans`) 링크, 실패(409 등)하면 서버 `message`를 버튼 옆에 보여 준다. 비로그인이면 버튼을 보이지 않는다 |
| AC12 | 회원이 | 헤더 메뉴와 SC-08 퀴즈 결과를 보면 | 헤더에 "복습" 메뉴(`meta.nav` order 30, `show: 'user'`)가 보이고 `/review-plans`로 간다(비로그인은 안 보임, 비로그인으로 주소를 열면 로그인 화면으로). SC-08 결과의 "틀린 개념 k개를 … 추가했습니다." 문구 뒤에 "복습 계획 보기"(`/review-plans`) 링크가 붙는다 |

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다. Swagger Tag: `복습 계획`. 네 엔드포인트 모두 로그인 필요(기본 인증 규칙, `PUBLIC_GET_PATHS`에 넣지 않는다).

### 공통 항목 `ReviewPlanItemResponse`

```json
{
  "id": 31,
  "planDate": "2026-11-21",
  "status": "TODO",
  "reason": "WRONG",
  "doneAt": null,
  "concept": {
    "id": 12, "name": "JWT 인증", "category": "security", "difficulty": "JUNIOR",
    "summary": "서버가 서명한 토큰으로 요청한 사람을 확인하는 방식이다.", "avgRating": 4.5, "wishCount": 12
  }
}
```

- `status`: `TODO` | `DONE`. `reason`: `WRONG`(퀴즈 오답, F-14) | `WISH`(찜, F-12) | `MANUAL`(직접 추가)
- `doneAt`: DONE이면 `"2026-11-20T10:00:00+09:00"`, 아니면 `null`

### 3.1 `GET /api/review-plans?from=2026-11-20&to=2026-11-26`

- 쿼리: `from`, `to` (`YYYY-MM-DD`, 둘 다 주거나 둘 다 생략. 생략하면 오늘 ~ 오늘+6일. `from ≤ to`, `to - from ≤ 30일`)
- 응답 `200`

```json
{
  "today": "2026-11-20",
  "from": "2026-11-20",
  "to": "2026-11-26",
  "overdue": [
    { "id": 25, "planDate": "2026-11-18", "status": "TODO", "reason": "WRONG", "doneAt": null, "concept": { "id": 7, "name": "트랜잭션", "category": "spring", "difficulty": "JUNIOR", "summary": "...", "avgRating": 0.0, "wishCount": 0 } }
  ],
  "items": [
    { "id": 30, "planDate": "2026-11-20", "status": "DONE", "reason": "MANUAL", "doneAt": "2026-11-20T09:12:00+09:00", "concept": { "id": 3, "...": "..." } },
    { "id": 31, "planDate": "2026-11-21", "status": "TODO", "reason": "WRONG", "doneAt": null, "concept": { "id": 12, "...": "..." } }
  ]
}
```

- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 400 | COMMON_400 | 날짜 형식 오류, 한쪽만 줌, `from > to`, 31일 초과 (`details`에 `from`·`to`) |
| 401 | AUTH_401 | 토큰 없음·만료 |

### 3.2 `POST /api/review-plans`

- 요청

```json
{ "conceptId": 12, "planDate": "2026-11-21" }
```

- `planDate` 선택. 생략하면 내일. 오늘 ~ 오늘+365일
- 응답 `201`: `ReviewPlanItemResponse` (`reason` = `MANUAL`, `status` = `TODO`)
- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 400 | COMMON_400 | `conceptId` 없음·0 이하, `planDate` 범위 밖 (`details`에 필드) |
| 401 | AUTH_401 | 토큰 없음·만료 |
| 404 | CONCEPT_404 | 없는 카드, `PUBLISHED`가 아닌 카드 |
| 409 | REVIEW_PLAN_409 | 그 카드의 TODO 계획이 이미 있음, 또는 같은 날짜의 계획(DONE 포함)이 있음. message "이미 복습 계획에 있는 카드입니다." |

### 3.3 `PATCH /api/review-plans/{id}`

- 요청

```json
{ "status": "DONE" }
```

- 응답 `200`: `ReviewPlanItemResponse`
- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 400 | COMMON_400 | `status` 없음 또는 `TODO`·`DONE`이 아님 |
| 401 | AUTH_401 | 토큰 없음·만료 |
| 404 | REVIEW_PLAN_404 | 없는 계획, 다른 회원의 계획, `PUBLISHED`가 아닌 카드의 계획. message "복습 계획을 찾을 수 없습니다." |
| 409 | REVIEW_PLAN_409 | `DONE→TODO`인데 그 카드의 다른 TODO 계획이 있음 |

### 3.4 `DELETE /api/review-plans/{id}`

- 응답 `204`
- 에러: 401 `AUTH_401`, 404 `REVIEW_PLAN_404` (3.3과 같은 조건)

### 새 에러 코드 (`ErrorCode`의 `// ── REVIEW_PLAN (A F-15) ──` 블록)

| 코드 | HTTP | message |
|---|---|---|
| `REVIEW_PLAN_404` | 404 | 복습 계획을 찾을 수 없습니다. |
| `REVIEW_PLAN_409` | 409 | 이미 복습 계획에 있는 카드입니다. |

`docs/conventions.md` 3절 `REVIEW_PLAN` 행에 두 코드를 적는다.

## 4. DB 변경

- Flyway: **없음**. `review_plans`(V1)와 UNIQUE `uk_review_plans_user_concept_date`(V2)를 그대로 쓴다. 기간 조회·밀린 복습은 `idx_review_plans_user_date (user_id, plan_date)`를 탄다.
- 사용 테이블: `review_plans`(읽기·쓰기, A), `concepts`(읽기, `PUBLISHED` 확인과 요약. `ConceptQueryService`를 통해서만)

## 5. 백엔드 구조

- `reviewplan/ReviewPlanController` (`@Tag(name = "복습 계획")`): 3.1~3.4. `@AuthenticationPrincipal`로 회원 id를 받는다.
- `reviewplan/ReviewPlanService`
  - `list(userId, from, to)`: 오늘 기준 기본값·검증(AC3) → `mapper.findInRange`·`mapper.findOverdue` → 카드 요약은 `ConceptQueryService.findPublishedSummaries(ids)`로 한 번에 받아 붙이고, 요약이 없는(=`PUBLISHED`가 아닌) 계획은 뺀다
  - `addManual(userId, conceptId, planDate)`: `ConceptQueryService.getPublished`(없으면 `CONCEPT_404`) → `existsTodo`면 409 → `insertIgnoringDuplicate`가 0이면 409
  - `changeStatus(userId, planId, status)`, `delete(userId, planId)`: `findOwned(userId, planId)` + 카드 `PUBLISHED` 확인, 아니면 `REVIEW_PLAN_404`. `DONE→TODO`는 `existsOtherTodo(userId, conceptId, planId)`면 409
  - `addWish(userId, conceptId)` (AC8): `addWrongAnswers`와 같은 내부 메서드(`addIfNoTodo(userId, conceptId, planDate, reason)`)를 함께 쓴다. F-12가 호출한다
- `ReviewPlanMapper`(+XML): `findInRange`, `findOverdue`, `findOwned`, `existsOtherTodo`, `updateStatus(id, status, doneAt)`, `deleteOwned(userId, id)` 추가. 기존 `existsTodo`·`insertIgnoringDuplicate`는 그대로 쓴다
- DTO `reviewplan/dto/`: `ReviewPlanItemResponse`, `ReviewPlanListResponse`, `ReviewPlanCreateRequest`(`@NotNull @Positive conceptId`, `planDate`), `ReviewPlanStatusRequest`(`@NotNull status`). 모든 필드에 `@Schema`
- 동시성: `addManual`의 `existsTodo` → INSERT 사이에 다른 요청이 같은 카드의 다른 날짜 TODO를 넣는 경합은 F-14와 같이 막지 않는다 (같은 날짜는 UNIQUE가 막는다). 이 경우 같은 카드의 TODO가 둘이 될 수 있으며 목록에 둘 다 보인다

## 6. LLM

해당 없음.

## 7. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | `/review-plans` → `views/reviewplan/ReviewPlanView.vue` (SC-11). 새 route 파일 `router/routes/reviewplan.js` (`requiresAuth`, `meta.nav = { label: '복습', order: 30, show: 'user' }`), `router/index.js`에 import 한 줄(공통) |
| 컴포넌트 | `components/reviewplan/ReviewPlanItem.vue`(체크박스·`ConceptCard`·이유 배지·삭제·항목 에러), `components/reviewplan/ReviewPlanAddButton.vue`(SC-10 "복습에 추가", AC11) |
| 공통 파일 변경 | `views/concept/ConceptDetailView.vue`: 헤더의 요약(`p.summary`) 아래에 `<ReviewPlanAddButton :concept-id="concept.id" />` 한 줄과 import (B가 F-11에서 같은 파일의 제목 줄·리뷰 절을 고치는 중이라 그 줄들은 건드리지 않는다. PR에 @JoonseobKim 멘션). `views/QuizView.vue`(A)의 복습 안내 뒤 `RouterLink` |
| store | 없음 (화면 로컬 상태. 로그인 여부는 `useAuthStore().isLoggedIn`) |
| API 함수 | 새 `src/api/reviewPlans.js`: `fetchReviewPlans({ from, to } = {})`, `addReviewPlan({ conceptId, planDate })`, `updateReviewPlanStatus(id, status)`, `deleteReviewPlan(id)` |
| 날짜 | 기간 이동은 응답의 `today`·`from`을 기준으로 ±7일을 계산한다(브라우저 시계를 쓰지 않는다). 라벨은 `M월 D일 (요일)` |
| 상태별 UI | 로딩 "불러오는 중…" / 빈 기간 `EmptyState` + `/concepts` 링크 (F-16이 추천 카드로 바꾼다) / 조회 에러 message + "다시 시도" / 항목 에러는 항목 아래 |
| 반응형 | 본문 최대 폭 760px, 항목은 체크박스 · 카드 · 배지·삭제가 한 줄이고 375px에서는 카드 아래로 배지·삭제가 줄바꿈 |
| CODEOWNERS | `/frontend/src/components/reviewplan/`, `/frontend/src/api/reviewPlans.js`, `/frontend/src/router/routes/reviewplan.js`, `/frontend/src/views/__tests__/ReviewPlan*` → @whans1996 (`views/reviewplan/`은 이미 있음) |

## 8. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1, AC2, AC3 | `reviewplan/ReviewPlanApiTest#AC1_…`~`AC3_…` (기본 기간, 정렬, 남의 계획·비공개 카드 제외, doneAt 형식, 밀린 복습은 기간과 무관·DONE 제외, 400 종류별·401) | MockMvc 통합 (고정 Clock) |
| AC4, AC5 | `ReviewPlanApiTest#AC4_…`, `#AC5_…` (기본 내일·지정 날짜, 400·404·409 두 경우·401, 에러 시 행 수 그대로) | MockMvc 통합 |
| AC6, AC7 | `ReviewPlanApiTest#AC6_…`, `#AC7_…` (doneAt 설정·해제·같은 상태 유지, 409, 남의 계획 404 후 행이 그대로, 204 후 조회에 없음) | MockMvc 통합 |
| AC8 | `reviewplan/ReviewPlanServiceTest#AC8_…` (WISH 내일, TODO 있으면 empty, 같은 날짜 있으면 empty, 호출자 롤백 시 함께 롤백) | 통합 (Testcontainers) |
| AC9, AC10 | `views/__tests__/ReviewPlanView.spec.js` (기간·이동, 밀린 복습 표시 조건·개수, 날짜 머리글·오늘, 배지, 빈 상태, 조회 에러·다시 시도, 체크·삭제 성공·실패 되돌림) | Vitest |
| AC11 | `components/reviewplan/__tests__/ReviewPlanAddButton.spec.js` (비로그인 숨김, 성공 문구·링크, 409 message, 요청 중 비활성) | Vitest |
| AC12 | `router/__tests__` 기존 nav 테스트에 "복습" 추가, `views/__tests__/QuizView.spec.js`에 링크 확인 | Vitest |
| API 함수 | `api/__tests__/reviewPlans.spec.js` (경로·메서드·본문) | Vitest |
| 화면 | ui-verifier: SC-11 데스크톱·375px, SC-10 버튼 | 브라우저 |

## 9. 범위 밖

- 대시보드(SC-04)의 "오늘 복습할 카드" — F-16(추천)과 함께 만든다 (11-20 결정)
- 찜(F-12) 자체와 찜 시 `addWish` 호출 — B의 F-12 spec이 이 메서드를 부른다. 찜 해제 시 계획은 그대로 둔다
- 계획 날짜 바꾸기(미루기), SC-10 버튼의 날짜 고르기(API는 `planDate`를 받지만 화면은 내일만), SC-11에서 카드 검색해 추가
- 망각곡선 1·3·7일 자동 반복(F-23, P2), 완료 시 다음 복습 자동 생성
- 복습 완료의 피드 활동(`activities`) 기록 — F-04가 정한다. 챌린지 집계는 F-25가 `review_plans.status = 'DONE'`·`done_at`으로 직접 센다
- 같은 카드의 다른 날짜 TODO가 동시 요청으로 둘 생기는 경합 방지 (5절)

## 10. 결정 필요

- 없음 (11-20 사용자 결정: 찜은 F-12가 `addWish`로 자동 추가(내일·WISH), 직접 추가는 SC-10 버튼, 밀린 복습은 SC-11 맨 위에 모음, 대시보드는 F-16. 결정로그 반영)

## 11. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/F-15-review-plan`
- [x] Flyway 마이그레이션 (없음)
- [x] AC별 실패 테스트 작성 (red 확인)
- [x] BE 구현 (green): ErrorCode 블록, Mapper·Service·Controller·DTO
- [x] FE 구현 (green): api, route·nav, SC-11, SC-10 버튼, SC-08 링크, CODEOWNERS
- [x] Swagger 어노테이션 + `docs/api/openapi.json` 갱신, conventions 3절 에러 코드
- [ ] contract-checker 통과
- [ ] ui-verifier 통과 (데스크톱·모바일)
- [ ] reviewer 지적 사항 반영
- [ ] 산출물 반영: 화면 설계서 SC-11 절·스크린샷과 SC-10·SC-08 변경, 유스케이스 UC31(기획 기준 → 구현), ERD `review_plans` 설명(F-15 추가 경로)
- [ ] PR CI 초록 (공통 파일 변경 @JoonseobKim 멘션)
- [ ] 상태 DONE (PR 머지 후)
