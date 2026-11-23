# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-24 07:38 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-20b-team-understanding

---

## 현재 기능

**F-20b** 팀 공동 분석 이해도 (`docs/specs/F-20b-team-understanding.md`, APPROVED 11-24) — `/implement` 진행 중

## 진행

- ✅ BE AC1~AC9 green: V9 `team_analyses`, `TeamAnalysisController`·`Service`·`Access`·`TeamUnderstandingCalculator`, DTO 8개, `ErrorCode` TEAM_006~010, `TeamService` leave·removeMember·removeAllOf 공유 정리
- ✅ A 영역 변경: `QuizService.getQuiz`(공유면 통과)·`submit`(`QuizMapper.findActive` + 주인/공유), `Quiz.ownerId`
- ✅ 테스트: `team/TeamAnalysisApiTest`(AC1~4,6~9), `quiz/QuizTeamAccessApiTest`(AC5), `team/TeamUnderstandingCalculatorTest`
- ⏳ BE 전체 verify·openapi.json 재생성 → 커밋 `feat(F-20b): BE AC1~AC9 ...`

## 바로 다음 할 일

1. BE 커밋·push (openapi.json 포함)
2. FE AC10~AC13: `src/api/teams.js`에 `fetchTeamAnalyses`·`shareTeamAnalysis`·`unshareTeamAnalysis`·`fetchTeamUnderstanding` → `components/team/TeamAnalysisList.vue`·`TeamAnalysisPicker.vue` → `TeamDetailView.vue` 영역 → `views/team/TeamUnderstandingView.vue`(+ `ModuleUnderstandingBars`·`MemberUnderstandingTable`, 라우트 `routes/team.js`) → `QuizView.vue` `?team=` 링크. 각 spec 파일 먼저(red)
3. contract-checker, ui-verifier(1280·375), 산출물(SC-23·SC-24·SC-08, ERD, 요구사항 정의서)

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통·A 영역 변경(`ErrorCode`, conventions 3절, `QuizService`·`QuizMapper`·`Quiz`·`QuizView.vue`) → PR에서 @whans1996 멘션
- 테스트 함정: `@Transactional` 테스트에서 MockMvc 요청 사이에 jdbc로 값을 바꾸면 MyBatis 세션 캐시 때문에 같은 SELECT가 옛 값을 줄 수 있음 → 요청 전에 데이터를 준비
- 상대 F-19(분석 soft delete)도 퀴즈 권한 근처 → QuizService 충돌 가능, 먼저 머지된 쪽을 `git merge origin/develop`
- 로컬 DB 점검용 계정 `ui-team-owner@`·`ui-team-member@example.test`(비번 vibe1234)·`ui-admin@`
