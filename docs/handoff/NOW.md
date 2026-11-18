# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 20:30 · 작업자: whans1996 (Claude, 집 PC) · 브랜치: feature/F-06-overview-explanation

## 현재 기능

F-06 개요·파일별 설명 — `docs/specs/F-06-overview-explanation.md` **APPROVED, 구현 중 (BE 먼저, 이번 세션 목표 AC1~AC12)**

## 진행

- ✅ V4 `analysis_files.role`, `application.yml` `analysis.explain.*`·`llm.ollama.num-ctx`, ErrorCode `ANALYSIS_001`·`LLM_502`
- ✅ `analysis.explain` 패키지 단위 green 32개: `ApiDetector`(AC7), `PromptTemplates`+프롬프트 6개(AC2·AC3), `ExplainPrompts`(AC3), `FileExplainer`(AC4·5·6), `OverviewExplainer`(AC5·6·8), `LlmJsonCall`(1회 재시도)
- ✅ Mock fixture `llm-fixtures/overview.json`·`file.json`(DS-02-01 realworld 기준), 테스트 fixture 새 스키마
- ⏳ 파이프라인 연결(AC1·AC2), 조회 API(AC9~11), Ollama num_ctx(AC12)

## 바로 다음 할 일

1. `AnalysisPipeline.stages`에 `EXPLAINING(40)` 추가 → `explain(Context)`: `AnalysisMapper.findFiles` → `ApiDetector.detect` → `OverviewExplainer` → `saveOverview`·advance 45 → 6개 묶음 `FileExplainer` → `updateFileExplanation`·advance `45+floor(24k/n)`
2. 테스트: `AnalysisExplainingPipelineTest`(@MockitoSpyBean LlmClient) AC1·AC2·AC5·AC6, `AnalysisPipelineTest#AC5_단계를...`에 EXPLAINING 기대값 추가
3. `GET /api/analyses/{id}`·`/files`·`/files/{fileId}` (`AnalysisResultApiTest` AC9~11), `OllamaLlmClientTest#AC12_num_ctx를_보낸다`
4. 그다음 FE AC13~15 (다음 세션)

## 막힌 점 / 결정 필요

- 사람: 이슈 #16(설정 키 키워드 확장 범위), 노션 대시보드 팀원 초대
- 사람: 집 PC 스마트 앱 컨트롤이 oxlint를 막음 → SAC 끄기 또는 wasm 바인딩. FE 커밋 전 필요

## 주의

- 1.4 유스케이스(마감 11/23): `docs/산출물/02-유스케이스.md`에 분석 요청 UC 흐름 추가 필요
- 집 PC DB는 V1~V3 적용 상태(V4는 bootRun 때 적용). verify(Gradle)를 bootRun과 동시에 돌리지 않는다
- `.env`는 properties 형식으로 읽힌다: 따옴표·끝 공백이 값에 포함됨
