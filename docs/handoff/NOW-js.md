# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-24 07:46 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-20b-team-understanding

---

## 현재 기능

**F-20b** 팀 공동 분석 이해도 (`docs/specs/F-20b-team-understanding.md`, APPROVED 11-24) — `/implement` 마무리 단계

## 진행

- ✅ BE AC1~AC9 (커밋 9d50761): V9 `team_analyses`, `TeamAnalysisController`·`Service`·`Access`·`TeamUnderstandingCalculator`, TEAM_006~010, 탈퇴·내보내기·회원 탈퇴 시 공유 정리. A 영역 `QuizService`·`QuizMapper.findActive`·`Quiz.ownerId`
- ✅ FE AC10~AC13 (커밋 112c183): `TeamAnalysisList`·`TeamAnalysisPicker`, SC-24 `TeamUnderstandingView`(+ `ModuleUnderstandingBars`·`MemberUnderstandingTable`), `QuizView` `?team=`
- ✅ contract-checker 불일치 0 (spec 예시 JSON에 `attemptId` 추가), 산출물(ERD `team_analyses`, 요구사항 F-20 행, 화면 설계서 SC-23·SC-24·SC-08) 반영
- ⏳ ui-verifier 미실행: 8080의 BE가 11-23에 띄운 옛 코드라 재시작 필요 (세션에서 프로세스 종료가 권한 정책에 막힘)

## 바로 다음 할 일

1. 사람이 BE 재시작: 8080 프로세스 종료 → `cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'` (V9 적용)
2. ui-verifier: SC-23(`/teams/{id}` 공유 영역)·SC-24(`/teams/{id}/analyses/{A}`)·SC-08(`?team=`) 1280·375. 계정 `ui-team-owner@`·`ui-team-member@example.test`(vibe1234)로 DONE 분석 공유 후 점검, 스크린샷 `docs/산출물/screens/SC-24-*.png`
3. `/pr-review` (A 영역·공통 변경 → @whans1996 멘션)

## 막힌 점 / 결정 필요

- 없음 (BE 재시작만 사람 손 필요)

## 주의

- 공통·A 영역 변경(`ErrorCode`, conventions 3절, `QuizService`·`QuizMapper`·`Quiz`·`QuizView.vue`, `FlywayMigrationTest` 테이블 목록) → PR에서 @whans1996 멘션
- 테스트 함정: `@Transactional` 테스트에서 MockMvc 요청 사이에 jdbc로 값을 바꾸면 MyBatis 세션 캐시 때문에 같은 SELECT가 옛 값을 줄 수 있음 → 요청 전에 데이터를 준비
- 상대 F-19(분석 soft delete)도 퀴즈 권한 근처 → QuizService 충돌 가능, 먼저 머지된 쪽을 `git merge origin/develop`
