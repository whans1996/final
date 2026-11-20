# F-12 카드 찜

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-12 |
| 우선순위 | P0 |
| 영역 오너 | B 김준섭 (카드·소셜) |
| 관련 필수 요구사항 | ⑪ 콘텐츠 찜/즐겨찾기 |
| 의존 spec | F-02 (로그인), F-10 (카드 상세 `ConceptDetailView`·`ConceptService.getDetail`), F-03 (프로필 SC-18 `UserProfileView`·`UserService.getPublicMember`), F-15 (`ReviewPlanService.addWish`, develop에 머지됨). 활동 기록은 F-00b의 `ActivityService` |
| 관련 이슈 | #57 (찜할 때 `addWish`로 복습 계획 자동 추가) → 구현 PR에 `Closes #57` |
| 승인 | 김준섭 2026-11-20 |

## 1. 목적

개념 카드를 읽은 학습자가 중요한 카드를 **♥ 찜**해 두고, 프로필에서 **찜한 카드 목록**으로 다시 찾아본다.
찜한 카드는 내일 복습 계획(F-15, `reason = WISH`)에 자동으로 들어가고, F-16 추천의 재료(`concept_wishes`)와 팔로워 피드(F-04)의 `WISH` 활동이 된다.
카드의 찜 수(`concepts.wish_count`)는 목록 인기순 정렬(F-09)에 쓰이므로 찜·해제 때 같은 트랜잭션에서 정확히 맞춘다.

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_찜하면_찜_수가_늘고_내일_복습_계획에_들어간다()`
> BE 테스트는 시드 카드에 기대지 않는다. 고유한 카테고리(예: `f12-test`)의 카드와 회원을 직접 넣고 그것만 확인한다. "내일"은 `Asia/Seoul` 기준 (F-15의 `Clock` 빈으로 고정).

### 백엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 로그인 회원 U가 `PUBLISHED` 카드 C(찜 2개)를 찜하지 않았고 C의 복습 계획이 없을 때 | `POST /api/concepts/{C}/wish`를 보내면 | 200과 `{wished:true, wishCount:3, reviewPlanDate:"<내일>"}`을 받는다. 같은 트랜잭션에서 `concept_wishes`에 `(U, C)` 행이 생기고, `concepts.wish_count`가 3이 되고, `activities`에 `(U, WISH, C)` 한 행이 생기고, `ReviewPlanService.addWish(U, C)`로 `review_plans`에 내일·`TODO`·`WISH` 한 행이 생긴다 |
| AC2 | U가 이미 C를 찜했을 때 | 다시 `POST`하면 | 200과 `{wished:true, wishCount:<그대로>, reviewPlanDate:null}`. 찜 행·찜 수·활동·복습 계획이 바뀌지 않는다 (멱등). 같은 회원의 `POST` 두 개가 동시에 들어와도 찜 행 1개·찜 수 +1이고 둘 다 200이다 (500이 아니다) |
| AC3 | U가 C의 TODO 복습 계획(예: `WRONG`)을 이미 가지고 있거나 내일 날짜 계획이 있을 때 | C를 찜하면 | 찜은 AC1처럼 성공하고 `reviewPlanDate`는 `null`이다. 복습 계획은 바뀌지 않는다 (`addWish`가 `Optional.empty()`) |
| AC4 | U가 C를 찜했을 때 (찜 3개) | `DELETE /api/concepts/{C}/wish`를 보내면 | 200과 `{wished:false, wishCount:2, reviewPlanDate:null}`. 찜 행이 지워지고 `wish_count`가 2가 된다. **복습 계획과 `WISH` 활동은 지우지 않는다** (F-15 결정, 활동은 F-04가 거른다). 찜하지 않은 카드에 `DELETE`해도 200과 `{wished:false, wishCount:<그대로>}`이고 찜 수는 음수가 되지 않는다 (멱등) |
| AC5 | U가 C를 찜했다가 해제했을 때 | 다시 찜하면 | 찜 행이 다시 생기고 찜 수가 1 늘지만 **`activities`에 새 행이 생기지 않는다** (같은 `(U, WISH, C)` 활동이 이미 있으면 기록하지 않음, 11-20 결정). `addWish`는 다시 부르며 AC3 규칙을 따른다 |
| AC6 | 회원 3명이 C를 찜하고 그중 1명이 해제했을 때 | `concepts.wish_count`와 `GET /api/concepts/{C}`의 `wishCount`를 보면 | 둘 다 `concept_wishes`의 실제 행 수(2)와 같다. 서로 다른 회원이 같은 카드를 동시에 찜해도 찜 수가 행 수와 같다 (카드 행 잠금) |
| AC7 | U가 C를 찜했고 D는 찜하지 않았을 때 | `GET /api/concepts/{id}`를 요청하면 | U의 토큰으로 C는 `wished:true`, D는 `wished:false`. 비로그인이나 깨진·만료 토큰이면 `wished:false`다 (F-10과 같이 공개 경로) |
| AC8 | 회원 U가 카드 15개를 찜했고 그중 1개가 `HIDDEN`, 1개가 `DRAFT`가 되었을 때 | 누구나(비로그인) `GET /api/users/{U}/wishes?page=1&size=12`를 요청하면 | 200과 페이지 응답 `{items, page:1, size:12, total:13}`. `items`는 `PUBLISHED` 카드만 **찜한 시각 최신순**(`concept_wishes.created_at DESC, concept_id DESC`)으로 12개, 항목은 `ConceptSummaryResponse`. `page=2`면 나머지 1개, 범위를 넘은 page면 `items: []`·`total: 13`. 찜이 없으면 `items: []`·`total: 0` |
| AC9 | 없는 회원 id(`999999`)나 탈퇴한 회원(`users.status='WITHDRAWN'`)일 때 | `GET /api/users/{id}/wishes`를 요청하면 | 404 `USER_404`다 (F-03 공개 프로필과 같다) |
| AC10 | 카드가 `DRAFT`·`HIDDEN`이거나 없는 id(`999999`)일 때 | `POST`·`DELETE .../wish`를 보내면 | 404 `CONCEPT_404`. 숨겨진 카드에 남아 있던 찜 행과 `wish_count`는 바뀌지 않는다 |
| AC11 | 토큰 없음·만료·형식 오류일 때 | `POST`·`DELETE .../wish`를 보내면 | 401 `AUTH_401`. `GET /api/users/{id}/wishes`와 카드 상세는 토큰이 깨져 있어도 200이다 (공개 경로) |

### 프론트엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC12 | 누구나 | SC-10을 열면 | 헤더의 찜 수 자리에 ♥ 찜 버튼(`WishButton`)이 "♥ N"으로 보인다. `wished`면 채운 하트·강조색, 아니면 빈 하트(♡)다. 버튼에 `aria-pressed`(찜 여부)와 `aria-label`("찜하기 N" / "찜 해제 N")이 있다. 비로그인이 누르면 요청 없이 `/login?redirect=/concepts/{id}`로 간다. 관련 카드를 눌러 `:id`가 바뀌면 새 카드의 `wished`·`wishCount`로 바뀐다 |
| AC13 | 로그인 회원이 찜하지 않은 카드에서 | 찜 버튼을 누르면 | 즉시 채운 하트·N+1로 바꾸고(낙관적 갱신) 요청 중에는 버튼을 끈다. 성공하면 응답 `wished`·`wishCount`로 맞추고, `reviewPlanDate`가 있으면 버튼 아래에 "{M월 D일} 복습 계획에 추가했습니다."와 "복습 계획 보기"(`/review-plans`) 링크를 보여 준다. 실패하면 원래 상태·숫자로 되돌리고 서버 `message`를 보여 준다 |
| AC14 | 로그인 회원이 찜한 카드에서 | 찜 버튼을 누르면 | 즉시 빈 하트·N−1로 바꾸고 `unwishConcept` 후 응답 값으로 맞춘다. 복습 안내 문구는 지운다. 실패하면 되돌리고 서버 `message`. 확인 대화상자는 없다 |
| AC15 | 누구나 | SC-18 프로필을 열면 | 학습 통계 아래에 "찜한 카드 N"(`total`) 절이 생기고 `ConceptCard` 12개가 격자로 보인다(누르면 `/concepts/{id}`). 남은 카드가 있으면 "더 보기"가 다음 12개를 아래에 붙인다(다 불러오면 사라짐). 0건이면 내 프로필은 "아직 찜한 카드가 없습니다."와 "개념 카드 둘러보기"(`/concepts`), 남의 프로필은 "아직 찜한 카드가 없습니다."만. 찜 목록 조회가 실패해도 프로필은 그대로이고 이 절에만 서버 `message`가 보인다. 프로필이 404면 절을 그리지 않는다. 다른 회원 프로필로 `:id`가 바뀌면 다시 불러온다. 375px에서 한 열이고 페이지 가로 스크롤이 없다 (NF-02) |

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다 (contract-checker, CI가 검사). Swagger Tag: `개념 카드`. 새 파일 `com.vibenote.concept.wish.ConceptWishController`에 아래 3개를 둔다 (클래스에 `@RequestMapping` 없이 메서드마다 전체 경로).

### `POST /api/concepts/{conceptId}/wish`

- 권한: 로그인 필요. 요청 본문 없음
- 응답 `200` (`ConceptWishResponse`, 새로 찜했든 이미 찜했든 200. 새 자원 id가 없는 멱등 토글이라 conventions 2절의 "생성 201" 대신 200)

```json
{ "wished": true, "wishCount": 13, "reviewPlanDate": "2026-12-04" }
```

- `ConceptWishResponse` (record, `com.vibenote.concept.wish.dto`): `wished`(boolean), `wishCount`(int, 요청 처리 후 `concepts.wish_count`), `reviewPlanDate`(`YYYY-MM-DD` \| null, 이번 요청으로 복습 계획이 새로 생겼을 때만 그 날짜)

### `DELETE /api/concepts/{conceptId}/wish`

- 권한: 로그인 필요
- 응답 `200` (`ConceptWishResponse`): 찜하지 않은 카드여도 200 (멱등). conventions 2절의 "삭제 204" 대신 바뀐 찜 수를 돌려준다 (F-11 리뷰 삭제와 같은 이유)

```json
{ "wished": false, "wishCount": 12, "reviewPlanDate": null }
```

### `GET /api/users/{userId}/wishes?page=1&size=12`

- 권한: 공개. `SecurityConfig.PUBLIC_GET_PATHS`의 CONCEPT 블록에 `"/api/users/{id:\\d+}/wishes"`를 추가한다 (공통 파일 → PR에서 @whans1996 멘션)
- 페이지 파라미터는 공통 `PageQuery`(page 1부터, size 기본 20·최대 100). 화면은 `size=12`를 보낸다
- 응답 `200` (`PageResponse<ConceptSummaryResponse>`)

```json
{
  "items": [
    {
      "id": 57,
      "name": "JWT 기반 인증",
      "category": "보안",
      "difficulty": "JUNIOR",
      "summary": "서버가 서명한 토큰으로 로그인 상태를 증명하는 방식",
      "avgRating": 4.3,
      "wishCount": 13
    }
  ],
  "page": 1,
  "size": 12,
  "total": 13
}
```

### `GET /api/concepts/{id}` (F-10, 필드 추가)

- `ConceptDetailResponse`에 `wished`(boolean, 필수) 추가. 로그인 회원이 찜했으면 `true`, 그 밖(비로그인·깨진 토큰 포함)은 `false`
- FE 테스트 fixture(`ConceptDetailView.spec.js` 등)에 `wished`를 더한다

### 에러 (위 엔드포인트 공통)

| 상태 | 코드 | 언제 |
|---|---|---|
| 400 | COMMON_400 | `page`·`size` 값 오류, 숫자가 아닌 id |
| 401 | AUTH_401 | 찜·해제에 토큰 없음·만료 (AC11) |
| 404 | CONCEPT_404 | 찜·해제할 카드가 없거나 `DRAFT`·`HIDDEN` (AC10) |
| 404 | USER_404 | 찜 목록의 회원이 없거나 탈퇴 (AC9) |

- 새 에러 코드는 없다

## 4. DB 변경

- Flyway: 없음 (`concept_wishes` PK `(user_id, concept_id)`, `concepts.wish_count`는 V1, `activities.type` `WISH`는 V2에 있다)
- 사용 테이블: `concept_wishes`(읽기·쓰기), `concepts`(`wish_count` 갱신·잠금), `activities`(쓰기, 존재 확인), `review_plans`(F-15 `addWish`를 통해서만), `users`(찜 목록 회원 확인)
- 매퍼: 새 `ConceptWishMapper`(`mapper/concept/ConceptWishMapper.xml`)
  - `exists(userId, conceptId)`, `insert(userId, conceptId)`, `delete(userId, conceptId)`
  - `lockConcept(conceptId)`: `SELECT id FROM concepts WHERE id = ? FOR UPDATE`
  - `refreshWishCount(conceptId)`: `UPDATE concepts SET wish_count = (SELECT COUNT(*) FROM concept_wishes WHERE concept_id = ?) WHERE id = ?`, `findWishCount(conceptId)`
  - `findPublishedPageByUser(userId, size, offset)`: `concept_wishes w JOIN concepts c ON c.id = w.concept_id WHERE w.user_id = ? AND c.status = 'PUBLISHED' ORDER BY w.created_at DESC, w.concept_id DESC LIMIT ? OFFSET ?`, `countPublishedByUser(userId)`. PK의 선두 `user_id`로 회원의 찜만 읽는다
- 쓰기 순서 (`ConceptWishService`, `@Transactional`):
  - 찜: `ConceptQueryService.getPublished`(→ 404) → `lockConcept`(같은 카드의 동시 찜을 직렬화해 찜 수가 행 수와 같게) → `exists`면 찜 수만 읽어 `{true, count, null}` → 아니면 `insert`(`DuplicateKeyException`도 "이미 찜함"으로 처리) → `refreshWishCount` → `ActivityService.recordOnce(U, WISH, C)` → `ReviewPlanService.addWish(U, C)` → 응답
  - 해제: `getPublished` → `lockConcept` → `delete` → `refreshWishCount` → 응답 (복습 계획·활동은 그대로)
- 활동: `ActivityService`에 `recordOnce(userId, type, targetId)` 추가 (같은 `(user_id, type, target_id)` 행이 있으면 넣지 않는다, `ActivityMapper.exists` 추가). `WISH`의 `target_id`는 **카드 id**다 → **F-04 피드는 `concept_wishes`(아직 찜 중)·`concepts.status = 'PUBLISHED'`와 JOIN해 해제된 찜과 숨겨진 카드의 활동을 걸러야 한다** (F-04 spec에 옮긴다). `feed` 패키지는 공통이므로 PR에서 멘션
- 카드 상세 `wished`: `ConceptService.getDetail(id, userId)`에서 `userId`가 있으면 `ConceptWishMapper.exists`로 채운다
- `wish_count`는 탈퇴 회원의 찜도 센다 (행이 남아 있으므로 `concept_wishes` 행 수와 같게 유지). 시드 `R__seed_concepts.sql`의 upsert는 `wish_count`를 덮어쓰지 않는다 (F-11에서 확인)

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | 기존 `/concepts/:id`(SC-10)와 `/users/:id`(SC-18). 라우트 변경 없음 |
| 컴포넌트 | `components/concept/WishButton.vue`(새 파일: `conceptId`·`wished`·`wishCount` prop, 낙관적 토글, 요청 중 비활성, 비로그인 로그인 이동, 복습 안내 문구·에러 표시, 결과를 `change`({wished, wishCount}) 이벤트로 올림), `components/concept/UserWishes.vue`(새 파일: `userId`·`isMe` prop, "찜한 카드 N" 절, `ConceptCard` 격자 + 더 보기 + 빈 상태 + 절 안 에러) |
| 연결 | `ConceptDetailView`: 헤더의 `<span class="wish">`를 `<WishButton>`으로 바꾸고 `change`로 `concept.wished`·`wishCount`를 갱신. `UserProfileView`: 통계 타일 아래에 `<UserWishes :user-id :is-me>` 한 줄 (프로필을 불러온 뒤에만) |
| store | 없음. 로그인 여부·내 id는 `useAuthStore` |
| API 함수 | `src/api/concepts.js`에 `wishConcept(conceptId)`(POST), `unwishConcept(conceptId)`(DELETE), `fetchUserWishes(userId, { page, size })` 추가 |
| 상태별 UI | 찜 목록 로딩 "찜한 카드를 불러오는 중…" / 0건 안내(AC15) / 실패 시 절 안 서버 `message` / 찜 실패 시 버튼 아래 서버 `message` |
| 반응형 | 찜 목록 격자는 데스크톱 `auto-fill minmax(240px, 1fr)`, 375px 한 열. 찜 버튼 터치 영역 최소 32px. 모바일 헤더에서 찜·평점은 이름 아래 줄 (SC-10 설계 그대로) |

## 6. LLM (AI 기능만)

해당 없음.

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1~AC5 | `concept/wish/ConceptWishApiTest#AC1_...`~`#AC5_...` (응답 본문, `concept_wishes`·`wish_count`·`activities`·`review_plans` DB 값, 이미 찜·TODO 있음·해제 후 재찜 시 활동 수 불변, 찜하지 않은 카드 DELETE) | MockMvc + Testcontainers 통합 |
| AC2 동시성 | `ConceptWishServiceTest#AC2_DuplicateKey도_이미_찜함` (매퍼가 `DuplicateKeyException`을 던지도록 mock) | 단위 |
| AC6 | `ConceptWishApiTest#AC6_...` (3명 찜·1명 해제 후 DB 값과 상세 `wishCount`) + 서로 다른 회원 동시 찜(`ExecutorService` 2스레드) 후 `wish_count` = 행 수 | 통합 |
| AC7 | `concept/ConceptDetailApiTest`(기존)에 `#AC7_찜_여부` 추가 (찜함·안 함·비로그인·깨진 토큰) | MockMvc 통합 |
| AC8, AC9 | `ConceptWishApiTest#AC8_...`, `#AC9_...` (15개 중 HIDDEN·DRAFT 제외, 찜 시각 순서·같은 시각 tie-break, 2쪽·범위 밖·0건, 없는·탈퇴 회원) | MockMvc 통합 |
| AC10, AC11 | `ConceptWishApiTest#AC10_...`, `#AC11_...` (DRAFT·HIDDEN·없는 id × POST·DELETE, 숨김 카드 찜 행 불변, 토큰 없음·깨짐 × 2 + 공개 경로 200) | MockMvc 통합 |
| 활동 | `feed/ActivityServiceTest#recordOnce_...` (같은 활동이 있으면 넣지 않음) | 통합 |
| AC12~AC14 | `components/concept/__tests__/WishButton.spec.js` (`vi.mock('@/api/concepts')`, 표시·aria, 비로그인 이동, 낙관적 갱신·요청 중 비활성, 성공 시 응답 값·복습 문구·링크, `reviewPlanDate` null이면 문구 없음, 실패 되돌림·message, `change` 이벤트) | Vitest |
| AC12 연결 | `views/__tests__/ConceptDetailView.spec.js`에 추가 (헤더에 `WishButton`, `:id` 변경 시 새 카드 값) | Vitest |
| AC15 | `components/concept/__tests__/UserWishes.spec.js` (목록·더 보기·0건 내/남 문구·절 안 에러·`userId` 변경 재조회) + `views/__tests__/UserProfileView.spec.js`에 절 표시·404면 미표시 추가 + ui-verifier 375px·1280px | Vitest + ui-verifier |
| API 함수 | `api/__tests__/concepts.spec.js`에 3개 함수 추가 (경로·메서드·파라미터) | Vitest |

