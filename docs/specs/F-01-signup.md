# F-01 회원가입

| 항목 | 값 |
|---|---|
| 상태 | DRAFT <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-01 |
| 우선순위 | P0 |
| 영역 오너 | 팀원 B (학습·소셜) |
| 관련 필수 요구사항 | ② (명세서 F1306~F1310 회원 가입) |
| 의존 spec | 없음. **F-02와 같은 브랜치·PR(`feature/F-01-auth`)로 구현** (가입 후 자동 로그인) |
| 승인 | (승인한 사람, 날짜) |

## 1. 목적

AI로 코딩하는 입문자·주니어가 이메일·비밀번호·닉네임과 **자기 학습 수준(입문/주니어/숙련)** 을 골라 가입한다.
수준은 이후 분석 설명의 기본 난이도가 된다. 가입 직후 별도 안내 없이 로그인 상태로 서비스를 쓸 수 있어야 한다 (NF-01).

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_정상_요청이면_201을_반환한다()`

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 비회원이 | 유효한 email·password·nickname·level로 `POST /api/auth/signup` 하면 | 201과 `{ "id": n }`을 받는다. DB에는 email이 trim·소문자로, password는 BCrypt 해시(원문 아님)로, role=`USER`, status=`ACTIVE`로 저장된다 |
| AC2 | 비회원이 | 형식이 틀린 email, 규칙을 어긴 password·nickname, 없거나 허용되지 않은 level로 가입하면 | 400 `COMMON_400`과 `details`에 문제 필드별 사유를 받는다 (여러 필드가 틀리면 모두) |
| AC3 | `user@example.com`이 이미 가입되어 있을 때 | `" User@Example.com "`으로 가입하면 | 409 `USER_409`, `details: { "email": "이미 사용 중인 이메일입니다." }` |
| AC4 | 닉네임 `코딩초보`가 이미 있을 때 | 같은 닉네임으로 가입하면 | 409 `USER_409`, `details: { "nickname": "이미 사용 중인 닉네임입니다." }` |
| AC5 | 비회원이 | 경계값으로 가입하면 | 비밀번호 8자·64자, 닉네임 2자·20자는 201. 비밀번호 7자·65자, 영문만·숫자만, 닉네임 1자·21자·공백/특수문자 포함은 400 |
| AC6 | 서비스의 중복 확인을 통과했지만 | 저장 시점에 DB UNIQUE 위반(`DuplicateKeyException`, 동시 가입)이 나면 | 500이 아니라 409 `USER_409`를 받는다 |
| AC7 | 비회원이 `/signup` 화면에서 | 값을 입력하고 가입하면 | 클라이언트 검증 실패는 요청 없이 필드 아래에 표시된다. 서버의 `COMMON_400`/`USER_409` `details`는 해당 필드 아래에 표시된다. 성공하면 같은 자격 증명으로 로그인(F-02 `login`)한 뒤 `redirect` 쿼리 또는 홈(`/`)으로 이동한다 |

### 검증 규칙

| 필드 | 규칙 | 에러 메시지 (`details` 값) |
|---|---|---|
| email | 필수, 이메일 형식(`@Email`), 정규화(trim·소문자) 후 ≤255자 | "올바른 이메일 형식이 아닙니다." |
| password | 필수, 8~64자, 영문 1자 이상 + 숫자 1자 이상 | "비밀번호는 8~64자이며 영문과 숫자를 모두 포함해야 합니다." |
| nickname | 필수, trim 후 2~20자, `^[가-힣A-Za-z0-9_]+$` | "닉네임은 2~20자의 한글·영문·숫자·_만 사용할 수 있습니다." |
| level | 필수, `BEGINNER`·`JUNIOR`·`SENIOR` 중 하나 | "학습 수준을 선택하세요." |

- 이메일 중복은 정규화한 값으로 확인한다 (`utf8mb4_0900_ai_ci`라 DB UNIQUE도 대소문자 무시).
- 닉네임 중복은 대소문자를 구분하지 않는다 (같은 collation).

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다 (contract-checker, CI가 검사). Swagger Tag: `인증`.

### `POST /api/auth/signup`

- 권한: 공개 (`SecurityConfig.PUBLIC_PATHS`에 명시 나열, F-02 AC7)
- 요청

```json
{
  "email": "user@example.com",
  "password": "vibe1234",
  "nickname": "코딩초보",
  "level": "BEGINNER"
}
```

- 응답 `201`

```json
{ "id": 1 }
```

- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 400 | COMMON_400 | 검증 실패 (`details`: 필드별 사유) |
| 409 | USER_409 | 이메일 또는 닉네임 중복 (`details`: `email`·`nickname` 중 해당 필드). **신규 코드** |

```json
{ "code": "USER_409", "message": "이미 사용 중인 정보가 있습니다.", "details": { "email": "이미 사용 중인 이메일입니다." } }
```

## 4. DB 변경

- Flyway: 없음 (V1 `users` 그대로 사용)
- 사용 테이블: `users` (INSERT, email·nickname 존재 확인)
- 패키지: `com.vibenote.user` — `UserController`가 아니라 **`AuthController`(F-02와 공유, `com.vibenote.auth`)가 signup을 받고 `UserService.signup()`을 호출**한다. `UserMapper.xml`(`mapper/user/`)에 `insert`, `existsByEmail`, `existsByNickname`, `findByEmail`(F-02에서 사용)

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | `/signup` → `SignupView.vue` (SC-03, 공개, 로그인 상태면 홈으로) |
| 컴포넌트 | 수준 선택은 라디오 카드 3개 (입문 BEGINNER / 주니어 JUNIOR / 숙련 SENIOR, 한 줄 설명 포함). 하단에 "이미 계정이 있나요? 로그인" 링크(`/login`) |
| store | `useAuthStore().login()` (F-02)을 가입 성공 후 호출 |
| API 함수 | `src/api/auth.js`의 `signup({ email, password, nickname, level })` |
| 상태별 UI | 제출 중 버튼 비활성화·로딩 표시 / 필드별 에러 / 그 밖의 에러는 `errorMessage()`로 폼 상단 |

## 6. LLM (AI 기능만)

해당 없음.

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1 | `backend/src/test/java/com/vibenote/auth/SignupApiTest#AC1_...` | MockMvc 통합 (DB 행·BCrypt `matches` 확인) |
| AC2 | `SignupApiTest#AC2_...` (`@ParameterizedTest`) | MockMvc 통합 |
| AC3 | `SignupApiTest#AC3_...` | MockMvc 통합 |
| AC4 | `SignupApiTest#AC4_...` | MockMvc 통합 |
| AC5 | `SignupApiTest#AC5_...` (`@ParameterizedTest`) | MockMvc 통합 |
| AC6 | `UserServiceTest#AC6_...` (Mapper가 `DuplicateKeyException`을 던지도록 mock) | 단위 |
| AC7 | `frontend/src/views/__tests__/SignupView.spec.js` (`vi.mock('@/api/auth')`) | Vitest |

