# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 21:10 · 작업자: whans1996 (Claude, 집 PC) · 브랜치: feature/F-06-overview-explanation

## 현재 기능

F-06 개요·파일별 설명 — `docs/specs/F-06-overview-explanation.md` **APPROVED, BE(AC1~AC12) 완료·커밋, FE(AC13~15) 남음**

## 진행

- ✅ 8b09a86: V4, `analysis.explain`(ApiDetector·PromptTemplates·ExplainPrompts·Overview/FileExplainer·LlmJsonCall), 프롬프트 6개, fixture
- ✅ 58a6ccd: `AnalysisPipeline` EXPLAINING, `GET /api/analyses/{id}`·`/files`·`/files/{fileId}`, Ollama num_ctx, openapi.json, conventions·ERD·DS-06. BE 273개 green (push 전)
- ⏳ contract-checker(BE↔spec) 실행 중

## 바로 다음 할 일

1. `/clear` 후 새 세션(Opus/high)에서 `/implement F-06` 이어서 FE:
   - `frontend/src/api/analyses.js`에 `fetchAnalysis(id)`·`fetchAnalysisFiles(id)`·`fetchAnalysisFile(id, fileId)` + `src/api/__tests__/analyses.spec.js`
   - `src/utils/layers.js`(spec 5.5절 라벨·순서), `components/analysis/AnalysisOverviewTab.vue`·`AnalysisFilesTab.vue`·`CodeViewer.vue`
   - `views/AnalysisDetailView.vue`: DONE이면 상세 1회 로드 + 탭(쿼리 `?tab=files&file=123`), Vitest `AnalysisDetailView.spec.js`·`AnalysisOverviewTab.spec.js`·`AnalysisFilesTab.spec.js`
2. ui-verifier(데스크톱·375px), 산출물 `06-화면설계서.md` SC-07, `08-외부API-생성형AI-활용.md` §1.3, `04-WBS`·`05-간트` 실적
3. `/pr-review`

## 막힌 점 / 결정 필요

- 사람: 이슈 #16(설정 키 키워드 확장 범위), 노션 대시보드 팀원 초대
- 사람: 집 PC 스마트 앱 컨트롤이 oxlint를 막음 → SAC 끄기 또는 wasm 바인딩. **FE 커밋 전 필요**

## 주의

- 1.4 유스케이스(마감 11/23): `docs/산출물/02-유스케이스.md`에 분석 요청 UC 흐름 추가 필요
- MySQL JSON 컬럼(`analyses.overview`)은 키 순서를 바꿔 저장한다. 테스트는 문자열 말고 트리로 비교
- 집 PC DB는 V1~V3 적용 상태(V4는 bootRun 때 적용). verify(Gradle)를 bootRun과 동시에 돌리지 않는다
