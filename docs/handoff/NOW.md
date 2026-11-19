# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-19 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-07-execution-flow

## 현재 기능

F-07 실행 흐름 설명 — `docs/specs/F-07-execution-flow.md` APPROVED, **구현 중 (BE 순수 로직 완료, 서비스·생성기 전)**

## 진행

- ✅ V5 `analysis_flows`(status·summary·error_code·updated_at)
- ✅ AC12(#19) `FileSelector.layer(path, content)` + `pipeline/JavaComments`(주석 제거 공용), AC13(#21) `ApiDetector` 리터럴·배열만 + 190자 상한
- ✅ AC2·AC3 `flow/FlowContextBuilder`(inputFiles·render), AC4·AC5(파서) `flow/FlowResponseParser`·`FlowStep`·`FlowResult`, 프롬프트 `prompts/flow/*.md`
- `explain/LlmJsonCall`·`ExplainPrompts.fileBlock/truncate`를 public으로 (flow 패키지 재사용)

## 바로 다음 할 일

1. `flow/FlowMapper`(+`mapper/analysis/FlowMapper.xml`): 분석 `FOR UPDATE` 잠금, 흐름 INSERT·리셋·조회·완료·실패, PENDING 일괄 FAILED
2. `FlowService`(AC6~AC9·AC11 거부), `FlowRequestedEvent`+`FlowLauncher`(AFTER_COMMIT→`analysisExecutor`), `FlowGenerator`(AC1·AC5), `AnalysisRecovery`에 흐름 정리(AC11)
3. `ErrorCode` ANALYSIS_002·003·409, 엔드포인트(`AnalysisFlowControllerTest` AC8~AC10), `llm-fixtures/flow.json`(DS-02-01 dev만), application.yml `analysis.flow.*`
4. FE 탭(AC14~AC16), 산출물·openapi.json

## 막힌 점 / 결정 필요

- AC11 문구: 흐름은 메시지를 저장하지 않아 `ANALYSIS_500`이 재시작·큐 거부·예기치 못한 오류에 같이 쓰인다 → 재시작 문구를 모든 ANALYSIS_500 흐름에 보여 줄지 사용자 확인 필요
- 사람: 이슈 #16, 노션 팀원 초대. 하네스: `codex-review.mjs` 옛 스레드 FINDINGS 판정

## 주의

- 작업로그에 F-06 작업(11-18 19:50 이후) 항목이 없다 → 다음 handoff 때 보충
- 통합 테스트는 Docker Desktop이 켜져 있어야 한다. V1~V5 수정 금지(V5 커밋 후)
