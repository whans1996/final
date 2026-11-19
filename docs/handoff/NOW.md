# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-19 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-07-execution-flow

## 현재 기능

F-07 실행 흐름 설명 — `docs/specs/F-07-execution-flow.md` APPROVED, **BE·FE 구현 완료(AC1~AC16 green), 검증·산출물 남음**

## 진행

- ✅ V5, AC12(#19)·AC13(#21), AC2~AC5 순수 로직(`FlowContextBuilder`·`FlowResponseParser`), 프롬프트 `prompts/flow/*`
- ✅ FE: `AnalysisFlowsTab`·`AnalysisFlowCard`·`MethodBadge`(개요 탭과 공용), 상세 `?tab=flows`, `usePolling.start({ delay })`
- ✅ `flow/` FlowService(접수·캐시·개수·FOR UPDATE)·FlowLauncher(AFTER_COMMIT→analysisExecutor, 거부→FAILED)·FlowGenerator·FlowController(`/api/analyses/{id}/flows` POST 202·GET)·FlowMapper(.xml), `AnalysisRecovery`가 PENDING 흐름도 정리, `llm-fixtures/flow.json`, openapi.json
- 결정(11-19): 흐름 `ANALYSIS_500` 문구는 공통 "실행 흐름 생성이 중단되었습니다. 다시 요청해 주세요." (spec AC11·결정 로그 반영)
- MyBatis 배열 javaType은 `[Lcom...FlowStep;` (`FlowStep[]`은 못 읽음, backend/CLAUDE.md·JsonTypeHandler 주석 수정)

## 바로 다음 할 일

1. contract-checker, ui-verifier(BE `bootRun --spring.profiles.active=local` + FE `npm run dev`, `/analyses/{id}?tab=flows` 데스크톱·375px)
2. `docs/산출물/06-화면설계서.md` SC-07 실행 흐름 탭, `04-WBS.md`·`05-간트차트.md` F-07 실적 → `/pr-review` (본문 Closes #19, #21)
3. 1.4 유스케이스(마감 11/23): `docs/산출물/02-유스케이스.md`에 분석 요청·결과 조회·실행 흐름 UC

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대. 하네스: `codex-review.mjs` 옛 스레드 FINDINGS 판정

## 주의

- 작업로그에 F-06 작업(11-18 19:50 이후) 항목이 없다 → 다음 handoff 때 보충
- 통합 테스트는 Docker Desktop이 켜져 있어야 한다. V1~V5 수정 금지(V5 커밋 후)
