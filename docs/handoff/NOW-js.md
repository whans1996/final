# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-24 09:03 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-20c-team-challenge

---

## 현재 기능

**F-20c** 팀 기간 목표 (`docs/specs/F-20c-team-challenge.md`, APPROVED). `/implement` 마무리 단계

## 진행

- ✅ BE AC1~AC10 (070691c): V10 `team_challenges`, `TeamChallengeController·Service·Calculator·Mapper`, TEAM_011~013, #89 `projectName`. BE 전체 904 통과, openapi.json 갱신
- ✅ FE AC11~AC13 (c5650ca): `TeamChallengeList`·`Progress`·`Form`, SC-23·SC-24 연결, 해제 확인 문구, `Closes #89`. FE 전체 통과
- ✅ contract-checker 불일치 0, 산출물(화면 설계서 SC-23·24, ERD, 요구사항 F-20c) 반영

## 바로 다음 할 일

1. ui-verifier: BE(local)·FE dev를 띄우고 API로 시드(회원 2명, 합성 분석 DONE+퀴즈, 팀 공유, 목표 1개 진행 중) → SC-23 `/teams/{T}`(팀 목표 영역), SC-24 `/teams/{T}/analyses/{A}`(목표 진행·팀장 폼) 1280·375, 팀장·팀원
2. 스크린샷을 `docs/산출물/screens/SC-23-team-challenges-*`, `SC-24-team-challenge-*`로 화면 설계서에 추가
3. `/pr-review` (공통 변경 → @whans1996 멘션)

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 변경(`ErrorCode` TEAM_011~013, conventions 3절, `FlywayMigrationTest` 테이블 목록) → PR에서 @whans1996 멘션. A 영역 코드 변경은 없음
- SC-24 팀장 판단은 이해도 응답 `members`의 OWNER userId와 `useAuthStore().user.id` 비교 (추가 요청 없음)
- 남은 날 문구: 진행 중은 `종료일 - 오늘`일(0이면 "오늘 마감"), 예정은 `시작일 - 오늘`로 "D-n"
