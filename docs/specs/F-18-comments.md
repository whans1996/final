# F-18 댓글

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-18 |
| 우선순위 | P0 |
| 영역 오너 | B 김준섭 (카드·소셜) |
| 관련 필수 요구사항 | ④ 커뮤니티 댓글 (명세서 "추가 · 커뮤니티: 댓글 CRUD") |
| 의존 spec | F-17 (게시글·SC-15 상세 화면, develop에 머지됨), F-02 (로그인). 탈퇴 회원 차단은 `UserService.getActiveMember`(F-11·F-17과 같음), 탈퇴 작성자 표시는 F-17과 같은 방식 |
| 관련 이슈 | 없음 |
| 승인 | 김준섭 2026-11-20 |

## 1. 목적

커뮤니티 글(특히 코드 이해 Q&A)에 다른 학습자가 **댓글로 답한다**. 누구나 댓글을 읽고, 회원은 댓글을 쓰며 자기 댓글을 수정·삭제한다 (UC41).
댓글은 대화 순서대로(오래된 것부터) 게시글 상세(SC-15) 아래에 한 번에 보인다. 피드 활동은 남기지 않는다.

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_비로그인으로_댓글을_오래된순_전부_조회한다()`
> BE 테스트는 다른 테스트가 넣은 글·댓글에 기대지 않는다. 테스트마다 회원·글을 직접 넣고 그 글 id로 확인한다.

### 백엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 글 P에 댓글 3개(C1·C2·C3 순서로 작성)가 있을 때 | 누구나(비로그인) `GET /api/posts/{P}/comments`를 요청하면 | 200과 `{items, total: 3}`을 받는다. `items`는 `created_at ASC, id ASC` 순서(C1, C2, C3)이고 항목은 3절 `CommentResponse`다. 다른 글의 댓글은 섞이지 않는다. 댓글이 없으면 `items: []`·`total: 0`이다 |
| AC2 | P에 댓글이 201개 있을 때 | 목록을 요청하면 | `items`는 오래된 200개, `total`은 201이다 (응답 크기 안전장치, 상수 `MAX_COMMENTS = 200`) |
| AC3 | 작성자가 탈퇴한(`users.status = 'WITHDRAWN'`) 댓글이 있을 때 / 수정한 댓글이 있을 때 | 목록을 요청하면 | 탈퇴 작성자의 댓글도 목록·`total`에 그대로 나오고 `authorId`는 `null`, `authorNickname`은 `"탈퇴한 사용자"`다. 활성 회원의 댓글은 `authorId`와 현재 닉네임을 담는다. `edited`는 `updatedAt`이 `createdAt`보다 뒤일 때만 `true`다 |
| AC4 | 없는 글(`999999`)이거나 지워진 글일 때 | 목록·작성·수정·삭제를 요청하면 | 404 `POST_404` "게시글을 찾을 수 없습니다."이고 아무것도 바뀌지 않는다 |
| AC5 | 로그인 회원 U가 | `POST /api/posts/{P}/comments` `{content:"  같은 클래스 안 호출은 프록시를 거치지 않아서예요.  "}`를 보내면 | 201과 생성된 댓글의 `CommentResponse`(`authorId: U`, `edited: false`)를 받는다. 저장된 `content`는 앞뒤 공백을 뺀 값이다. `activities`에는 행이 생기지 않는다. 이후 글 상세·목록의 `commentCount`가 1 오른다. 남의 글·자기 글 모두 쓸 수 있다 |
| AC6 | 로그인 회원이 | `content`가 없음·`null`·공백만·501자(앞뒤 공백 제외 길이)로 작성·수정하면 | 400 `COMMON_400`과 `details.content` 사유를 받고 댓글은 생기거나 바뀌지 않는다. 정확히 500자(앞뒤 공백 포함 원문은 더 길어도 됨)는 성공한다 |
| AC7 | U가 P에 쓴 댓글 C(작성 시각 T)가 있을 때 | U가 `PUT /api/posts/{P}/comments/{C}` `{content:"고친 내용"}`을 보내면 | 200과 바뀐 `CommentResponse`를 받는다. `content`는 앞뒤 공백을 뺀 값, `id`·`createdAt`(T)은 그대로이고 `updatedAt`이 T보다 뒤라 `edited: true`다 (테스트는 `created_at`을 과거로 넣어 둔다). 같은 내용으로 보내도 200이고 `edited`는 바뀌지 않는다 |
| AC8 | U가 P에 쓴 댓글 C가 있을 때 | U가 `DELETE /api/posts/{P}/comments/{C}`를 보내면 | 204를 받고 C가 지워진다. 이후 목록에 C가 없고 `commentCount`가 1 줄어든다. 글 P와 다른 댓글은 그대로다 |
| AC9 | V가 쓴 댓글 C가 P에 있을 때 | U가 C를 `PUT`·`DELETE`하면 / 없는 댓글 id나 **다른 글의** 댓글 id를 P 경로로 `PUT`·`DELETE`하면 | 403 `POST_004` "본인이 쓴 댓글만 수정·삭제할 수 있습니다." / 404 `POST_003` "댓글을 찾을 수 없습니다."를 받고 C는 바뀌지 않는다. `PUT`의 판단 순서는 401 → 글(POST_404) → 댓글(POST_003) → 작성자(POST_004) → 형식 검증(400)이다 (남의 댓글에 빈 내용을 보내도 403). 글 작성자라도 남의 댓글은 수정·삭제할 수 없다 |
| AC10 | 토큰 없음·만료·형식 오류이거나, 탈퇴한 회원의 남은 Access 토큰일 때 | 작성·수정·삭제를 요청하면 | 401 `AUTH_401`이고 아무것도 바뀌지 않는다. 목록 조회는 토큰이 깨져 있어도 200이다 (공개 경로) |

### 프론트엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC11 | 누구나 | SC-15 `/community/:id`를 열면 | 본문·첨부 아래 "댓글 N"(목록 `total`) 절에 댓글이 오래된순으로 보인다. 각 댓글은 닉네임(`/users/{authorId}` 링크, `authorId`가 `null`이면 링크 없는 "탈퇴한 사용자"), 작성일(`YYYY-MM-DD HH:mm`), `edited`이면 "(수정됨)", 내용이다. 내용은 **글자 그대로**(줄바꿈 유지, HTML이 태그로 해석되지 않음) 보인다. 0건이면 "첫 댓글을 남겨 주세요", 불러오기 실패는 댓글 절 안에 서버 `message`(본문은 그대로 보인다). `total`이 `items` 수보다 크면 목록 아래 "댓글이 많아 처음 200개만 보입니다". 글이 404면 댓글 절을 보여 주지 않는다 |
| AC12 | SC-15에서 | 비로그인이면 / 로그인 회원이면 | 비로그인은 작성 폼 대신 "로그인하고 댓글 남기기" 링크(`/login?redirect=/community/{id}`)를 본다. 회원은 입력창과 "등록"을 본다: 공백뿐이면 "등록"이 비활성, 500자에서 더 입력되지 않고 아래 "n/500"이 보인다. "등록"을 누르면 `createComment` 후 입력창을 비우고 댓글 목록만 다시 불러온다(헤더 "댓글 N"도 갱신). 저장 중에는 버튼이 비활성이다. 400이면 `details.content`를 입력창 아래에, 그 밖의 실패는 서버 `message`를 보여 주고 입력은 남긴다 |
| AC13 | SC-15에서 | 로그인 회원이 자기 댓글(`authorId === auth.user.id`)을 보면 / 남의 댓글이나 비로그인이면 | 자기 댓글에만 머리 줄에 "수정"·"삭제" 글자 버튼이 보인다(375px에서도 같은 배치). "수정"은 그 자리를 값이 채워진 입력창("저장"·"취소", "n/500")으로 바꾸고, 한 번에 한 댓글만 수정 중이다. "저장"은 공백뿐이면 비활성이고 `updateComment` 후 목록을 다시 불러오며, "취소"는 요청 없이 원래 내용으로 돌아간다. "삭제"는 확인 대화상자에서 확인하면 `deleteComment` 후 목록을 다시 불러오고, 취소하면 아무 요청도 보내지 않는다. 수정·삭제가 400이면 입력창 아래 `details.content`, 403·404면 서버 `message`를 그 댓글 아래에 보여 준다. SC-15는 375px 폭에서 가로 스크롤이 없다 (NF-02) |

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다 (contract-checker, CI가 검사). Swagger Tag: `커뮤니티`. 기존 패키지 `com.vibenote.post`에 `CommentController`(`@RequestMapping("/api/posts/{postId}/comments")`), `CommentService`, `CommentMapper`(+`mapper/post/CommentMapper.xml`), `post/domain/Comment`, `post/dto`(`CommentRequest`·`CommentResponse`·`CommentListResponse`)를 추가한다.

### `GET /api/posts/{postId}/comments`

- 권한: 공개. `SecurityConfig.PUBLIC_GET_PATHS`의 POST 블록에 `"/api/posts/{id:\\d+}/comments"`를 추가한다 (공통 파일 → PR에서 @whans1996 멘션)
- 페이지 없음. 오래된순(`created_at ASC, id ASC`)으로 최대 200개, `total`은 그 글의 전체 댓글 수
- 응답 `200` (`CommentListResponse`)

```json
{
  "items": [
    {
      "id": 101,
      "content": "같은 클래스 안에서 호출하면 프록시를 거치지 않아서 트랜잭션이 안 걸려요.\n별도 빈으로 빼 보세요.",
      "authorId": 9,
      "authorNickname": "스프링러버",
      "edited": false,
      "createdAt": "2026-11-20T14:10:05+09:00",
      "updatedAt": "2026-11-20T14:10:05+09:00"
    },
    {
      "id": 104,
      "content": "저도 같은 문제였는데 self-invocation 검색하면 나와요.",
      "authorId": null,
      "authorNickname": "탈퇴한 사용자",
      "edited": true,
      "createdAt": "2026-11-20T15:01:40+09:00",
      "updatedAt": "2026-11-20T15:03:12+09:00"
    }
  ],
  "total": 2
}
```

### `POST /api/posts/{postId}/comments`

- 권한: 로그인 필요 (탈퇴 회원의 남은 토큰은 `getActiveMember`로 401)
- 요청 (`CommentRequest`)

```json
{ "content": "같은 클래스 안에서 호출하면 프록시를 거치지 않아서 트랜잭션이 안 걸려요." }
```

- 응답 `201`: 생성된 `CommentResponse` (위 항목 형식, `edited: false`)
- 판단 순서: 401 → 형식 검증 400(`@Valid`) → 글 존재(POST_404)

### `PUT /api/posts/{postId}/comments/{commentId}`

- 권한: 로그인 필요, 댓글 작성자 본인만
- 요청: `POST`와 같은 `CommentRequest`
- 응답 `200`: 바뀐 `CommentResponse`
- 판단 순서: 401 → 글(POST_404) → 댓글이 없거나 그 글의 댓글이 아님(POST_003) → 작성자(POST_004) → 형식 검증 400. F-17 `PUT`과 같이 Controller에 `@Valid`를 달지 않고 `CommentService.update`가 확인 뒤 `jakarta.validation.Validator`로 검증한다

### `DELETE /api/posts/{postId}/comments/{commentId}`

- 권한: 로그인 필요, 댓글 작성자 본인만
- 응답 `204` (본문 없음)
- 판단 순서: 401 → POST_404 → POST_003 → POST_004

### 에러 (위 엔드포인트 공통)

| 상태 | 코드 | 메시지 | 언제 |
|---|---|---|---|
| 400 | COMMON_400 | (검증) | `content` 없음·공백만·500자 초과(앞뒤 공백 제외), `details.content` |
| 401 | AUTH_401 | (공통) | 작성·수정·삭제에 토큰 없음·만료·탈퇴 회원 |
| 403 | POST_004 | 본인이 쓴 댓글만 수정·삭제할 수 있습니다. | 남의 댓글 `PUT`·`DELETE` (글 작성자여도) |
| 404 | POST_404 | 게시글을 찾을 수 없습니다. | 없는 글의 댓글 목록·작성·수정·삭제 |
| 404 | POST_003 | 댓글을 찾을 수 없습니다. | 없는 댓글, 경로의 글에 속하지 않는 댓글 |

- 새 코드 `POST_003`·`POST_004`는 `ErrorCode`와 `docs/conventions.md` 3절 POST 행에 추가한다

## 4. DB 변경

- Flyway: 없음. V1의 `comments`(`post_id` FK `ON DELETE CASCADE`, `user_id`, `content VARCHAR(1000)`, `created_at`, `updated_at ON UPDATE CURRENT_TIMESTAMP`, `idx_comments_post_created (post_id, created_at)`)를 그대로 쓴다. 500자는 애플리케이션 검증이다
- 사용 테이블: `comments`(읽기·쓰기), `posts`(존재 확인), `users`(닉네임·status)
- 목록: `WHERE post_id = ? ORDER BY created_at, id LIMIT 200` + `COUNT(*)`. 인덱스 `idx_comments_post_created`를 탄다
- 수정: `UPDATE comments SET content = ? WHERE id = ?`. `updated_at`은 `ON UPDATE`가 바꾼다 (내용이 같으면 MySQL이 행을 바꾸지 않아 `updated_at`도 그대로 → AC7 "같은 내용이면 `edited` 유지"). `DATETIME`이 초 단위라 작성 후 1초 안에 고치면 `edited`가 `false`로 남는다 (허용)
- 활동: 기록하지 않는다 (`ActivityType` 변경 없음, 11-20 결정)
- 탈퇴 회원의 댓글은 남고 작성자를 "탈퇴한 사용자"로 표시한다 (기획서 ERD 주석, F-17과 같은 방식). 글을 지우면 FK CASCADE로 댓글도 지워진다 (F-17 AC10에서 이미 확인)

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | 새 라우트 없음. SC-15 `views/community/PostDetailView.vue` 본문·첨부 아래에 댓글 절을 붙인다 (글을 불러온 뒤, 404가 아닐 때만) |
| 컴포넌트 | `components/community/PostComments.vue`(절 전체: "댓글 N", 작성 폼/로그인 링크, 목록, 0건·실패·200개 안내, `postId` prop), `CommentItem.vue`(한 댓글: 머리 줄·내용·내 댓글 수정/삭제, 인라인 수정), `CommentForm.vue`(입력창 + "n/500" + 등록/저장/취소, 작성·수정 겸용) — 새 파일 |
| store | 없음 (컴포넌트 지역 상태). 로그인 회원 id는 `useAuthStore().user.id` |
| API 함수 | `src/api/posts.js`에 `fetchComments(postId)`, `createComment(postId, {content})`, `updateComment(postId, commentId, {content})`, `deleteComment(postId, commentId)` 추가 |
| 상태별 UI | 불러오는 중 / 0건 "첫 댓글을 남겨 주세요" / 실패 시 댓글 절 안 서버 `message` / 200개 초과 안내 / 저장 중 버튼 비활성 / 400 필드 메시지 / 403·404 메시지 |
| 내용 표시 | 텍스트 보간(`{{ }}`) + `white-space: pre-wrap` + 긴 단어 줄바꿈(`overflow-wrap: anywhere`). `v-html` 쓰지 않는다 |
| 모바일 | 한 열. 수정·삭제는 데스크톱과 같은 글자 버튼 (⋯ 메뉴 없음, 11-20 결정 → `06-화면설계서.md` SC-15 문구 수정) |

## 6. LLM (AI 기능만)

해당 없음.

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1~AC3 | `CommentListApiTest#AC1_...`~`#AC3_...` (오래된순·다른 글 제외·0건, 201개 중 200개·total, 탈퇴 작성자·edited) | MockMvc + Testcontainers 통합 |
| AC4 | `CommentListApiTest#AC4_...`, `CommentWriteApiTest#AC4_...` (없는 글에 목록·작성·수정·삭제) | MockMvc 통합 |
| AC5~AC9 | `CommentWriteApiTest#AC5_...`~`#AC9_...` (trim·활동 없음·commentCount, 경계 500·501·공백·null, 수정 edited·같은 내용, 삭제, 403·404·다른 글 댓글·403이 400보다 먼저·글 작성자도 403) | MockMvc 통합 |
| AC10 | `CommentAuthApiTest#AC10_...` (토큰 없음·만료·형식 오류·탈퇴 회원 토큰, 공개 목록에 깨진 토큰) | MockMvc 통합 |
| AC11~AC13 | `components/community/__tests__/PostComments.spec.js`, `CommentItem.spec.js`, `CommentForm.spec.js`, `views/__tests__/PostDetailView.spec.js`(댓글 절 표시·404면 숨김) | Vitest |
| AC11~AC13 화면 | ui-verifier: SC-15 댓글 있음·없음·내 댓글 수정 중, 데스크톱·375px, 콘솔 에러 없음 | Playwright |
| API 계약 | contract-checker: spec ↔ `openapi.json` ↔ `src/api/posts.js` | 정적 |

