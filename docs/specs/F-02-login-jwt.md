# F-02 로그인·로그아웃 (JWT)

| 항목 | 값 |
|---|---|
| 상태 | DRAFT <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-02 |
| 우선순위 | P0 |
| 영역 오너 | 팀원 B (학습·소셜) |
| 관련 필수 요구사항 | ① (명세서 F1306~F1310 로그인/로그아웃) |
| 의존 spec | F-01 (가입한 회원). **F-01과 같은 브랜치·PR `feature/F-01-auth`** |
| 승인 | (승인한 사람, 날짜) |

## 1. 목적

가입한 회원이 이메일·비밀번호로 로그인해 서비스를 쓰고, 새로고침이나 Access 만료에도 로그인이 끊기지 않으며, 로그아웃하면 그 기기의 세션이 서버에서도 폐기된다.
뼈대(F-00)에서 "F-02 spec에서 정한다"로 미뤄 둔 Refresh 토큰 전달·보관·재발급 방식을 여기서 확정한다.

### 토큰 방식 (확정)

| 항목 | 결정 |
|---|---|
| Access 토큰 | JWT(`sub`=userId, `role`), 30분 (`app.jwt.access-token-validity`). FE는 **메모리(Pinia)에만** 보관 |
| Refresh 토큰 | 32바이트 랜덤 문자열(base64url, JWT 아님), **14일** (`app.jwt.refresh-token-validity: 14d`). DB `refresh_tokens.token_hash`에 SHA-256 해시만 저장 |
| Refresh 전달 | **HttpOnly 쿠키** `refresh_token`: `HttpOnly; SameSite=Strict; Path=/api/auth; Max-Age=1209600`, `Secure`는 `app.jwt.refresh-cookie-secure`(기본 true, `local` 프로필 false). 응답 본문에는 넣지 않는다 |
| 회전 | `/refresh` 성공 시 기존 행을 지우고 새 토큰 행을 만든다 (1회용). 이미 쓴 토큰은 AUTH_002 |
| 세션 복원 | 앱 시작 시 FE가 `POST /api/auth/refresh`를 1회 호출. 성공하면 로그인 상태, 실패(401)하면 비로그인으로 조용히 시작 |
| 여러 기기 | 로그인마다 행을 하나씩 만든다 (기기별 세션). 로그아웃은 그 쿠키의 행만 지운다 |
| role 신뢰 | Access 토큰의 role·status는 만료(최대 30분)까지 다시 확인하지 않는다. `/refresh`가 DB의 role·status를 다시 읽어 새 Access를 만들고, `WITHDRAWN`이면 거부한다 |

CSRF: Refresh 쿠키는 `SameSite=Strict` + `Path=/api/auth`라 다른 사이트 요청에 실리지 않는다. 다른 API는 쿠키가 아닌 Bearer 헤더로 인증하므로 CSRF 보호(비활성 상태)를 켜지 않는다.

