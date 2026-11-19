# F-11 카드 리뷰

| 항목 | 값 |
|---|---|
| 상태 | DRAFT <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-11 |
| 우선순위 | P0 |
| 영역 오너 | B 김준섭 (카드·소셜) |
| 관련 필수 요구사항 | ⑩ 리뷰 작성/조회 |
| 의존 spec | F-02 (로그인), F-10 (카드 상세 화면 `ConceptDetailView`·`ConceptController`·`src/api/concepts.js`, 모두 develop에 머지됨). 활동 기록은 F-00b의 `ActivityService.record` |
| 관련 이슈 | 없음 |
| 승인 | (승인한 사람, 날짜) |

## 1. 목적

카드를 읽은 학습자가 "이해에 도움이 됐나요?"에 **별점(1~5)과 한 줄 리뷰**를 남기고, 다른 학습자는 리뷰 목록과 평균 별점으로 어떤 카드가 이해에 도움이 되는지 판단한다.
카드당 한 사람이 리뷰 하나만 쓸 수 있으므로(DB UNIQUE), 생각이 바뀌면 **내 리뷰를 수정하거나 삭제**한다 (11-19 결정, 기획서의 "작성·조회"에 수정·삭제를 더함).
리뷰를 쓰면 팔로워 피드(F-04)에 `REVIEW` 활동으로 남는다.

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_공개_카드의_리뷰를_비로그인으로_최신순_조회한다()`
> BE 테스트는 시드 카드에 기대지 않는다. 고유한 카테고리(예: `f11-test`)의 카드와 회원을 직접 넣고 그것만 확인한다.

### 백엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | `PUBLISHED` 카드 C에 리뷰 12개가 있을 때 | 누구나(비로그인) `GET /api/concepts/{C}/reviews?page=1&size=10`을 요청하면 | 200과 페이지 응답 `{items, page:1, size:10, total:12}`를 받는다. `items`는 `created_at DESC, id DESC` 순서의 10개이고 항목은 3절 `ConceptReviewResponse`다. `page=2`면 나머지 2개, 범위를 넘은 page면 `items: []`·`total: 12`다. 리뷰가 없으면 `items: []`·`total: 0` |
| AC2 | 리뷰 작성자 중 탈퇴한 회원(`users.status = 'WITHDRAWN'`)이 있을 때 | 목록을 요청하면 | 그 리뷰도 목록과 `total`에 포함되고, `authorId`는 `null`, `authorNickname`은 `"탈퇴한 사용자"`다. 활성 회원 리뷰는 `authorId`와 현재 닉네임을 담는다 |
| AC3 | 로그인 회원 U가 C에 리뷰를 쓰지 않았을 때 | `POST /api/concepts/{C}/reviews` `{rating:4, content:"  예제가 명확했어요  "}`를 보내면 | 201과 3절 `ConceptReviewSaveResponse`를 받는다. `review.content`는 앞뒤 공백을 뺀 `"예제가 명확했어요"`다. 같은 트랜잭션에서 `concepts.avg_rating`이 C의 전체 리뷰 평균(소수 1자리 반올림)으로 바뀌고, 응답의 `avgRating`·`reviewCount`가 그 값과 같다. `activities`에 `(U, REVIEW, 리뷰 id)` 한 행이 생긴다 |
| AC4 | 평점 계산 경계값 | 리뷰 4·4·5를 쓰면 / 그중 하나를 지워 4·5가 되면 / 모두 지우면 | `avg_rating`이 각각 `4.3` / `4.5` / `0.0`이 되고 `reviewCount`는 3 / 2 / 0이다 |
| AC5 | 로그인 회원이 | `rating`이 없음·0·6이거나, `content`가 없음·공백만·201자(앞뒤 공백 제외)이면 | 400 `COMMON_400`과 `details`에 해당 필드(`rating`·`content`)와 사유를 받는다. 200자 정확히는 성공한다. 리뷰·평점·활동은 바뀌지 않는다 |
| AC6 | U가 이미 C에 리뷰를 썼을 때 | U가 다시 `POST`하면 | 409 `CONCEPT_409` "이미 이 카드에 리뷰를 남겼습니다."를 받고 기존 리뷰는 그대로다. 동시에 두 요청이 들어와 UNIQUE 제약에 걸려도 409다 (500이 아니다) |
| AC7 | U가 C에 리뷰를 썼을 때 | `GET /api/concepts/{C}/reviews/me`를 요청하면 | 200과 U의 `ConceptReviewResponse`를 받는다. 리뷰가 없으면 204(본문 없음)다 |
| AC8 | U가 C에 리뷰(rating 2)를 썼을 때 | `PUT /api/concepts/{C}/reviews/me` `{rating:5, content:"다시 읽으니 이해됨"}`을 보내면 | 200과 `ConceptReviewSaveResponse`(바뀐 리뷰, 다시 계산한 `avgRating`·`reviewCount`)를 받는다. 리뷰 `id`·`createdAt`은 그대로라 목록 순서가 바뀌지 않는다. 활동은 새로 생기지 않는다. 검증 규칙은 AC5와 같다 |
| AC9 | U가 C에 리뷰를 썼을 때 | `DELETE /api/concepts/{C}/reviews/me`를 보내면 | 200과 `{avgRating, reviewCount}`(`ConceptRatingResponse`)를 받고, 리뷰가 지워지고 평점이 다시 계산된다. 그 뒤 같은 회원이 새 리뷰를 쓸 수 있다 (201) |
| AC10 | U가 C에 리뷰를 쓰지 않았을 때 | `PUT`·`DELETE .../reviews/me`를 보내면 | 404 `CONCEPT_001` "작성한 리뷰가 없습니다."를 받는다. 다른 회원 V의 리뷰는 U의 요청으로 바뀌거나 지워지지 않는다 (`/me`는 토큰의 회원만 가리킨다) |
| AC11 | 카드가 `DRAFT`·`HIDDEN`이거나 없는 id(`999999`)일 때 | 목록·`/me`·작성·수정·삭제를 요청하면 | 모두 404 `CONCEPT_404`다. 숨겨진 카드에 남아 있던 리뷰도 조회·변경되지 않는다 |
| AC12 | 토큰 없음·만료·형식 오류일 때 | `/me` 조회, 작성, 수정, 삭제를 요청하면 | 401 `AUTH_401`이다. 목록 조회는 토큰이 깨져 있어도 200이다 (공개 경로) |

### 프론트엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC13 | 누구나 | SC-10을 열면 | 헤더 평점 옆에 "리뷰 N"(목록 응답 `total`)이 보이고, 아래 "리뷰" 절에 최신순 10개가 별점(★ 1~5), 내용, 닉네임, 작성일(`YYYY-MM-DD`)로 보인다. 닉네임은 `/users/{authorId}`(SC-18) 링크이고, `authorId`가 `null`이면 링크 없는 "탈퇴한 사용자"다. 남은 리뷰가 있으면 "더 보기"가 보이고 누르면 다음 10개를 아래에 붙인다(다 불러오면 버튼이 사라진다). 0건이면 "아직 리뷰가 없습니다. 첫 리뷰를 남겨 주세요". 리뷰 내용은 글자 그대로 보인다(HTML이 태그로 해석되지 않는다) |
| AC14 | 비로그인 방문자가 | "리뷰" 절을 보면 | 작성 폼 대신 "로그인하고 리뷰 남기기" 버튼이 보이고, 누르면 `/login?redirect=/concepts/{id}`로 간다 (로그인 후 이 카드로 돌아온다) |
| AC15 | 리뷰를 쓰지 않은 로그인 회원이 | 별 1~5 중 하나를 고르고 내용을 입력해 "등록"을 누르면 | `createReview`가 호출되고, 성공하면 새 리뷰가 목록 맨 위에 붙고 "리뷰 N"이 1 늘고 헤더 평점이 응답 `avgRating`으로 바뀌며 폼 자리가 "내 리뷰" 상자로 바뀐다. 별을 고르지 않았거나 내용이 공백뿐이면 "등록"이 비활성이다. 입력창은 200자에서 더 입력되지 않고 "n/200"을 보여 준다. 400이면 폼 아래 `details`의 필드 메시지를, 409이면 서버 `message`를 보여 주고 내 리뷰를 다시 불러온다. 그 밖의 실패는 서버 `message` |
| AC16 | 리뷰를 쓴 로그인 회원이 | 화면을 열면 | `fetchMyReview` 결과로 폼 대신 "내 리뷰" 상자(별점, 내용, "수정"·"삭제")가 보인다. "수정"을 누르면 값이 채워진 폼이 되고 "저장"하면 `updateReview` 후 목록의 같은 항목(불러와 있으면)과 헤더 평점이 바뀐다. "취소"는 원래 상자로 돌아간다 |
| AC17 | 리뷰를 쓴 로그인 회원이 | "삭제"를 누르고 확인 대화상자에서 확인하면 | `deleteReview` 후 목록에서 그 항목이 빠지고 "리뷰 N"이 1 줄고 헤더 평점이 응답 값으로 바뀌며 빈 작성 폼이 다시 보인다. 대화상자에서 취소하면 아무 요청도 보내지 않는다 |
| AC18 | 상세 화면에서 | 관련 카드를 눌러 `:id`만 바뀌면 | 리뷰 목록·내 리뷰·폼 상태를 초기화하고 새 카드 기준으로 다시 불러온다. 리뷰 목록 조회가 실패해도 카드 본문은 그대로 보이고 리뷰 절에만 서버 `message`가 보인다. 375px 폭에서 가로 스크롤이 없다 (NF-02) |

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다 (contract-checker, CI가 검사). Swagger Tag: `개념 카드`. 새 파일 `com.vibenote.concept.review.ConceptReviewController`(`@RequestMapping("/api/concepts/{conceptId}/reviews")`)에 둔다.

### `GET /api/concepts/{conceptId}/reviews?page=1&size=10`

- 권한: 공개. `SecurityConfig.PUBLIC_GET_PATHS`의 CONCEPT 블록에 `"/api/concepts/{id:\\d+}/reviews"`를 추가한다 (공통 파일 → PR에서 @whans1996 멘션). `/reviews/me`는 이 패턴에 걸리지 않으므로 계속 인증이 필요하다
- 페이지 파라미터는 공통 `PageQuery`(page 1부터, size 기본 20·최대 100). 화면은 `size=10`을 보낸다
- 응답 `200`

```json
{
  "items": [
    {
      "id": 301,
      "rating": 5,
      "content": "토큰 서명과 만료 검증 순서가 한눈에 들어왔어요",
      "authorId": 7,
      "authorNickname": "코딩하는감자",
      "createdAt": "2026-12-02T21:14:03+09:00"
    },
    {
      "id": 288,
      "rating": 3,
      "content": "예제는 좋은데 refresh 토큰 설명이 더 있으면 좋겠어요",
      "authorId": null,
      "authorNickname": "탈퇴한 사용자",
      "createdAt": "2026-12-01T09:40:51+09:00"
    }
  ],
  "page": 1,
  "size": 10,
  "total": 12
}
```

- `ConceptReviewResponse` (record, `com.vibenote.concept.review.dto`): `id`(long), `rating`(int 1~5), `content`(string), `authorId`(long \| null, 탈퇴면 null), `authorNickname`(string, 탈퇴면 `"탈퇴한 사용자"`), `createdAt`(ISO-8601 + 오프셋). 수정 시각은 넣지 않는다

### `GET /api/concepts/{conceptId}/reviews/me`

- 권한: 로그인 필요
- 응답 `200`: `ConceptReviewResponse` (위와 같은 형식) / `204`: 내 리뷰 없음 (본문 없음)

### `POST /api/concepts/{conceptId}/reviews`

- 권한: 로그인 필요
- 요청 (`ConceptReviewRequest`, 작성·수정 공용)

```json
{ "rating": 4, "content": "예제가 명확했어요" }
```

| 필드 | 규칙 |
|---|---|
| rating | 필수, 정수 1~5 (`@NotNull @Min(1) @Max(5) Integer`. 소수(`3.5`)는 Jackson 설정(`ACCEPT_FLOAT_AS_INT`)에 따라 정수로 바뀌어 받아질 수 있어 계약에 넣지 않는다. 전역 설정은 바꾸지 않는다) |
| content | 필수, 앞뒤 공백 제거 후 1~200자 (`@NotBlank` + 제거 후 길이 검사. 저장도 제거한 값) |

- 응답 `201` (`ConceptReviewSaveResponse`)

```json
{
  "review": {
    "id": 302,
    "rating": 4,
    "content": "예제가 명확했어요",
    "authorId": 9,
    "authorNickname": "준섭",
    "createdAt": "2026-12-03T10:05:00+09:00"
  },
  "avgRating": 4.3,
  "reviewCount": 13
}
```

### `PUT /api/concepts/{conceptId}/reviews/me`

- 권한: 로그인 필요. 요청은 `ConceptReviewRequest` (같은 규칙)
- 응답 `200`: `ConceptReviewSaveResponse` (위와 같은 형식)

### `DELETE /api/concepts/{conceptId}/reviews/me`

- 권한: 로그인 필요
- 응답 `200` (`ConceptRatingResponse`)

```json
{ "avgRating": 4.2, "reviewCount": 12 }
```

- conventions 2절의 "삭제 204" 대신 200으로 바뀐 평점을 돌려준다. 평균은 소수 1자리로 반올림되어 있어 FE가 지운 별점으로 다시 계산할 수 없고, 헤더 갱신을 위해 카드 상세를 다시 부르는 것보다 가볍다

### 에러 (위 엔드포인트 공통)

| 상태 | 코드 | 언제 |
|---|---|---|
| 400 | COMMON_400 | `rating`·`content` 검증 실패 (`details`에 필드별 사유, AC5) |
| 401 | AUTH_401 | `/me`·작성·수정·삭제에 토큰 없음·만료 (AC12) |
| 404 | CONCEPT_404 | 없는 카드, `DRAFT`·`HIDDEN` 카드 (AC11) |
| 404 | **CONCEPT_001** (새 코드) | 수정·삭제할 내 리뷰가 없음 "작성한 리뷰가 없습니다." (AC10) |
| 409 | **CONCEPT_409** (새 코드) | 같은 카드에 두 번째 리뷰 "이미 이 카드에 리뷰를 남겼습니다." (AC6) |

- 새 코드는 `ErrorCode`의 `// ── CONCEPT` 블록 안에 추가하고 `docs/conventions.md` 3절 CONCEPT 행에도 적는다

