# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 10:00 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-01-auth

## 현재 기능

F-01 회원가입 + F-02 로그인·JWT — 한 브랜치·한 PR. **/pr-review 완료, PR 생성 대기** (설명: `.harness/pr-F-01.md`, gitignore라 이 PC에만 있음)

## 진행

- ✅ 전체 AC green: BE 84, FE 51. contract-checker 불일치 0건, ui-verifier `/`·`/login`·`/signup`·`/login?joined=1` 이상 없음
- ✅ reviewer 반드시 수정 2건: 비밀번호 ASCII 제한(BCrypt 72바이트 → 한글 가입 500, `d7a43f9`), 동시 refresh 둘 다 성공 → `deleteByHash==0`이면 AUTH_002(`ab2cf18`)
- ✅ reviewer 권장 반영(사용자 결정): 모르는·회전된 토큰 401엔 쿠키 삭제 헤더 없음(여러 탭 복원 보호), 가입 후 자동 로그인 실패 → `/login?joined=1` 안내, `noRollbackFor` 실검증 테스트(NOT_SUPPORTED), AC9 비-AUTH_401 테스트, `redirectTarget` `/\evil.com` 차단(`9af97b0`)

## 바로 다음 할 일

1. 사용자 확인 후 `gh pr create --base develop --title "feat(F-01): 회원가입·로그인·JWT (F-01, F-02)" --body-file .harness/pr-F-01.md` → `gh workflow run CI --ref feature/F-01-auth` → `gh pr checks`
2. 머지 후 spec F-01·F-02 상태 DONE, `/spec F-03`(프로필·탈퇴: 탈퇴 시 이메일 마스킹 규칙, `refresh_tokens` user_id 일괄 삭제)

## 막힌 점 / 결정 필요

- CI가 push·PR 이벤트로 자동 실행되지 않음 (workflow active, 권한 all, 실행 0건). Settings → Actions, PR close/reopen 확인 (사람)
- WBS 3.1(11/20~11/26)이 W1 마감(11/23)을 넘음 → F-03을 W2 초로 조정 제안 (사람)
- 팀원 GitHub 아이디(리뷰어 지정), 제출 zip 이름용 지역·반·이름 (사람)

## 주의

- 로컬 실행: refresh 쿠키는 `local` 프로필에서만 Secure=false (http). 운영·기본은 Secure=true
- 브라우저 콘솔의 첫 `POST /api/auth/refresh` 401은 비로그인 세션 복원이라 정상. ui-check 첫 실행에 `ERR_ABORTED` 1건이 났다가 재실행에선 재현 안 됨(Vite 첫 로드 추정)
- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금). 다른 PC는 pull 후 `git ls-files --eol | grep w/crlf` 확인
- V1·V2 수정 금지. 이번 기능은 마이그레이션 없음