## 2. 수용 기준 (Acceptance Criteria)

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | ACTIVE 회원이 | 맞는 email(대소문자·공백 무관)·password로 `POST /api/auth/login` 하면 | 200과 `accessToken`·`expiresIn`(초)·`user{id,nickname,level,role}`을 받고, `Set-Cookie: refresh_token=...; HttpOnly; SameSite=Strict; Path=/api/auth; Max-Age=1209600`이 온다. DB `refresh_tokens`에 그 토큰의 SHA-256 해시와 `expires_at`(+14일) 행이 생긴다. 받은 accessToken으로 보호된 API를 호출할 수 있다 |
| AC2 | | 없는 email, 틀린 password, `WITHDRAWN` 회원으로 로그인하면 | 셋 다 401 `AUTH_001`, 같은 메시지 "이메일 또는 비밀번호가 올바르지 않습니다."를 받고 쿠키·DB 행이 생기지 않는다. 빈 email·password는 400 `COMMON_400` |
| AC3 | 유효한 refresh 쿠키가 있을 때 | `POST /api/auth/refresh` 하면 | 200과 새 `accessToken`·`expiresIn`·`user`를 받고, 새 `refresh_token` 쿠키가 온다. 옛 토큰의 DB 행은 지워지고 새 행이 생긴다 |
| AC4 | | 쿠키 없음, DB에 없는 토큰, 만료된 토큰(`expires_at` 지남), 이미 회전된 옛 토큰으로 refresh 하면 | 넷 다 401 `AUTH_002` "로그인이 만료되었습니다. 다시 로그인하세요."를 받고, 응답에 쿠키 삭제(`refresh_token=; Max-Age=0; Path=/api/auth`)가 온다. 만료 토큰의 행은 지운다 |
| AC5 | refresh 쿠키를 가진 회원이 `WITHDRAWN`이 되었을 때 | refresh 하면 | 401 `AUTH_002` + 쿠키 삭제, 해당 행 삭제. role이 바뀐 회원이면 새 accessToken의 role이 DB 값이다 |
| AC6 | 로그인한 상태에서 | `POST /api/auth/logout` 하면 | 204, 그 쿠키의 DB 행 삭제, 쿠키 삭제 헤더. 이후 그 쿠키로 refresh하면 AUTH_002. 쿠키 없이 logout해도 204 (멱등) |
| AC7 | 토큰 없이 | `/api/auth/signup`·`/login`·`/refresh`·`/logout`을 호출하면 인증 필터를 통과하고, 그 밖의 `/api/auth/**` 주소(예: `/api/auth/__probe__`)는 | 401 `AUTH_401`을 받는다 (`PUBLIC_PATHS` 와일드카드 → 명시 나열) |
| AC8 | 비회원이 `/login` 화면에서 | 로그인하면 | 성공 시 store에 accessToken·user가 들어가고 `redirect` 쿼리 또는 홈으로 이동. `AUTH_001`이면 폼 상단에 메시지, 입력 유지. 로그인 상태에서 로그아웃하면 `logout` 호출 후 store를 비우고 홈으로. 앱 시작 시 `restore()`가 refresh로 세션을 복원한다 (새로고침해도 로그인 유지) |
| AC9 | 로그인 상태에서 Access가 만료되어 | API가 401 `AUTH_401`을 주면 | `http.js` 인터셉터가 `/api/auth/refresh`를 **1회** 호출하고 원 요청을 새 토큰으로 재시도한다. 동시에 여러 요청이 401이면 refresh는 한 번만 호출된다. refresh가 실패하거나 401 코드가 `AUTH_401`이 아니거나 `/api/auth/*` 요청 자체가 401이면 재시도하지 않고 store를 비운 뒤 `/login?redirect=<현재 주소>`로 보낸다. `authGuard`의 비로그인 이동 대상도 `login`이다 |

## 3. API 계약

> Swagger Tag: `인증`. 네 API 모두 `@SecurityRequirements()`(공개).

### `POST /api/auth/login`

- 권한: 공개
- 요청

```json
{ "email": "user@example.com", "password": "vibe1234" }
```