## 4. DB 변경

- Flyway: 없음 (`concept_reviews`는 V1, UNIQUE `(concept_id, user_id)`·user FK RESTRICT는 V2에 있다)
- 사용 테이블: `concept_reviews`(읽기·쓰기), `concepts`(`avg_rating` 갱신·잠금), `users`(작성자 닉네임·상태), `activities`(쓰기)
- 매퍼: 새 `ConceptReviewMapper`(`mapper/concept/ConceptReviewMapper.xml`)
  - `findPage(conceptId, size, offset)`: `concept_reviews r JOIN users u ON u.id = r.user_id WHERE r.concept_id = ? ORDER BY r.created_at DESC, r.id DESC LIMIT ? OFFSET ?`, `countByConcept(conceptId)`. 기존 인덱스 `idx_concept_reviews_concept (concept_id, created_at)` 사용
  - `findByConceptAndUser`, `insert`(`useGeneratedKeys`), `update`, `delete`
  - `lockConcept(conceptId)`: `SELECT id FROM concepts WHERE id = ? FOR UPDATE`
  - `refreshAvgRating(conceptId)`: `UPDATE concepts SET avg_rating = COALESCE((SELECT ROUND(AVG(rating), 1) FROM concept_reviews WHERE concept_id = ?), 0.0) WHERE id = ?`