## 8. 범위 밖

- 카드 목록(SC-09)·분석 개념 탭·복습 목록의 `ConceptCard`에서 찜 토글 (11-20 결정: 상세에서만. 목록 카드는 지금처럼 찜 수만 보여 준다)
- 찜 목록 비공개 설정, 찜 목록 정렬·필터 변경 (찜한 시각 최신순 고정)
- 찜 해제 시 복습 계획 삭제 (F-15 결정: 계획 유지)
- 피드 조회와 해제된 찜 활동 걸러내기 (F-04)
- 탈퇴 시 찜 행 정리 (F-03 범위. 지금은 남기고 찜 수에 포함)
- 추천 점수에 찜 반영 (F-16, A)

## 9. 결정 필요

- (해결 11-20) 찜 목록은 모두에게 공개(`GET /api/users/{id}/wishes`), 찜 토글은 상세(SC-10)에서만, `WISH` 활동은 카드당 처음 한 번만 (결정로그)
- (구현 착수 전) `PUBLIC_GET_PATHS`에 `/api/users/{id:\\d+}/wishes`를 넣어도 `/api/users/me/...` 경로가 공개로 열리지 않는지 AC11 테스트로 확인한다

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [ ] 브랜치 `feature/F-12-concept-wish`
- [ ] Flyway 마이그레이션 (없음)
- [ ] AC별 실패 테스트 작성 (red 확인)
- [ ] BE 구현 (green): `ConceptWishController`, `ConceptWishService`, `ConceptWishMapper`(+XML), `ConceptWishResponse`, `ConceptDetailResponse.wished`, `ActivityService.recordOnce`·`ActivityMapper.exists`, `PUBLIC_GET_PATHS`
- [ ] FE 구현 (green): API 함수 3개, `WishButton`, `UserWishes`, `ConceptDetailView`·`UserProfileView` 연결
- [ ] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [ ] contract-checker 통과
- [ ] ui-verifier 통과 (데스크톱·모바일)
- [ ] reviewer 지적 사항 반영
- [ ] 산출물 반영: `06-화면설계서.md` SC-10 찜 버튼·SC-18 찜 절 + 스크린샷, `02-유스케이스.md` UC23("상세에서 ♥", 프로필 찜 목록, 재찜 활동), `01-요구사항정의서.md` F-12 행 확인
- [ ] PR CI 초록 (`ConceptController`·`ConceptService`·`SecurityConfig`·`feed` 패키지·`src/api/concepts.js`가 공통 → 본문에서 @whans1996 멘션, `node scripts/owners.mjs --diff origin/develop`). PR 본문 `Closes #57`
- [ ] 상태 DONE (PR 머지 전 마지막 커밋)
