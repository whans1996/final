# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 12:50 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-03-profile

## 현재 기능

F-03 프로필·탈퇴 — **AC1~AC10 구현·push 완료, /pr-review 중 reviewer 권장 6건 반영 중** (PR 아직 안 엶)

## 진행

- ✅ 커밋 f913ad4(AC1~5), d3a7f9c(AC6~8), 0092568(AC9·10 FE), bfcbc48(점검 체크). contract-checker 불일치 0, ui-verifier 문제 0
- ✅ reviewer: 반드시 수정 0, 권장 6건 → 사람이 전부 반영 결정. 테스트 먼저 작성, red 확인 완료(BE 4·FE 2, 예상과 일치)
- ✅ 권장 반영 구현·커밋 (verify all: BE 116, FE 74 green):
  - BE `UserMapper.xml updateProfile` AND status='ACTIVE' + `UserService.updateMe` 0행이면 AUTH_401
  - BE `RefreshTokenMapper.xml insert` → INSERT…SELECT FROM users WHERE ACTIVE, `AuthService.issue(user, failure)` 0행이면 login AUTH_001 / refresh AUTH_002
  - FE `UserProfileView.vue` load(): 요청 번호(lastRequest)·정수 id 가드
  - 테스트: `WithdrawalTransactionTest`(롤백), `UserStatsApiTest`(75%, total 0), `UserServiceTest#AC8_...`, `AuthServiceTest#F03_AC8_` 2개, `WithdrawalApiTest#AC8_탈퇴한_회원에게는_refresh...`, FE `UserProfileView.spec`(A→B→A, NaN), `MySettingsView.spec`(logout 미호출, 에러 2개)

## 바로 다음 할 일

1. PR 설명 초안 작성됨 `.harness/pr-F-03.md` → 사용자 확인 → `gh pr create --base develop --title "feat(F-03): 프로필 조회·수정·학습 통계·회원 탈퇴" --body-file .harness/pr-F-03.md`
2. 그다음 `.harness/pr-F-03.md`(템플릿 `.github/pull_request_template.md`, 예시 `.harness/pr-F-01.md`) → 사용자 확인 → `gh pr create --base develop` → `gh pr checks` → Codex 리뷰 확인

## 막힌 점 / 결정 필요

- WBS 3.1 일정, 팀원 GitHub 아이디(리뷰어 지정), 제출 zip 이름 (사람)

## 주의

- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금). bootRun은 TaskStop 후에도 java.exe가 남을 수 있어 8080 PID를 확인해 종료
- V1·V2 수정 금지. 이번 기능은 마이그레이션 없음