- 쓰기 순서 (`ConceptReviewService`, `@Transactional`): `ConceptQueryService.getPublished`(→ 404) → `lockConcept`(같은 카드의 동시 쓰기를 직렬화해 평균이 다른 트랜잭션의 쓰기를 놓치지 않게 함) → insert/update/delete → `refreshAvgRating` → 평균·개수를 다시 읽어 응답. 작성은 `DuplicateKeyException`도 `CONCEPT_409`로 바꾼다
- 활동: 작성 때만 `ActivityService.record(userId, ActivityType.REVIEW, reviewId)`. 수정 때는 기록하지 않는다. 리뷰를 지워도 활동 행은 지우지 않는다 → **F-04 피드는 `concept_reviews`와 JOIN해 지워진 리뷰의 활동을 걸러야 한다** (F-04 spec에 옮긴다). `target_id`가 리뷰 id라 피드가 카드 이름·별점을 함께 보여 줄 수 있다
- 시드 `R__seed_concepts.sql`의 upsert는 `avg_rating`·`wish_count`를 덮어쓰지 않으므로 다시 적용돼도 평점이 유지된다

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | 기존 `/concepts/:id` → `views/concept/ConceptDetailView.vue`의 관련 카드 절 아래에 "리뷰" 절을 붙인다. 라우트 변경 없음 |
| 컴포넌트 | `components/concept/ConceptReviews.vue`(절 전체: 내 리뷰 상자/폼/로그인 버튼 + 목록 + 더 보기, `conceptId` prop, 평점 변경을 `rating-change`({avgRating, reviewCount}) 이벤트로 올림), `components/concept/ReviewForm.vue`(별점 선택 + 입력 + 글자 수 + 등록/저장/취소), `components/concept/StarRating.vue`(읽기 전용 표시와 선택 입력 겸용, 별 버튼에 `aria-label="별점 n점"`) — 새 파일 |
| store | 없음. 로그인 여부는 `useAuthStore` |
| API 함수 | `src/api/concepts.js`에 `fetchReviews(conceptId, { page, size })`, `fetchMyReview(conceptId)`(204면 `null`), `createReview(conceptId, body)`, `updateReview(conceptId, body)`, `deleteReview(conceptId)` 추가 |
| 헤더 | `ConceptDetailView`의 ★ 평점 옆에 "· 리뷰 N". N과 평점은 리뷰 컴포넌트의 `rating-change`로 갱신한다 (카드 상세를 다시 부르지 않는다) |
| 상태별 UI | 리뷰 목록 로딩 "리뷰를 불러오는 중…" / 0건 안내 / 목록 실패 시 리뷰 절 안에만 서버 `message` / 폼 검증 실패는 필드 아래 (AC15) |
| 반응형 | 리뷰 항목은 한 열, 긴 내용은 줄바꿈(`overflow-wrap: anywhere`). 375px에서 별 버튼 터치 영역 최소 32px |

