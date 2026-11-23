# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-24 08:25 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-20c-team-challenge

---

## 현재 기능

**F-20c** 팀 기간 목표 (`docs/specs/F-20c-team-challenge.md`, **APPROVED** 김준섭 11-24). F-20b는 PR #90 머지됨(11-24)

## 진행

- ✅ spec 초안: AC1~AC10 BE(생성·검증·TEAM_006/011/012·목록 정렬·기간 안 최근 풀이·상태 3종·삭제 TEAM_013·CASCADE·401·#89 projectName), AC11~AC13 FE(SC-23 팀 목표 영역, SC-24 목표 진행·팀장 폼, #89 제목)
- ✅ 11-24 사용자 결정 반영: 공유 분석 1개 + 목표 정답률, 지금 멤버 전원 달성, 분석마다 끝나지 않은 목표 1개·팀당 20개, #89 흡수 → 결정로그 한 줄 추가 (승인 커밋에 포함)

## 바로 다음 할 일

1. `/clear` → 새 세션(Opus/high)에서 `/implement F-20c`. 시작 때 `git merge origin/develop`(F-19 PR #93 머지 여부) 후 `node scripts/next-migration.mjs`로 번호 확인(지금 V10)
2. 첫 단계: `V10__team_challenges.sql`(spec 4절 DDL) + `FlywayMigrationTest` 테이블 목록 → `team/TeamChallengeCalculatorTest`·`TeamChallengeApiTest#AC1_…` red부터

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 변경 예정(`ErrorCode` TEAM_011~013, conventions 3절, `FlywayMigrationTest` 테이블 목록) → PR에서 @whans1996 멘션. A 영역 코드 변경은 없음
- `team_challenges(team_id, analysis_id)` → `team_analyses` 복합 FK CASCADE로 해제·탈퇴·팀 삭제 정리 (서비스 코드 추가 없음)
- 테스트 함정: `@Transactional` 테스트에서 MockMvc 요청 사이에 jdbc로 값을 바꾸면 MyBatis 세션 캐시 때문에 같은 SELECT가 옛 값을 줄 수 있음 → 요청 전에 데이터를 준비. 오늘은 `@TestBean Clock`으로 11-24 고정
