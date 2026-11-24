# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-24 09:06 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-20c-team-challenge

---

## 현재 기능

**F-20c** 팀 기간 목표 (`docs/specs/F-20c-team-challenge.md`, APPROVED). `/implement` 마무리 단계

## 진행

- ✅ BE AC1~AC10 (070691c): V10 `team_challenges`, `TeamChallengeController·Service·Calculator·Mapper`, TEAM_011~013, #89 `projectName`. BE 전체 904 통과, openapi.json 갱신
- ✅ FE AC11~AC13 (c5650ca): `TeamChallengeList`·`Progress`·`Form`, SC-23·SC-24 연결, 해제 확인 문구, `Closes #89`. FE 전체 통과
- ✅ contract-checker 불일치 0, 산출물(화면 설계서 SC-23·24, ERD, 요구사항 F-20c) 반영
- ✅ ui-verifier SC-23·SC-24 팀장·팀원 1280·375 문제 0건, 스크린샷 6장(`SC-23-team-challenges-*`, `SC-24-team-challenge(-form)-*`) 추가

## 바로 다음 할 일

1. `/clear` → 새 세션(Opus/high)에서 `/pr-review` (verifier·reviewer·contract-checker → PR 본문에 @whans1996 멘션(ErrorCode·conventions·FlywayMigrationTest 공통 변경), `Closes #89`, 스크린샷은 `node scripts/pr-screens.mjs SC-23 SC-24`)
2. 화면 점검용 서버: BE 8082·FE 5175(이 세션이 띄움, 스크래치패드 `vite5175.mjs`)는 PR 뒤 정리해도 됨. 8080·5173은 옛 코드

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 변경(`ErrorCode` TEAM_011~013, conventions 3절, `FlywayMigrationTest` 테이블 목록) → PR에서 @whans1996 멘션. A 영역 코드 변경은 없음
- SC-24 팀장 판단은 이해도 응답 `members`의 OWNER userId와 `useAuthStore().user.id` 비교 (추가 요청 없음)
- 남은 날 문구: 진행 중은 `종료일 - 오늘`일(0이면 "오늘 마감"), 예정은 `시작일 - 오늘`로 "D-n"
