# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 10:40 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-01-auth

## 현재 기능

F-01 회원가입 + F-02 로그인·JWT — 한 브랜치·한 PR. **모든 AC green**, /pr-review 전.

## 진행

- ✅ BE: F-01 AC1~6 (`auth/SignupApiTest`, `user/UserServiceTest`), F-02 AC1~7 (`LoginApiTest`·`RefreshApiTest`·`LogoutApiTest`·`SecurityConfigTest` F02_AC7). 전체 BE 테스트 green
- ✅ FE: F-01 AC7 (`SignupView.spec`), F-02 AC8·9 (`LoginView.spec`, `stores/auth.spec`, `api/http.spec`, `api/auth.spec`, `authGuard.spec`). lint·test·build green
- ✅ contract-checker 불일치 0건(참고 3건 반영), ui-verifier `/`·`/login`·`/signup` 데스크톱·모바일 이상 없음, 실제 서버 curl로 가입→로그인 쿠키→재발급→로그아웃→AUTH_002 확인
- ✅ 산출물: 02 UC01·UC02 흐름, 06 SC-02·SC-03. 하네스: ui-check가 비로그인 refresh 401을 세지 않음

## 바로 다음 할 일

1. `/pr-review` (verifier·reviewer·contract-checker) → 지적 반영 → PR `feature/F-01-auth` → develop. CI는 `gh workflow run CI --ref feature/F-01-auth`로 수동 실행
2. 머지 후 spec F-01·F-02 상태 DONE, `/spec F-03`(프로필·탈퇴: 탈퇴 시 이메일 마스킹 규칙, Refresh 일괄 폐기 `refresh_tokens` user_id 삭제)

## 막힌 점 / 결정 필요

- CI가 push·PR 이벤트로 자동 실행되지 않음 (workflow active, 권한 all, 실행 0건). Settings → Actions, PR close/reopen 확인 (사람)
- WBS 3.1(11/20~11/26)이 W1 마감(11/23)을 넘음 → F-03을 W2 초로 조정 제안 (사람)
- 팀원 GitHub 아이디(리뷰어 지정), 제출 zip 이름용 지역·반·이름 (사람)

## 주의

- 로컬 실행: refresh 쿠키는 `local` 프로필에서만 Secure=false (http). 운영·기본은 Secure=true
- 브라우저 콘솔의 첫 `POST /api/auth/refresh` 401은 비로그인 세션 복원이라 정상
- verify(Gradle)를 동시에 두 개 돌리지 않는다 (Windows 파일 잠금). 다른 PC는 pull 후 `git ls-files --eol | grep w/crlf` 확인
- V1·V2 수정 금지. 이번 기능은 마이그레이션 없음
