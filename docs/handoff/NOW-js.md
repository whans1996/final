# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-24 08:07 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-20b-team-understanding

---

## 현재 기능

**F-20b** 팀 공동 분석 이해도 (`docs/specs/F-20b-team-understanding.md`, APPROVED 11-24) — **PR #90** CI 초록·Codex ✅ a01fd24, spec DONE 표시(6a504e5). 머지 대기

## 진행

- ✅ BE AC1~AC9 (커밋 9d50761): V9 `team_analyses`, `TeamAnalysisController`·`Service`·`Access`·`TeamUnderstandingCalculator`, TEAM_006~010, 탈퇴·내보내기·회원 탈퇴 시 공유 정리. A 영역 `QuizService`·`QuizMapper.findActive`·`Quiz.ownerId`
- ✅ FE AC10~AC13 (커밋 112c183): `TeamAnalysisList`·`TeamAnalysisPicker`, SC-24 `TeamUnderstandingView`(+ `ModuleUnderstandingBars`·`MemberUnderstandingTable`), `QuizView` `?team=`
- ✅ contract-checker 불일치 0 (spec 예시 JSON에 `attemptId` 추가), 산출물(ERD `team_analyses`, 요구사항 F-20 행, 화면 설계서 SC-23·SC-24·SC-08) 반영
- ✅ reviewer 반드시 수정 0, 권장 3건 반영(659bd11)·1건 #89. PR #90 생성
- ✅ 11-24 시드 데이터로 ui-verifier SC-23·SC-24·SC-08 `?team=` 1280·375 통과, SC-24 스크린샷 화면 설계서 반영

## 바로 다음 할 일

1. 사람이 GitHub에서 PR #90 머지 (CI·Codex 확인). 상대 F-19가 먼저 머지됐으면 `git merge origin/develop` → push → CI 재확인
2. 머지 뒤 `git switch develop && git pull`, #89(SC-24 제목 없을 때 projectName 대체) 처리는 `feature/issue-89-*` 또는 F-20c에 흡수
3. 다음 기능: F-20c(팀 기간 목표) `/spec` — 새 세션, Opus/high

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통·A 영역 변경(`ErrorCode`, conventions 3절, `QuizService`·`QuizMapper`·`Quiz`·`QuizView.vue`, `FlywayMigrationTest` 테이블 목록) → PR에서 @whans1996 멘션
- 테스트 함정: `@Transactional` 테스트에서 MockMvc 요청 사이에 jdbc로 값을 바꾸면 MyBatis 세션 캐시 때문에 같은 SELECT가 옛 값을 줄 수 있음 → 요청 전에 데이터를 준비
- 상대 F-19(분석 soft delete)도 퀴즈 권한 근처 → QuizService 충돌 가능, 먼저 머지된 쪽을 `git merge origin/develop`
