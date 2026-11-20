# F-04 팔로우·활동 피드

| 항목 | 값 |
|---|---|
| 상태 | DONE (PR #71) <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-04 |
| 우선순위 | P0 |
| 영역 오너 | B 김준섭 (카드·소셜) |
| 관련 필수 요구사항 | ⑥ 팔로우/팔로잉 (명세서 매핑: 추가 · 회원 "팔로우 등록·조회·삭제") |
| 의존 spec | F-02 (로그인), F-03 (프로필 SC-18 `UserProfileView`·`UserService.getPublicMember`, 탈퇴 시 `FollowService.removeAllOf`), 활동 기록 F-00b `ActivityService`와 각 기능의 기록 시점 (F-05a `ANALYSIS`, F-11 `REVIEW`, F-12 `WISH`, F-17 `POST`) |
| 관련 이슈 | #64 (피드의 `WISH`는 아직 찜 중이고 공개된 카드만) → AC7, 구현 PR에 `Closes #64` |
| 승인 | 김준섭 2026-11-20 |

## 1. 목적

다른 학습자의 프로필에서 **팔로우**하고, 팔로우한 사람이 공개한 분석·남긴 리뷰·찜한 카드·쓴 글을 **피드(SC-17)** 에서 최신순으로 본다.
"남들은 무엇을 공부하나"를 보여 주어 학습 동기를 주고 카드·게시글로 다시 들어가는 길을 만든다 (결정로그 11-17 "팔로우: 공개 활동 피드").
활동은 이미 각 기능이 `activities`에 기록하고 있으므로, 이 기능은 **팔로우 관계 + 읽기(필터링)** 가 핵심이다.

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_다른_회원을_팔로우하면_200과_팔로우_상태를_반환한다()`

### 팔로우 (BE)

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 로그인한 회원이 활성 회원 B를 팔로우하지 않은 상태에서 | `POST /api/users/{B}/follow` | 200 `{following: true, followerCount, followingCount}`(B의 수, 방금 팔로우 반영). `follows`에 (나, B) 행이 1개 생긴다. **이미 팔로우 중이면** 같은 200을 돌려주고 행은 1개 그대로다 (멱등) |
| AC2 | 로그인한 회원이 B를 팔로우 중일 때 | `DELETE /api/users/{B}/follow` | 200 `{following: false, followerCount, followingCount}`. 행이 지워진다. **팔로우하지 않은 상태면** 같은 200 (멱등) |
| AC3 | | 자기 자신에게 `POST`·`DELETE` / 없거나 탈퇴한 회원에게 `POST`·`DELETE` / 토큰 없이 `POST`·`DELETE` / 탈퇴한 회원의 남은 Access 토큰으로 `POST`·`DELETE` | 각각 400 `USER_002` / 404 `USER_404` / 401 `AUTH_401` / 401 `AUTH_401` (F-12와 같은 방식). 어느 경우든 `follows`는 바뀌지 않는다 |
| AC4 | 누구나 (비로그인 포함) | `GET /api/users/{B}/follow` | 200 `{following, followerCount, followingCount}`. `following`은 로그인한 내가 B를 팔로우 중일 때만 `true`(비로그인·내 프로필이면 `false`). 없거나 탈퇴한 B면 404 `USER_404` |
| AC5 | 누구나 | `GET /api/users/{B}/followers`, `GET /api/users/{B}/followings` (`?page=&size=`, 기본 20) | 200 `PageResponse` `{items: [{id, nickname, level, followedAt}], page, size, total}`. 팔로우한 시각 최신순(같으면 회원 id 내림차순). 없거나 탈퇴한 B면 404 `USER_404`, page·size가 범위 밖이면 400 `COMMON_400` |

### 피드 (BE)

| ID | Given | When | Then |
|---|---|---|---|
| AC6 | 로그인한 회원 A가 B·C를 팔로우하고, B·C·D(팔로우 안 함)·A 자신이 활동을 남겼을 때 | `GET /api/feed` (`?page=&size=`, 기본 20) | 200 `PageResponse`. **B·C의 활동만** `created_at` 최신순(같으면 활동 id 내림차순)으로 나온다. A 자신·D의 활동은 없다. 항목은 `{id, type, createdAt, actor: {id, nickname}, target: {id, title, rating}}` (3절). 토큰이 없으면 401 `AUTH_401` |
| AC7 | 팔로우한 사람의 활동 대상이 지금 볼 수 없는 상태일 때 | `GET /api/feed` | 다음 항목은 **목록과 `total`에서 빠진다**: `ANALYSIS` — 분석이 비공개(`is_public = FALSE`)·삭제(`deleted_at` 있음)·`DONE`이 아님 / `REVIEW` — 리뷰가 지워졌거나 카드가 `PUBLISHED`가 아님 / `WISH` — 찜을 해제했거나(`concept_wishes` 행 없음) 카드가 `PUBLISHED`가 아님 (#64) / `POST` — 글이 지워짐 / `CHALLENGE_JOIN`·`CHALLENGE_COMPLETE` — 챌린지가 지워짐. 다시 찜하거나 공개로 바꾸면 원래 활동이 원래 시각 그대로 다시 보인다 |
| AC8 | 팔로우한 사람이 없거나, 있어도 보일 활동이 없을 때 | `GET /api/feed` | 200 `{items: [], page: 1, size: 20, total: 0}` |

### 프론트엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC9 | SC-18 프로필을 열면 | | 닉네임 아래 "팔로워 N · 팔로잉 N" 링크(각각 `/users/:id/follows?tab=followers`·`followings`)가 보인다. **남의 프로필**이면 로그인한 회원에게 "팔로우"(팔로우 중이면 "팔로잉", 마우스를 올리면 "언팔로우") 버튼이 보이고, 누르면 요청 중 비활성 → 응답의 `following`·수로 버튼과 숫자를 바꾼다. 비로그인이 누르면 `/login?redirect=/users/:id`로 간다. 내 프로필에는 버튼이 없다. 요청이 실패하면 버튼 옆에 서버 `message`를 보이고 상태는 그대로다 |
| AC10 | `/users/:id/follows?tab=followers\|followings` (SC-18 팔로우 목록) | 화면을 열거나 탭을 바꾸면 | 제목 "{닉네임}님의 팔로우", 탭 "팔로워 N"·"팔로잉 N"(탭은 `?tab=`과 동기화, 잘못된 값이면 followers), 항목은 이니셜·닉네임(누르면 SC-18)·수준 배지. 20명씩 "더 보기"(이미 보인 id는 거름). 0명이면 "아직 팔로워가 없습니다."·"아직 팔로우한 사람이 없습니다.", `USER_404`면 "탈퇴했거나 없는 사용자입니다.", 그 밖의 에러는 서버 `message`. "← 프로필로" 링크 |
| AC11 | 로그인한 회원이 `/feed` (SC-17)를 열면 | | 상단 nav "피드"(회원만)로 들어온다. 비로그인은 `/login?redirect=/feed`. 항목은 이니셜·닉네임(SC-18 링크)·상대 시각("방금 전"·"n분 전"·"n시간 전"·"어제"·"n일 전", 7일 넘으면 `YYYY-MM-DD`)과 종류별 문장·대상 링크(아래 표). 20개씩 "더 보기"는 이미 보인 활동 id를 거른다. 빈 피드면 내 `followingCount`(AC4, 내 id)가 0일 때 "아직 팔로우한 사람이 없어요" + "커뮤니티 둘러보기"(`/community`), 아니면 "새 활동이 없습니다". 에러는 서버 `message` |

AC11 종류별 문장 (SC-17):

| type | 문장 · 대상 | 링크 |
|---|---|---|
| `ANALYSIS` | 새 분석을 공개했어요 · {분석 제목 또는 프로젝트명} | `/analyses/:id` (SC-07) |
| `REVIEW` | 카드에 리뷰를 남겼어요 · {카드 이름} ★{rating} | `/concepts/:id` (SC-10) |
| `WISH` | 카드를 찜했어요 · {카드 이름} | `/concepts/:id` (SC-10) |
| `POST` | 글을 썼어요 · {글 제목} | `/community/:id` (SC-15) |
| `CHALLENGE_JOIN` / `CHALLENGE_COMPLETE` | 챌린지에 참여했어요 / 챌린지를 달성했어요 · {챌린지 제목} | `/challenges/:id` (SC-13, F-25가 화면을 만든다) |
| 그 밖(모르는 type) | 항목을 그리지 않는다 | |

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다. Swagger Tag: 팔로우 API는 `팔로우`, 피드는 `피드` (`docs/conventions.md` 8절).
> 공개 GET 3개(`/api/users/{id}/follow`, `/followers`, `/followings`)는 `SecurityConfig.PUBLIC_GET_PATHS`의 USER 블록 아래 **FOLLOW 블록**에 숫자 id 패턴으로 더한다 (`/api/users/me/...`는 열리지 않는다).

### `POST /api/users/{id}/follow` · `DELETE /api/users/{id}/follow`

- 권한: 로그인 필요. 요청 본문 없음
- 응답 `200` (POST·DELETE 같은 모양, 수는 대상 회원 B 기준)

```json
{ "following": true, "followerCount": 12, "followingCount": 3 }
```

- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 400 | USER_002 | 자기 자신 ("자기 자신은 팔로우할 수 없습니다.") |
| 401 | AUTH_401 | 토큰 없음·무효, 탈퇴한 회원의 토큰 |
| 404 | USER_404 | 없거나 탈퇴한 회원 |

### `GET /api/users/{id}/follow`

- 권한: 공개 (토큰이 있으면 `following`을 계산한다)
- 응답 `200`: 위와 같은 모양. 비로그인·본인이면 `"following": false`
- 에러: 404 `USER_404`

### `GET /api/users/{id}/followers` · `GET /api/users/{id}/followings`

- 권한: 공개. `?page=1&size=20` (`PageQuery`)
- 응답 `200` (`followers`면 나를 팔로우한 사람, `followings`면 내가 팔로우한 사람. `followedAt` = `follows.created_at`)

```json
{
  "items": [
    { "id": 7, "nickname": "java러버", "level": "JUNIOR", "followedAt": "2026-11-20T15:02:11" }
  ],
  "page": 1, "size": 20, "total": 12
}
```

- 에러: 400 `COMMON_400`(page·size), 404 `USER_404`

### `GET /api/feed`

- 권한: 로그인 필요. `?page=1&size=20` (`PageQuery`)
- 응답 `200`. `id`는 활동 id. `target.id`는 **링크할 대상의 id**다 (`REVIEW`는 리뷰 id가 아니라 카드 id). `rating`은 `REVIEW`만 값이 있고 나머지는 `null`. `ANALYSIS`의 `title`은 분석 제목, 없으면 프로젝트명, 둘 다 없으면 `"제목 없는 분석"`

```json
{
  "items": [
    {
      "id": 91, "type": "REVIEW", "createdAt": "2026-11-20T13:40:00",
      "actor": { "id": 7, "nickname": "java러버" },
      "target": { "id": 12, "title": "JWT 기반 인증", "rating": 5 }
    },
    {
      "id": 88, "type": "POST", "createdAt": "2026-11-20T11:05:00",
      "actor": { "id": 3, "nickname": "코딩초보" },
      "target": { "id": 40, "title": "@Transactional이 안 먹어요", "rating": null }
    }
  ],
  "page": 1, "size": 20, "total": 2
}
```

- `type` 허용값: `ANALYSIS`, `REVIEW`, `WISH`, `POST`, `CHALLENGE_JOIN`, `CHALLENGE_COMPLETE` (`ActivityType`)
- 에러: 400 `COMMON_400`(page·size), 401 `AUTH_401`

## 4. DB 변경

- Flyway: **없음** (V1의 `follows` PK `(follower_id, following_id)`·`idx_follows_following`, `activities`의 `idx_activities_user_created`로 충분하다)
- 사용 테이블: `follows`, `activities`, `users`, `analyses`, `concept_reviews`, `concepts`, `concept_wishes`, `posts`, `challenges`
- 팔로우 쓰기
  - 팔로우: 대상이 활성 회원인지 확인한 뒤 `INSERT IGNORE INTO follows` (PK가 중복을 막아 동시 요청에도 멱등). 탈퇴와 겹치지 않게 대상 `users` 행을 `FOR UPDATE`로 읽고 확인한다 (탈퇴는 같은 행을 UPDATE하므로 직렬화된다)
  - 언팔로우: `DELETE FROM follows WHERE follower_id = ? AND following_id = ?`
  - 응답의 수는 같은 트랜잭션에서 `COUNT(*)`로 다시 센다 (반정규화 컬럼 없음). 탈퇴 시 `follows`를 양방향으로 지우므로(F-03) 수에 탈퇴 회원은 없다
- 팔로우는 피드 활동을 남기지 않는다 (`ActivityType` 변경 없음, 기획서 F-04 피드 대상에 없음)
- 피드 조회 (`feed` 패키지 `FeedMapper.xml`, 한 쿼리 + 같은 조건의 `COUNT(*)`)
  - `activities a JOIN follows f ON f.following_id = a.user_id AND f.follower_id = #{me}` `JOIN users u ON u.id = a.user_id AND u.status = 'ACTIVE'`
  - 종류별 `LEFT JOIN` 후 `WHERE`에서 종류마다 보일 조건을 `OR`로 묶는다 (AC7): `analyses`(id, `is_public`, `status='DONE'`, `deleted_at IS NULL`) / `concept_reviews` → `concepts`(`PUBLISHED`) / `concept_wishes`(user_id = a.user_id, concept_id = a.target_id) + `concepts`(`PUBLISHED`) / `posts` / `challenges`
  - `ORDER BY a.created_at DESC, a.id DESC LIMIT #{size} OFFSET #{offset}`
  - 대상 title·rating은 같은 쿼리에서 `CASE a.type`으로 고른다. `${}`를 쓰지 않는다

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | `/feed` → `views/community/FeedView.vue` (`routes/community.js`, `meta: { requiresAuth: true, nav: { label: '피드', order: 7, show: 'user' } }`) · `/users/:id(\\d+)/follows` → `views/UserFollowsView.vue` (`routes/user.js`) · SC-18 `views/UserProfileView.vue`에 팔로우 영역 추가 |
| 컴포넌트 | `components/follow/FollowSummary.vue`(수 링크 + 버튼, `UserProfileView`가 프로필 로드 후 그림), `components/follow/FollowUserItem.vue`, `components/feed/FeedItem.vue`(종류별 문장·링크) |
| store | 없음 (화면 지역 상태). 로그인 여부·내 id는 `stores/auth` |
| API 함수 | `src/api/follows.js`: `fetchFollowStatus(id)`, `follow(id)`, `unfollow(id)`, `fetchFollowers(id, page)`, `fetchFollowings(id, page)` · `src/api/feed.js`: `fetchFeed(page)` |
| 유틸 | `utils/relativeTime.js` `formatRelativeTime(iso, now)` (AC11 규칙, `now`를 받아 테스트에서 고정) |
| 상태별 UI | 로딩 "불러오는 중…" / 빈 목록(AC10·AC11 문구) / 에러는 서버 `message` / 팔로우 영역만 실패하면 영역 안에 메시지, 프로필은 그대로 |
| 모바일 | 피드 항목 두 줄(1줄 닉네임·시각, 2줄 문장·대상), 팔로우 목록 한 열, 375px에서 버튼이 닉네임 아래로 내려간다 |

## 6. LLM (AI 기능만)

해당 없음.

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1~AC3 | `follow/FollowApiTest#AC1_…`~`#AC3_…` (멱등·행 수·에러 코드별 follows 불변) | MockMvc 통합 |
| AC4, AC5 | `follow/FollowQueryApiTest#AC4_…`, `#AC5_…` (비로그인·본인·팔로우 중, 정렬·페이지·탈퇴 404) | MockMvc 통합 |
| AC6~AC8 | `feed/FeedApiTest#AC6_…`~`#AC8_…` (활동 행을 직접 넣고 종류별 숨김 조건을 하나씩 확인, `total` 포함, 재찜하면 다시 보임) | MockMvc 통합 |
| AC9 | `components/follow/__tests__/FollowSummary.spec.js`, `views/__tests__/UserProfileView.spec.js`(팔로우 영역 표시·본인 숨김) | Vitest |
| AC10 | `views/__tests__/UserFollowsView.spec.js` | Vitest |
| AC11 | `views/community/__tests__/FeedView.spec.js`, `components/feed/__tests__/FeedItem.spec.js`, `utils/__tests__/relativeTime.spec.js` | Vitest |
| 전체 | ui-verifier: `/feed`(빈 피드·항목), `/users/:id`(팔로우 버튼), `/users/:id/follows` 데스크톱 1280·모바일 375 | Playwright |

## 8. 범위 밖

- 팔로우 알림, 맞팔로우 표시, 팔로우 추천, 차단
- 피드에서 내 활동 보기 (팔로우한 사람만, 결정로그 11-20)
- 댓글·팔로우 활동 (기록하지 않는다)
- 분석 공개 설정과 남의 공개 분석 상세 보기는 F-19(A). F-19 전에는 공개 분석이 없어 `ANALYSIS` 항목이 피드에 나오지 않는다
- 챌린지 화면(SC-13)과 `CHALLENGE_*` 활동 기록은 F-25(A). 이 spec은 피드 쿼리·문장만 준비한다
- 대시보드의 피드 진입 링크 (대시보드는 A 영역, 필요하면 follow-up 이슈)
- 커서 방식 페이지 (페이지 번호 + FE id 중복 거르기로 대신한다)

## 9. 결정 필요

- 없음 (11-20 결정: 팔로우·언팔로우 멱등 200, 피드 페이지 번호 + "더 보기", 팔로우 목록은 별도 화면, 피드는 팔로우한 사람만)

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/F-04-follow-feed`
- [x] Flyway 마이그레이션 (없음)
- [x] AC별 실패 테스트 작성 (red 확인)
- [x] BE 구현 (green): `ErrorCode` USER 블록 `USER_002`, `SecurityConfig` FOLLOW 블록, `follow` 패키지 Controller·Service·Mapper, `feed` 패키지 `FeedController`·`FeedService`·`FeedMapper`
- [x] FE 구현 (green)
- [x] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [x] contract-checker 통과 (불일치 0, 11-20)
- [x] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일) — 11-20, `/feed`(빈·항목)·`/users/:id`·`/users/:id/follows` 1280·375 문제 0, 스크린샷 SC-17·SC-18 8장
- [x] reviewer 지적 사항 반영 — 11-20, 반드시 수정 0. 권장: 빈 피드 안내 깜빡임·팔로우 직후 "언팔로우" 표시(FE), 팔로우 잠금 FOR SHARE, 피드 상대 시각 #17 댓글
- [x] 산출물 반영: 유스케이스 UC05(멱등으로 대안 흐름 수정)·UC06, 화면 설계서 SC-17("spec에서 정할 것" 정리, 스크린샷)·SC-18(팔로우 영역, 팔로우 목록 화면), REST API 명세
- [x] 공통 파일 변경(`ErrorCode`, `SecurityConfig`, `feed` 패키지, `routes/user.js`, `UserProfileView`) PR 본문에 @whans1996 멘션
- [x] PR CI 초록, `Closes #64` (PR #71, Codex ✅ fdbfcc8)
- [ ] 상태 DONE (PR 머지 후)
