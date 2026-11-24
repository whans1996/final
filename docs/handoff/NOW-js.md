# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-24 09:25 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-20c-team-challenge

---

## 현재 기능

**F-20c** 팀 기간 목표 (`docs/specs/F-20c-team-challenge.md`, DONE). **PR #98** 열림, Codex ✅ 53c1047, spec DONE → CI 초록 확인 뒤 머지

## 진행

- ✅ BE AC1~AC10, FE AC11~AC13, 산출물·스크린샷 6장 (이전 커밋들)
- ✅ `/pr-review`: verifier 전체 통과(BE 904, FE 625), contract-checker 불일치 0, reviewer 반드시 수정 0
- ✅ 2629b53 reviewer 권장 1: SC-24 만든 뒤 `creating`이 풀리지 않던 버그 → `routeVersion` 카운터 (`TeamUnderstandingView.vue` `onCreate`)
- ✅ 53c1047 날짜 Swagger 설명 경계 포함, DTO에 타입 오류 예외 주석(#96)
- ✅ 후속 이슈 #95(날짜 상한), #96(숫자 타입 오류 403<400) 생성
- ✅ PR #98 Codex ✅ 53c1047 (지적 없음, 재리뷰 1회), 완료 표시 커밋(spec DONE)

## 바로 다음 할 일

1. `gh pr checks 98` 전부 초록이면 GitHub에서 머지 (wip 없음, 일반 머지 가능). 머지 뒤 #89 닫힘 확인
2. 머지 전 F-24 PR #97이 먼저 머지되면 `git merge origin/develop` → push → CI 재확인 (마이그레이션 번호·openapi.json·ErrorCode 충돌 주의)
3. 화면 점검용 서버 BE 8082·FE 5175는 정리해도 됨

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 변경(`ErrorCode` TEAM_011~013, V10, `FlywayMigrationTest`, conventions 3절) → PR에서 @whans1996 멘션 완료