## 6. LLM (AI 기능만)

해당 없음.

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1, AC2 | `concept/review/ConceptReviewApiTest#AC1_...`, `#AC2_...` (12개 페이지 2쪽·범위 밖·0건, 같은 `created_at` tie-break, 탈퇴 작성자) | MockMvc + Testcontainers 통합 |
| AC3, AC4 | `ConceptReviewApiTest#AC3_...`, `#AC4_...` (공백 제거 저장, `concepts.avg_rating` DB 값, `activities` 행, 4·4·5 → 4.3 → 4.5 → 0.0) | MockMvc 통합 |
| AC5 | `ConceptReviewApiTest#AC5_...` (`@ParameterizedTest`: rating 없음·0·6, content 없음·공백·201자, 200자 성공, 실패 시 DB 불변) | MockMvc 통합 |
| AC6 | `ConceptReviewApiTest#AC6_...` (두 번째 POST 409) + `ConceptReviewServiceTest#AC6_UNIQUE_위반도_409` (매퍼가 `DuplicateKeyException`을 던지도록 mock) | 통합 + 단위 |
| AC7~AC10 | `ConceptReviewApiTest#AC7_...`~`#AC10_...` (`/me` 200·204, 수정 후 id·createdAt 유지·활동 수 불변, 삭제 후 재작성, 리뷰 없을 때 404, 다른 회원 리뷰 불변) | MockMvc 통합 |
| AC11, AC12 | `ConceptReviewApiTest#AC11_...`, `#AC12_...` (DRAFT·HIDDEN·없는 id × 5개 엔드포인트, 토큰 없음·깨짐 × 4개 + 목록 200) | MockMvc 통합 |
| AC13~AC17 | `components/concept/__tests__/ConceptReviews.spec.js` (`vi.mock('@/api/concepts')`, 목록·더 보기·0건·탈퇴 작성자, HTML 텍스트 처리, 비로그인 버튼 이동, 등록·수정·삭제 성공 시 목록·`rating-change`, 400 details·409, confirm 취소) | Vitest |
| AC15 폼 | `components/concept/__tests__/ReviewForm.spec.js` (별 미선택·공백 비활성, 200자 제한·글자 수) | Vitest |
| AC18 | `views/__tests__/ConceptDetailView.spec.js`에 추가 (`:id` 변경 시 리뷰 재조회, 리뷰 실패 시 본문 유지, 헤더 "리뷰 N" 갱신) + ui-verifier 375px·1280px | Vitest + ui-verifier |
| API 함수 | `api/__tests__/concepts.spec.js`에 5개 함수 추가 (204 → `null`) | Vitest |