## 8. 범위 밖

- 이메일 인증, 비밀번호 찾기·재설정, 소셜 로그인, 약관 동의
- 닉네임·이메일 실시간 중복 확인 API (제출 시 409로 충분)
- 프로필 수정·탈퇴, 탈퇴한 이메일 재가입 정책 (F-03: 탈퇴 시 마스킹 규칙과 함께 정한다)

## 9. 결정 필요

- 에러 코드 `USER_409` 하나에 `details`로 필드를 구분하는 방식 (대안: `USER_001` 이메일, `USER_002` 닉네임). 기본안은 COMMON_400과 같은 `details` 방식
- 반대 의견: WBS `3.1 회원가입·로그인·프로필·탈퇴`(11/20~11/26)가 W1 마감(11/23)을 넘는다. F-01·F-02는 W1 안(~11/21)에 끝내고 F-03은 W2 초로 옮기는 WBS 조정을 제안 (사람 결정)

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/F-01-auth` (F-02와 공유)
- [ ] Flyway 마이그레이션 (없음)
- [ ] AC별 실패 테스트 작성 (red 확인)
- [ ] BE 구현 (green)
- [ ] FE 구현 (green)
- [ ] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [ ] contract-checker 통과
- [ ] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일)
- [ ] reviewer 지적 사항 반영
- [ ] 산출물 반영 (`06-화면설계서.md` SC-03, `02-유스케이스.md` UC01 흐름)
- [ ] PR CI 초록
- [ ] 상태 DONE (PR 머지 후)
