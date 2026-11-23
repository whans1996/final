# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-24 08:55 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-20c-team-challenge

---

## 현재 기능

**F-20c** 팀 기간 목표 (`docs/specs/F-20c-team-challenge.md`, APPROVED). `/implement` 진행 중 (BE 먼저)

## 진행

- ✅ develop 합침(F-19 PR #93 포함), V10 번호 확인. `V10__team_challenges.sql`, `FlywayMigrationTest` 목록, `ErrorCode` TEAM_011~013
- ✅ BE red 확인(미커밋): `TeamChallengeCalculatorTest`(5), `TeamChallengeApiTest`(AC1~AC9, 22), `TeamAnalysisApiTest#AC10_…projectName`. DTO·도메인·`TeamChallengeMapper`(+xml)·`TeamChallengeController`는 작성, `TeamChallengeCalculator`·`TeamChallengeService`는 스텁(UnsupportedOperationException)
- ⏳ green 구현 중

## 바로 다음 할 일

1. `TeamChallengeCalculator.status/achieved/evaluate` 구현, `TeamChallengeService.create/list/delete` 구현 (판단 순서 401→TEAM_404→TEAM_403→400 검증(날짜는 String으로 받아 Service에서 파싱)→TEAM_006→TEAM_011→TEAM_012, 정렬 진행 중 end ASC→예정 start ASC→종료 end DESC→id DESC)
2. AC10: `TeamUnderstandingResponse`에 `projectName`(title 다음) 추가, `TeamAnalysisService.understanding`에서 `row.getProjectName()` 전달
3. verifier로 위 4개 테스트 클래스 green → BE 전체 → 커밋 `feat(F-20c): AC1~AC10 팀 목표 API` (openapi.json 포함) → push → FE AC11~AC13

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 변경(`ErrorCode` TEAM_011~013, conventions 3절, `FlywayMigrationTest` 테이블 목록) → PR에서 @whans1996 멘션. A 영역 코드 변경은 없음
- `team_challenges(team_id, analysis_id)` → `team_analyses` 복합 FK CASCADE로 해제·탈퇴·팀 삭제 정리 (서비스 코드 추가 없음)
- 테스트 함정: MockMvc 요청 사이에 jdbc로 값을 바꾸면 MyBatis 세션 캐시로 옛 값이 나올 수 있음 → 요청 전에 데이터 준비. `@TestBean Clock`으로 11-24 고정
