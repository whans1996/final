# F-03 프로필 관리·회원 탈퇴

| 항목 | 값 |
|---|---|
| 상태 | DONE (PR #4 머지, 2026-11-18) <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-03 |
| 우선순위 | P0 |
| 영역 오너 | 팀원 B (학습·소셜) |
| 관련 필수 요구사항 | ⑤ 회원 프로필 관리 (명세서 F1306~F1310 회원 조회·수정·삭제(비활성화)), 명세서 "분석·점수 표현"(F1305)의 학습 통계 |
| 의존 spec | F-01 회원가입, F-02 로그인·JWT (둘 다 DONE) |
| 승인 | whans1996 2026-11-18 |

## 1. 목적

회원이 내 정보(닉네임·소개·학습 수준)를 고치고, 자기와 다른 사람의 **학습 통계**(분석 수, 학습한 카드, 퀴즈 정답률)를 프로필에서 본다.
수준을 바꾸면 이후 분석 설명의 기본 난이도가 바뀐다. 탈퇴는 행을 지우지 않고 비활성화(`status=WITHDRAWN`)하며 개인정보를 즉시 마스킹한다.
작성한 글·댓글·리뷰와 퀴즈 기록은 남긴다 (V2 결정: 작성물 FK RESTRICT, 통계 보존).

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_로그인하면_내_프로필을_받는다()`

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 로그인한 회원이 | `GET /api/users/me` 하면 | 200과 `{id, email, nickname, bio, level, role, createdAt}`을 받는다. 토큰이 없으면 401 `AUTH_401` |
| AC2 | 누구나(비로그인 포함) | `GET /api/users/{id}` 하면 | 200과 `{id, nickname, bio, level, createdAt}`을 받는다 (**email·role 없음**). 없는 id거나 WITHDRAWN 회원이면 404 `USER_404`. `GET /api/users/{id}/stats`도 같은 규칙 |
| AC3 | 회원에게 분석·퀴즈·복습 기록이 있을 때 | `GET /api/users/{id}/stats` 하면 | `analysisCount` = status `DONE`이고 `deleted_at IS NULL`인 분석 수. `learnedConceptCount` = 정답(`is_correct`)인 `quiz_answers` 문항의 concept ∪ `review_plans` 중 `DONE`의 concept (중복 제거, `concept_id` NULL 제외). `quizAccuracy` = `round(100 × Σscore / Σtotal)` (정수 %), 풀이가 없으면 `null`. 기록이 없으면 `0, 0, null`. 다른 회원의 기록은 세지 않는다 |
| AC4 | 로그인한 회원이 | `PATCH /api/users/me`에 `{nickname?, bio?, level?}`를 보내면 | 보낸 필드만 바뀌고 `null`·생략한 필드는 그대로다. bio는 trim하고 빈 문자열이면 NULL로 지운다. 자기 현재 닉네임을 그대로 보내도 409가 아니다. 200과 수정된 내 프로필(AC1 형식)을 받는다 |
| AC5 | 로그인한 회원이 | 규칙을 어긴 값이나 다른 회원의 닉네임으로 수정하면 | 닉네임 규칙(F-01과 같음)·bio 300자 초과·허용되지 않은 level → 400 `COMMON_400`, `details`에 필드별 사유. 다른 회원이 쓰는 닉네임(대소문자 무시) 또는 저장 시 DB UNIQUE 위반(`DuplicateKeyException`) → 409 `USER_409`, `details.nickname` |
| AC6 | 로그인한 회원이 | `DELETE /api/users/me`에 맞는 `{password}`를 보내면 | 204와 refresh 쿠키 삭제 `Set-Cookie`를 받는다. **한 트랜잭션에서** status=`WITHDRAWN`, withdrawn_at=NOW(), email=`withdrawn:{id}`, nickname=`#탈퇴{id}`, bio=NULL, password_hash=로그인할 수 없는 값으로 바꾸고, 그 회원의 `refresh_tokens` **전부**와 `follows` **양방향** 행을 지운다. 그 회원의 분석은 진행 중이면 FAILED로 바꾸고 모두 soft delete하며 `analysis_files`·`analysis_flows`·`analysis_concepts`를 지운다 (#91, 11-25 추가). `posts`·`comments`·`concept_reviews`·`quiz_attempts`·`concept_wishes`는 남는다 |
| AC7 | 로그인한 회원이 | password 없이, 또는 틀린 password로 탈퇴하면 | 없으면 400 `COMMON_400` `details.password`, 틀리면 400 `USER_001` `details.password`. DB는 바뀌지 않는다 (users·refresh_tokens·follows 그대로) |
| AC8 | 회원이 탈퇴한 뒤 | 옛 계정으로 다시 접근하면 | 옛 이메일·비밀번호 로그인 → 401 `AUTH_001`. 다른 기기의 refresh 쿠키 → 401 `AUTH_002`. 아직 만료되지 않은 Access 토큰으로 `GET·PATCH·DELETE /api/users/me` → 401 `AUTH_401`. 같은 이메일·닉네임으로 재가입 → 201 (새 id) |
| AC9 | 누구나 | `/users/:id` 화면(SC-18)을 열면 | 닉네임·수준·소개·가입일과 통계 3개를 본다 (정답률이 `null`이면 "-"). 내 프로필이면 "정보 수정"(`/me/settings`) 링크가 보인다. 404면 "탈퇴했거나 없는 사용자입니다."를 보여 준다. 로그인 상태의 헤더 닉네임은 내 프로필 링크다 |
| AC10 | 로그인한 회원이 | `/me/settings` 화면(SC-19)에서 수정·탈퇴하면 | 폼은 `fetchMe()` 값으로 채워진다. 저장에 성공하면 성공 문구를 보여 주고 auth store의 user(nickname·level)를 갱신한다(헤더 닉네임이 바로 바뀜). 서버 `details`는 필드 아래에 표시한다. 탈퇴는 비밀번호 입력과 "탈퇴하면 되돌릴 수 없습니다" 확인 체크가 있어야 버튼이 활성화된다. 성공하면 세션을 비우고(`auth.clear()`) 홈으로 이동한다. `USER_001`은 비밀번호 필드 아래에 표시한다 |

### 검증 규칙

| 필드 | 규칙 | 에러 메시지 (`details` 값) |
|---|---|---|
| nickname | 선택. 보내면 trim 후 2~20자, `^[가-힣A-Za-z0-9_]+$` (`SignupRequest.NICKNAME_PATTERN` 재사용) | "닉네임은 2~20자의 한글·영문·숫자·_만 사용할 수 있습니다." |
| bio | 선택. trim 후 ≤300자 (`users.bio VARCHAR(300)`). 빈 문자열이면 NULL | "소개는 300자 이하로 입력하세요." |
| level | 선택. `BEGINNER`·`JUNIOR`·`SENIOR` 중 하나 (문자열로 받아 검증, F-01과 같은 방식) | "학습 수준을 선택하세요." |
| password (탈퇴) | 필수 (`@NotBlank`). 형식은 검사하지 않고 BCrypt `matches`만 한다 | "비밀번호를 입력하세요." / 불일치: "비밀번호가 올바르지 않습니다." |

### 탈퇴 마스킹 규칙

| 컬럼 | 탈퇴 후 값 | 이유 |
|---|---|---|
| email | `withdrawn:{id}` | `@` 가 없어 가입 검증을 통과할 수 없으므로 새 가입과 UNIQUE 충돌이 없다. 옛 이메일로 바로 재가입 가능 |
| nickname | `#탈퇴{id}` (≤30자) | `#`이 가입 패턴 밖이라 충돌 없음. 화면 표시는 이 값이 아니라 `status`로 "탈퇴한 사용자"를 쓴다 (F-17·F-18·F-11) |
| bio | NULL | 개인정보 |
| password_hash | BCrypt 형식이 아닌 고정값 (예: `!WITHDRAWN`) | `matches`가 항상 false. 로그인은 그 전에 `isActive()`로도 막힌다 |
| status / withdrawn_at | `WITHDRAWN` / NOW() | |

- 재가입한 계정은 옛 기록과 연결하지 않는다 (새 id).

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다 (contract-checker, CI가 검사). Swagger Tag: `회원`. 새 Controller `com.vibenote.user.UserController` (`/api/users`).

### `GET /api/users/me`

- 권한: 로그인 필요 (탈퇴 회원의 남은 Access 토큰은 401 `AUTH_401`)
- 응답 `200`

```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "코딩초보",
  "bio": "AI로 만든 Spring 프로젝트를 공부하고 있어요.",
  "level": "BEGINNER",
  "role": "USER",
  "createdAt": "2026-11-18T10:30:00"
}
```

### `PATCH /api/users/me`

- 권한: 로그인 필요
- 요청 (모든 필드 선택, `null`·생략은 변경 없음)

```json
{ "nickname": "주니어개발자", "bio": "", "level": "JUNIOR" }
```

- 응답 `200`: `GET /api/users/me`와 같은 형식 (위 예에서 `bio`는 `null`)
- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 400 | COMMON_400 | 검증 실패 (`details`: 필드별 사유) |
| 401 | AUTH_401 | 토큰 없음·만료·탈퇴 회원 |
| 409 | USER_409 | 다른 회원이 쓰는 닉네임 (`details.nickname`) |

### `DELETE /api/users/me`

- 권한: 로그인 필요
- 요청 (JSON 본문. FE는 axios `delete(url, { data })`)

```json
{ "password": "vibe1234" }
```

- 응답 `204` (본문 없음), 헤더 `Set-Cookie: refresh_token=; Path=/api/auth; Max-Age=0` (`RefreshTokenCookie.clear()`)
- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 400 | COMMON_400 | password 누락 (`details.password`) |
| 400 | USER_001 | 비밀번호 불일치 (`details.password`). **신규 코드** |
| 401 | AUTH_401 | 토큰 없음·만료·이미 탈퇴 |

```json
{ "code": "USER_001", "message": "비밀번호가 올바르지 않습니다.", "details": { "password": "비밀번호가 올바르지 않습니다." } }
```

### `GET /api/users/{id}`

- 권한: 공개 (`SecurityConfig`에 **GET 전용** 공개 규칙 `/api/users/{id:\d+}`, `/api/users/{id:\d+}/stats` 추가. `/api/users/me`는 계속 인증 필요)
- 응답 `200`

```json
{ "id": 1, "nickname": "코딩초보", "bio": "AI로 만든 Spring 프로젝트를 공부하고 있어요.", "level": "BEGINNER", "createdAt": "2026-11-18T10:30:00" }
```

- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 404 | USER_404 | 없는 id 또는 탈퇴 회원. **신규 코드** |

```json
{ "code": "USER_404", "message": "존재하지 않거나 탈퇴한 사용자입니다.", "details": null }
```

### `GET /api/users/{id}/stats`

- 권한: 공개 (위와 같음)
- 응답 `200`

```json
{ "analysisCount": 3, "learnedConceptCount": 12, "quizAccuracy": 75 }
```

풀이 기록이 없으면 `"quizAccuracy": null`.

- 에러: `GET /api/users/{id}`와 같음 (404 `USER_404`)

## 4. DB 변경

- Flyway: 없음 (V1 `users`의 `bio`·`level`·`status`·`withdrawn_at` 그대로 사용)
- 사용 테이블
  - `users`: SELECT, UPDATE(수정·탈퇴 마스킹). 조회 컬럼에 `created_at` 추가
  - `refresh_tokens`: `DELETE WHERE user_id = ?` (탈퇴)
  - `follows`: `DELETE WHERE follower_id = ? OR following_id = ?` (탈퇴)
  - 통계(읽기 전용): `analyses`, `quiz_attempts`, `quiz_answers`, `quiz_questions`, `review_plans`
- 패키지 구성 (순환 의존 방지)
  - `UserService`: `getMe`, `getPublicProfile`, `updateMe` 추가. `UserMapper.xml`에 `update`, `existsByNicknameExceptId`, `withdraw`
  - **`user/UserWithdrawalService`** (탈퇴 전용): 이미 `AuthService → UserService` 의존이 있어서 UserService가 AuthService를 부르면 순환이 생긴다. UserMapper·PasswordEncoder·`AuthService.revokeAll(userId)`·`FollowService.removeAllOf(userId)`를 쓴다
  - `auth`: `AuthService.revokeAll(userId)`, `RefreshTokenMapper.deleteByUserId`
  - `follow` (F-04가 확장): 최소 `FollowService.removeAllOf(userId)`, `FollowMapper.xml`
  - `user/UserStatsMapper.xml`: 여러 도메인 테이블을 읽기 전용으로 집계한다 (주석으로 명시)
- 탈퇴 회원의 Access 토큰: `JwtAuthenticationFilter`는 DB를 보지 않는다. `/api/users/me` 계열은 서비스에서 회원을 다시 읽어 active가 아니면 `AUTH_401`을 던진다

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | `/users/:id` → `UserProfileView.vue` (SC-18, 공개). `/me/settings` → `MySettingsView.vue` (SC-19, `meta.requiresAuth`) |
| 컴포넌트 | 프로필: 헤더(닉네임·수준 배지·가입일·소개) + 통계 타일 3개(분석 수 / 학습한 카드 / 퀴즈 정답률). 설정: 정보 수정 폼(닉네임, 소개 textarea 300자 카운터, 수준 라디오 카드 — SignupView와 같은 모양) + 하단 "회원 탈퇴" 영역(비밀번호, 확인 체크, 위험 버튼). `App.vue` 헤더 닉네임 → `/users/{내 id}` 링크 |
| store | `useAuthStore().updateUser(partial)` 추가 (nickname·level 반영). 탈퇴 성공 시 `clear()` (서버가 쿠키·토큰을 이미 폐기하므로 logout 호출 안 함) |
| API 함수 | `src/api/users.js`의 `fetchMe()`, `updateMe(payload)`, `withdraw(password)`, `fetchUser(id)`, `fetchUserStats(id)` |
| 상태별 UI | 로딩 / 404 "탈퇴했거나 없는 사용자입니다." / 필드별 에러 / 그 밖의 에러는 `errorMessage()`로 폼 상단 / 저장·탈퇴 중 버튼 비활성화 |

## 6. LLM (AI 기능만)

해당 없음.

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1 | `backend/src/test/java/com/vibenote/user/UserProfileApiTest#AC1_...` | MockMvc 통합 |
| AC2 | `UserProfileApiTest#AC2_...` (비로그인 200, email 없음, 없는 id·탈퇴 회원 404) | MockMvc 통합 |
| AC3 | `user/UserStatsApiTest#AC3_...` (JdbcTemplate로 concepts·analyses(DONE/FAILED/삭제)·quizzes·quiz_questions·quiz_attempts·quiz_answers·review_plans 삽입, 다른 회원 기록 포함) | MockMvc 통합 |
| AC4 | `UserProfileApiTest#AC4_...` | MockMvc 통합 (DB 확인) |
| AC5 | `UserProfileApiTest#AC5_...` (`@ParameterizedTest`), `UserServiceTest#AC5_수정_저장_시_DuplicateKey면_409` | MockMvc 통합 + 단위 |
| AC6 | `user/WithdrawalApiTest#AC6_...` (refresh 2개·follows 양방향·posts 행 삽입 후 확인) | MockMvc 통합 |
| AC7 | `WithdrawalApiTest#AC7_...` | MockMvc 통합 |
| AC8 | `WithdrawalApiTest#AC8_...` (로그인·refresh·`/me` 3종·재가입) | MockMvc 통합 |
| 공개 경로 | `common/SecurityConfigTest` (`/api/users/1`·`/1/stats` GET 공개, `/api/users/me`·PATCH `/api/users/1`은 401) | MockMvc |
| AC9 | `frontend/src/views/__tests__/UserProfileView.spec.js` (`vi.mock('@/api/users')`) | Vitest |
| AC10 | `frontend/src/views/__tests__/MySettingsView.spec.js`, `stores/__tests__/auth.spec.js#updateUser` | Vitest |

## 8. 범위 밖

- 비밀번호 변경·찾기, 이메일 변경, 프로필 이미지
- 프로필의 팔로우 버튼·팔로워/팔로잉 목록(F-04), 찜 목록(F-12), 참여 챌린지(F-25) — SC-18의 해당 영역은 각 기능에서 추가
- 작성물의 "탈퇴한 사용자" 표시 → **후속**: F-17·F-18·F-11에서 작성자 조회 시 `users.status`로 처리
- 탈퇴 철회·복구, 재가입 제한 기간, 탈퇴 비밀번호 시도 횟수 제한
- 다른 도메인 API에서 탈퇴 회원 Access 토큰 차단 (Access 30분 동안 유효. 필요하면 필터의 DB 조회를 따로 결정)
- 관리자 강제 탈퇴

## 9. 결정 필요

- (확정 11-18: 즉시 마스킹·재가입 허용 / 탈퇴 시 비밀번호 재확인, 변경은 제외 / 학습한 카드 = 퀴즈 정답 ∪ 복습 완료 / 탈퇴 시 refresh 전부·follows 양방향 삭제)
- 반대 의견(일정): WBS `3.1 회원가입·로그인·프로필·탈퇴`(11/20~11/26)가 W1 마감(11/23)을 넘는다. F-03은 AC 10개, 1.5~2일 규모라 11/21까지 끝내면 W1 안에 들어온다. WBS 조정은 사람이 결정

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/F-03-profile`
- [x] Flyway 마이그레이션 (없음)
- [x] AC별 실패 테스트 작성 (red 확인)
- [x] BE 구현 (green)
- [x] FE 구현 (green)
- [x] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [x] `ErrorCode` USER_404·USER_001 + `docs/conventions.md` 3절 USER 줄
- [x] contract-checker 통과 (불일치 0, 11-18)
- [x] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일) (문제 0, 11-18)
- [x] reviewer 지적 사항 반영 (PR 전 reviewer + Codex PR 리뷰)
- [x] 산출물 반영 (`06-화면설계서.md` SC-18·SC-19 상세, `02-유스케이스.md` UC03·UC04 흐름)
- [x] PR CI 초록
- [x] 상태 DONE (PR 머지 후)
