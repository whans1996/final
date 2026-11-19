# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-19 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-07-execution-flow

## 현재 기능

F-07 실행 흐름 — spec APPROVED, **구현·검증 완료(AC1~AC16 green), PR 전**

## 진행

- ✅ BE `analysis/flow/*`, V5, #19(AC12)·#21(AC13), 프롬프트·`flow.json`, openapi.json / FE `AnalysisFlowsTab`·`AnalysisFlowCard`·`MethodBadge`, `?tab=flows`
- ✅ contract-checker(openapi 표기 2건 수정), ui-verifier 문제 0건(1280·375, 생성 중→완료 전환 확인), 06 화면설계서 SC-07 실행 흐름 절·스크린샷
- 결정(11-19): 흐름 ANALYSIS_500 문구 공통 "실행 흐름 생성이 중단되었습니다. 다시 요청해 주세요."

## 바로 다음 할 일

1. `/pr-review` (PR 본문 Closes #19, #21) → Codex 리뷰 확인
2. 머지 전 `docs(F-07): 완료 표시`: spec 상태 DONE, `04-WBS.md` 3.11·`05-간트차트.md` t311 실적
3. 1.4 유스케이스(마감 11/23): `docs/산출물/02-유스케이스.md`에 분석 요청·결과 조회·실행 흐름 UC

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대. 하네스: `codex-review.mjs` 옛 스레드 FINDINGS 판정

## 주의

- 작업로그에 F-06·F-07 작업 항목이 없다 → handoff 때 보충
- Gradle 테스트를 bootRun과 동시에 돌리지 않는다. 통합 테스트는 Docker Desktop 필요. V1~V5 수정 금지
