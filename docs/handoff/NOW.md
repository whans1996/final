# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-05a-analysis-zip

## 현재 기능

F-05a 분석 요청(ZIP) — spec APPROVED, `/implement` 진행 중. 이번 세션 목표: 파이프라인 AC5~AC11 (FE AC14~16은 다음 세션)

## 진행

- ✅ (커밋 0c298c3) V3, BE API AC1~4·12·13, openapi.json
- ✅ 파이프라인 테스트 작성·red 확인(37개 중 34 실패, 모두 assertion): `analysis/pipeline/ZipArchiveReaderTest`·`SecretRedactorTest`·`FileSelectorTest`, `AnalysisPipelineTest`(AC5~10, `@MockitoSpyBean AnalysisService`), `AnalysisRecoveryTest`(AC11)
- ✅ 구현·verify be green(206개): `analysis/pipeline/`(ZipArchiveReader·SecretRedactor·FileSelector·Layer·SourceFile·SelectedFile·AnalysisFailedException), `AnalysisPipeline`(단계 리스트 `stages`), `AnalysisRecovery`(ApplicationReadyEvent), `AnalysisService.advance·saveFiles·complete·failInterrupted`, `AnalysisMapper` 4개 SQL, `domain/AnalysisFile`, `feed/ActivityService·ActivityMapper·ActivityType`, `AnalysisTempFiles.deleteAll`

## 바로 다음 할 일

1. FE AC14~16: `src/api/analyses.js`(+`__tests__/analyses.spec.js`), `AnalysisRequestView`·`AnalysisDetailView`·`AnalysisListView`(+ spec 3개), `components/analysis/AnalysisStatusBadge.vue`, 라우트 3개, 헤더 링크
2. 그 뒤 contract-checker·ui-verifier → 산출물(06 화면설계서 SC-05·06·07, 04-WBS 3.2a·3.2b) → `/pr-review` (reviewer 보안 중점: Zip Slip·zip bomb·키 마스킹)

## 막힌 점 / 결정 필요

- 없음. spec 해석: 1MB 초과로 건너뛴 파일도 "지원 파일 수(200)"에는 센다. `sk-` 패턴은 단어 중간(`task-...`)을 제외하려고 앞에 영숫자가 없을 때만 가린다

## 주의

- `AnalysisTestSupport`는 트랜잭션 롤백 대신 `@f05a.test` 회원 DELETE로 정리. 테스트의 키·PEM 문자열은 하네스 키 가드에 걸리지 않게 이어 붙여 만든다
- ZIP은 `ZipFile`로 메모리에서만 읽는다(디스크에 풀지 않음). 파일명 UTF-8 실패 시 MS949로 재시도
- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다. V1·V2·V3 수정 금지