## 8. 범위 밖

- 리뷰 신고·숨김, 관리자의 리뷰 삭제 (F-13에서 필요하면 결정)
- 리뷰 좋아요·답글, 별점별 필터·정렬 변경 (최신순 고정)
- 리뷰 수를 `concepts`에 저장하는 반정규화 컬럼 (매번 `COUNT`, 인덱스로 충분)
- 피드 조회와 지워진 리뷰 활동 걸러내기 (F-04)
- 프로필(SC-18)의 "내가 쓴 리뷰" 목록
- 찜 (F-12)

## 9. 결정 필요

- (해결 11-19) 범위: 작성·조회 + 내 리뷰 수정·삭제 (`/reviews/me`), 내용 필수 1~200자, 목록은 "더 보기" 10개씩 최신순 (결정로그)
- (구현 착수 전) `PUBLIC_GET_PATHS` 패턴 `/api/concepts/{id:\\d+}/reviews`가 `/reviews/me`를 공개로 열지 않는지 AC12 테스트로 확인한다

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [ ] 브랜치 `feature/F-11-concept-review`
- [ ] Flyway 마이그레이션 (없음)
- [ ] AC별 실패 테스트 작성 (red 확인)
- [ ] BE 구현 (green): `ConceptReviewController`, `ConceptReviewService`, `ConceptReviewMapper`(+XML), DTO 4종(`ConceptReviewRequest`·`ConceptReviewResponse`·`ConceptReviewSaveResponse`·`ConceptRatingResponse`), `ErrorCode` `CONCEPT_001`·`CONCEPT_409`, `PUBLIC_GET_PATHS`
- [ ] FE 구현 (green): API 함수 5개, `StarRating`, `ReviewForm`, `ConceptReviews`, `ConceptDetailView` 헤더·절 연결
- [ ] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [ ] contract-checker 통과
- [ ] ui-verifier 통과 (데스크톱·모바일)
- [ ] reviewer 지적 사항 반영
- [ ] 산출물 반영: `06-화면설계서.md` SC-10 리뷰 부분(내 리뷰 상자·수정·삭제·더 보기) + 스크린샷, `01-요구사항정의서.md` F-11 행·`02-유스케이스.md` UC22에 수정·삭제, `docs/conventions.md` 3절 CONCEPT 코드
- [ ] PR CI 초록 (CODEOWNERS상 concept 패키지·`components/concept`·`src/api/concepts.js`와 `SecurityConfig`·`ErrorCode`가 공통 → 본문에서 @whans1996 멘션, `node scripts/owners.mjs --diff origin/develop`)
- [ ] 상태 DONE (PR 머지 전 마지막 커밋)
