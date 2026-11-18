# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 21:05 · 작업자: whans1996 (Claude, 집 PC) · 브랜치: feature/F-06-overview-explanation

## 현재 기능

F-06 개요·파일별 설명 — `docs/specs/F-06-overview-explanation.md` **APPROVED, 구현 중 (BE AC1~AC12 구현 완료, 전체 검증 중)**

## 진행

- ✅ 커밋 8b09a86: V4, `analysis.explain` 패키지 + 프롬프트 6개 + fixture (단위 32개 green)
- 🔧 미커밋(전체 BE verify 실행 중): `AnalysisPipeline.explain`(EXPLAINING), Mapper 6개 쿼리, `AnalysisService` 조회·저장, `AnalysisController` GET `/{id}`·`/files`·`/files/{fileId}`, DTO 3개, `OllamaLlmClient` options.num_ctx, `AnalysisStatusResponse` errorCode 허용값 LLM_502·503, conventions 3절, ERD·datasets DS-06
- 🔧 테스트 수정: `AnalysisExplainingPipelineTest` 파일 블록 카운터를 `<<<FILE no=\d+ `로(템플릿 규칙 문장 제외), apis는 JsonNode 트리 비교(MySQL JSON이 키 순서를 바꿈)

## 바로 다음 할 일

1. `node scripts/verify.mjs be` 통과 확인 → `docs/api/openapi.json` 변경 포함해 커밋 `feat(F-06): AC1·AC2·AC5·AC6·AC9~AC12 EXPLAINING 단계와 결과 조회 API`
2. spec 11절 체크리스트(BE 구현, Swagger) 체크, `contract-checker` 실행
3. 다음 세션(FE, AC13~15): `src/api/analyses.js`에 `fetchAnalysis`·`fetchAnalysisFiles`·`fetchAnalysisFile`, `utils/layers.js`, `components/analysis/AnalysisOverviewTab.vue`·`AnalysisFilesTab.vue`·`CodeViewer.vue`, `AnalysisDetailView.vue` 탭 + Vitest 3종
4. 산출물: `06-화면설계서.md` SC-07 개요·파일 탭, `08-외부API-생성형AI-활용.md` §1.3 프롬프트 설계

## 막힌 점 / 결정 필요

- 사람: 이슈 #16(설정 키 키워드 확장 범위), 노션 대시보드 팀원 초대
- 사람: 집 PC 스마트 앱 컨트롤이 oxlint를 막음 → SAC 끄기 또는 wasm 바인딩. FE 커밋 전 필요

## 주의

- 1.4 유스케이스(마감 11/23): `docs/산출물/02-유스케이스.md`에 분석 요청 UC 흐름 추가 필요
- 집 PC DB는 V1~V3 적용 상태(V4는 bootRun 때 적용). verify(Gradle)를 bootRun과 동시에 돌리지 않는다
- `.env`는 properties 형식으로 읽힌다: 따옴표·끝 공백이 값에 포함됨
