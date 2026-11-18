# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 10:05 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-01-auth

## 현재 기능

F-01 회원가입 + F-02 로그인·JWT — **PR #3** (https://github.com/whans1996/final/pull/3) 열림, 머지 대기

## 진행

- ✅ 전체 AC green (BE 84, FE 53). contract-checker 0건, ui-verifier 이상 없음, reviewer 반드시 수정 2건·권장 5건 반영
- ✅ Codex PR 리뷰 P2 반영(`3e3dd7d`): 재발급 중 로그아웃하면 세션이 되살아나던 문제 → logout이 진행 중 refresh를 기다린 뒤 서버 logout, 로그아웃 중 refresh 결과 버림
- ✅ CI가 PR 이벤트로 자동 실행됨 (Actions에서 CI workflow Disable/Enable + PR close/reopen으로 해결). 반영 전 커밋 기준 CI 초록
- ✅ 하네스: `/pr-review` 8단계에 Codex 자동 리뷰 확인 추가 (`270667e`)

## 바로 다음 할 일

1. push 후 PR #3 CI(`gh pr checks 3`) 확인. Codex 재리뷰가 필요하면 사용자 확인 후 `gh pr comment 3 --body "@codex review"`
2. 사람이 머지하면 spec F-01·F-02 상태 DONE, `/spec F-03`(프로필·탈퇴: 탈퇴 시 이메일 마스킹 규칙, `refresh_tokens` user_id 일괄 삭제)

## 막힌 점 / 결정 필요

- WBS 3.1(11/20~11/26)이 W1 마감(11/23)을 넘음 → F-03을 W2 초로 조정 제안 (사람)
- 팀원 GitHub 아이디(리뷰어 지정), 제출 zip 이름용 지역·반·이름 (사람)

## 주의

- 로컬 실행: refresh 쿠키는 `local` 프로필에서만 Secure=false (http). 운영·기본은 Secure=true
- 브라우저 콘솔의 첫 `POST /api/auth/refresh` 401은 비로그인 세션 복원이라 정상
- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금). 다른 PC는 pull 후 `git ls-files --eol | grep w/crlf` 확인
- V1·V2 수정 금지. 이번 기능은 마이그레이션 없음
