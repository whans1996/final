# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 13:20 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-03-profile

## 현재 기능

F-03 프로필·탈퇴 — **PR #4 머지 대기** (CI 초록 659ffa1, reviewer·Codex 지적 전부 반영). spec APPROVED, 체크리스트는 "PR CI 초록·DONE"만 남음

## 진행

- ✅ AC1~AC10 구현 (BE 120, FE 76 green). contract-checker 불일치 0, ui-verifier 문제 0 (스크린샷 `.harness/screens/f03_*.png`)
- ✅ reviewer 권장 6건 반영 (a0539ff): 탈퇴 한 트랜잭션 롤백 테스트, 수정 UPDATE ACTIVE 조건, refresh INSERT…SELECT ACTIVE, 프로필 화면 경합, 테스트 보강
- ✅ Codex P2 3건 반영: 보낸 필드만 UPDATE·재발급 users FOR SHARE 먼저(0e73a48), 탈퇴 뒤 늦은 refresh 응답 무시(sessionEpoch, 659ffa1)
- 이번 기능에서 F-02 코드도 바뀜: `AuthService.refresh` 잠금 순서, `RefreshTokenMapper.insert` 조건부 (Login·Refresh 테스트 통과)

## 바로 다음 할 일

1. (사람) PR #4에 스크린샷 5장 첨부 → 영역 오너가 GitHub에서 머지 (`gh pr merge` 금지). 원하면 `@codex review`로 659ffa1 재리뷰
2. 머지 후: `git switch develop && git pull` → `docs/specs/F-03-profile.md` 상태 DONE·체크리스트 마지막 2칸 → 작업로그 → `/progress`로 다음 기능(W1: 시드 카드·LlmClient 확인, W2: F-05·F-06·F-09·F-10) 선택

## 막힌 점 / 결정 필요

- WBS 3.1 일정 조정(F-03은 11/18 완료로 W1 안), 팀원 GitHub 아이디(리뷰어 지정), 제출 zip 이름용 지역·반·이름 (사람)

## 주의

- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금). bootRun은 TaskStop 후에도 java.exe가 남을 수 있어 8080 PID를 확인해 종료
- 셸(heredoc·node -e)로 Markdown을 쓸 때 백틱이 명령 치환된다 → 문서 수정은 Edit 도구로
- V1·V2 수정 금지. 이번 기능은 마이그레이션 없음