- 응답 `200` (+ `Set-Cookie: refresh_token=...`)

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 1800,
  "user": { "id": 1, "nickname": "코딩초보", "level": "BEGINNER", "role": "USER" }
}
```

- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 400 | COMMON_400 | email·password 누락 |
| 401 | AUTH_001 | 이메일 없음, 비밀번호 불일치, 탈퇴 회원. **신규 코드** |

### `POST /api/auth/refresh`

- 권한: 공개 (요청 본문 없음, `refresh_token` 쿠키 사용)
- 응답 `200` (+ 새 `Set-Cookie: refresh_token=...`): login과 같은 본문
- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 401 | AUTH_002 | 쿠키 없음, 모르는 토큰, 만료, 이미 회전됨, 탈퇴 회원. 쿠키 삭제 헤더 포함. **신규 코드** |

### `POST /api/auth/logout`

- 권한: 공개 (쿠키 기준)
- 응답 `204` (+ 쿠키 삭제 헤더), 본문 없음
- 에러: 없음 (쿠키가 없거나 모르는 토큰이어도 204)

### 에러 응답 예

```json
{ "code": "AUTH_001", "message": "이메일 또는 비밀번호가 올바르지 않습니다.", "details": null }
```

## 4. DB 변경

- Flyway: 없음 (V1 `refresh_tokens(id, user_id, token_hash CHAR(64) UNIQUE, expires_at, created_at)` 그대로)
- 사용 테이블: `users` (조회), `refresh_tokens` (INSERT, 해시로 조회, DELETE)
- 패키지: `com.vibenote.auth` — `AuthController`(signup·login·refresh·logout), `AuthService`, `RefreshTokenMapper`(+`mapper/auth/RefreshTokenMapper.xml`), `RefreshTokenCookie`(쿠키 생성·삭제). 회원 조회는 `UserService`를 통해서만 한다 (다른 도메인 Mapper 직접 호출 금지)
- 설정·뼈대 변경:
  - `JwtProperties`에 `refreshTokenValidity`(Duration), `refreshCookieSecure`(boolean) 추가. `application.yml` 14d·true, `application-local.yml` secure false
  - `SecurityConfig.PUBLIC_PATHS`: `/api/auth/**` → 네 주소 명시. `SecurityConfigTest`의 `PUBLIC_AUTH_PROBE`(`/api/auth/__probe__`)는 이제 401이어야 하므로 `인증_API_주소는_토큰_없이_통과한다`를 실제 공개 주소 기준으로 바꾸고 probe는 401 케이스로 옮긴다
  - `ErrorCode`: `USER_409`(F-01), `AUTH_001`, `AUTH_002` 추가
  - `JwtTokenProvider` 클래스 주석의 "F-02에서 정한다" 정리

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | `/login` → `LoginView.vue` (SC-02, 공개, 로그인 상태면 홈으로). "계정이 없나요? 회원가입" 링크(`/signup`) |
| 컴포넌트 | `App.vue` 상단에 로그인 상태 표시: 비로그인 = 로그인·회원가입 링크, 로그인 = 닉네임 + 로그아웃 버튼 (최소 UI) |
| store | `stores/auth.js`: `login({email,password})`, `logout()`, `refresh()`(새 토큰 반환, 동시 호출은 같은 Promise 공유), `restore()`(앱 시작 시 1회, 실패해도 에러 없이 비로그인). 토큰은 메모리에만 둔다 (localStorage 금지) |
| API 함수 | `src/api/auth.js`: `signup()`, `login()`, `refresh()`, `logout()`. refresh·logout은 쿠키를 쓰므로 같은 출처(Vite 프록시 `/api`) 그대로, `withCredentials` 불필요 |
| 인터셉터 | `src/api/http.js`: AC9. 기존 테스트 `401을 받으면 로그인 상태를 지운다`는 AC9 케이스들로 대체 |
| 라우터 | `/login`·`/signup` 추가, `authGuard` 비로그인 대상 `{ name: 'login', query: { redirect } }`. `main.js`에서 `restore()`를 마운트 전에 기다린다 |
| 상태별 UI | 제출 중 버튼 비활성화 / AUTH_001 메시지 / 네트워크 오류는 `errorMessage()` |

## 6. LLM (AI 기능만)

해당 없음.

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1 | `backend/src/test/java/com/vibenote/auth/LoginApiTest#AC1_...` (쿠키 속성·DB 해시·발급 토큰으로 보호 API 통과) | MockMvc 통합 |
| AC2 | `LoginApiTest#AC2_...` (`@ParameterizedTest` 3종 + 400) | MockMvc 통합 |
| AC3 | `RefreshApiTest#AC3_...` | MockMvc 통합 |
| AC4 | `RefreshApiTest#AC4_...` (4종, 만료는 `expires_at`을 과거로 넣은 행) | MockMvc 통합 |
| AC5 | `RefreshApiTest#AC5_...` | MockMvc 통합 |
| AC6 | `LogoutApiTest#AC6_...` | MockMvc 통합 |
| AC7 | `common/SecurityConfigTest` (공개 4개 통과, probe 401) | MockMvc 통합 |
| AC8 | `frontend/src/views/__tests__/LoginView.spec.js`, `src/stores/__tests__/auth.spec.js` (login·logout·restore) | Vitest |
| AC9 | `src/api/__tests__/http.spec.js` (재시도 1회, single-flight, 실패 시 clear+이동, AUTH_401 외 코드·auth 요청은 재시도 안 함), `src/router/__tests__/authGuard.spec.js` | Vitest |

## 8. 범위 밖

- 소셜 로그인, 비밀번호 찾기, 로그인 실패 횟수 제한·잠금, 자동 로그인 체크박스(항상 14일 유지)
- 회전된 토큰 재사용 감지 시 그 회원의 전체 세션 폐기 (재사용 토큰은 행이 없으므로 AC4로 거부만 한다)
- 만료된 `refresh_tokens` 행 정리 배치 (refresh 시 만료 행만 지운다)
- 모든 기기에서 로그아웃, 탈퇴 시 Refresh 일괄 폐기 (F-03)
- 관리자 화면 (F-2x)

## 9. 결정 필요

- 에러 코드 `AUTH_001`(로그인 실패)·`AUTH_002`(Refresh 무효)를 AUTH_401과 나누는 기본안. FE가 `AUTH_401`일 때만 refresh를 시도하므로 무한 재시도를 막는다
- `/api/auth/logout`을 공개로 두는 기본안 (Access가 만료된 상태에서도 로그아웃이 되어야 하므로 쿠키 기준)

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/F-01-auth` (F-01과 공유)
- [ ] Flyway 마이그레이션 (없음)
- [ ] AC별 실패 테스트 작성 (red 확인)
- [ ] BE 구현 (green)
- [ ] FE 구현 (green)
- [ ] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [ ] contract-checker 통과
- [ ] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일)
- [ ] reviewer 지적 사항 반영
- [ ] 산출물 반영 (`06-화면설계서.md` SC-02, `02-유스케이스.md` UC02 흐름, `07-REST-API-명세.md`)
- [ ] PR CI 초록
- [ ] 상태 DONE (PR 머지 후)