## 8. 범위 밖

- 대댓글(답글)·멘션, 댓글 좋아요, 채택(Q&A 답변 채택)
- 댓글 페이지·"더 보기" (200개 상한만 둔다)
- 관리자나 글 작성자의 남의 댓글 삭제, 신고
- 피드 활동(`COMMENT`) 기록과 알림
- 프로필(SC-18)의 "내가 쓴 댓글" 목록
- 댓글 작성 중 화면 이탈 확인
- 댓글 마크다운·코드 하이라이트 (글자 그대로 표시만)

## 9. 결정 필요

- 없음

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/F-18-comments`
- [x] Flyway 마이그레이션 (없음)
- [x] AC별 실패 테스트 작성 (red 확인)
- [x] BE 구현 (green): `CommentController`, `CommentService`, `CommentMapper`(+XML), `Comment`, DTO 3종, `ErrorCode` `POST_003`·`POST_004`, `PUBLIC_GET_PATHS`
- [x] FE 구현 (green): `api/posts.js` 함수 4개, 컴포넌트 3개, `PostDetailView` 댓글 절
- [x] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [ ] contract-checker 통과
- [ ] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일)
- [ ] reviewer 지적 사항 반영
- [ ] 산출물 반영: `06-화면설계서.md` SC-15 "F-18에서 정할 것"(정렬·페이지)·모바일 버튼 문구 + 스크린샷, `docs/conventions.md` 3절 POST 코드, `02-유스케이스.md` UC41 "기획 기준" 표시 정리
- [ ] PR CI 초록
- [ ] 상태 DONE (PR 머지 후)
