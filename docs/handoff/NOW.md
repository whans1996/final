# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 09:50 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-01-auth

## 현재 기능

F-01 회원가입 + F-02 로그인·JWT — **한 브랜치·한 PR**. spec APPROVED, /implement 진행 중.

## 진행

- ✅ PR #2(F-00 뼈대) develop 머지. 교육장 PC bootRun 확인: Flyway V1·V2 적용, Swagger 200, 무인증 요청 401 AUTH_401
- ✅ spec F-01(AC7)·F-02(AC9) APPROVED (whans1996 11/18)
- ✅ F-01 BE AC1~AC6 green: `auth/SignupApiTest`, `user/UserServiceTest`, `AuthController#signup`, `UserService.signup()`, `mapper/user/UserMapper.xml`, ErrorCode +USER_409·AUTH_001·AUTH_002
- ✅ 확정: Refresh HttpOnly 쿠키 `refresh_token`(SameSite=Strict, Path=/api/auth) 14일·회전, Access FE 메모리, 앱 시작 시 refresh로 복원, 가입 후 자동 로그인, 검증 규칙 (conventions 3·4절, 결정 로그 반영)

## 바로 다음 할 일

1. F-02 BE: 이어서 `LoginApiTest`·`RefreshApiTest`·`LogoutApiTest`, `JwtProperties`(+refreshTokenValidity, refreshCookieSecure), `SecurityConfig.PUBLIC_PATHS` 명시 나열 + `SecurityConfigTest` 수정 (ErrorCode AUTH_001·AUTH_002는 이미 추가됨)
2. FE (F-01 AC7, F-02 AC8·9): `src/api/auth.js`, `stores/auth.js`(login·logout·refresh·restore), `http.js` 인터셉터(AC9), `LoginView`·`SignupView`, `authGuard` → login

## 막힌 점 / 결정 필요

- CI가 push·PR 이벤트로 자동 실행되지 않음 (workflow active, 권한 all, PR·push 실행 0건). Settings → Actions, PR close/reopen 확인 (사람). 그때까지 `gh workflow run CI --ref <브랜치>`
- WBS 3.1(11/20~11/26)이 W1 마감(11/23)을 넘음 → F-01·F-02 W1, F-03 W2 초로 조정 제안 (사람)
- 팀원 GitHub 아이디(리뷰어 지정), 제출 zip 이름용 지역·반·이름 (사람)

## 주의

- 다른 PC는 pull 후 `git ls-files --eol | grep w/crlf`로 CRLF 파일 확인 (`.gitattributes` eol=lf 이후)
- verify(Gradle)를 동시에 두 개 돌리지 않는다 (Windows 파일 잠금). 커밋 hook도 Gradle을 돌린다
- V1·V2 수정 금지. 이번 기능은 마이그레이션 없음. 회원은 soft delete (`users.status`)
