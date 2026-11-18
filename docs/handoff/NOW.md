# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-05a-analysis-zip

## 현재 기능

F-05a 분석 요청(ZIP) — spec APPROVED, `/implement` 진행 중. 이번 세션 목표: V3 + BE API AC1~AC4·AC12·AC13 (파이프라인 AC5~11·FE AC14~16은 다음 세션)

## 진행

- ✅ V3 `analyses.error_code`, `AnalysisControllerTest` 12개 작성, red 확인(404, AC3만 통과)
- ✅ BE API 구현·커밋: `analysis/` Controller·Service·Mapper(XML)·dto 4종·`AnalysisPipelineLauncher`(AFTER_COMMIT→`analysisExecutor`, 거절 시 FAILED, 롤백 시 임시 파일 삭제)·`AnalysisTempFiles`·`AnalysisProperties`·`AnalysisPipeline.run` 빈 껍데기. `ErrorCode` ANALYSIS_*, `BusinessException` 메시지 지정 생성자, `UserService.lockActiveMember`(FOR UPDATE), `common.config.ClockConfig`(Asia/Seoul). ERD 산출물 1줄 반영
- ✅ verify be 전체 green, openapi.json 갱신(분석 3개 엔드포인트)

## 바로 다음 할 일

1. 파이프라인: `analysis/pipeline/ZipArchiveReaderTest`·`SecretRedactorTest`·`FileSelectorTest`(단위) → `AnalysisPipelineTest`(AC5~AC10, `run(id)` 직접 호출) → `AnalysisRecoveryTest`(AC11)
2. FE AC14~16 (`src/api/analyses.js`, 3개 View, `AnalysisStatusBadge.vue`)

## 막힌 점 / 결정 필요

- 없음. 413(10MB 초과)은 MockMvc가 multipart 한도를 재현하지 못해 기존 GlobalExceptionHandler 매핑으로 갈음(테스트 주석)

## 주의

- `AnalysisTestSupport`는 트랜잭션 롤백을 안 쓰고 `@f05a.test` 회원 DELETE로 정리(커밋 후 파이프라인 시작 검증 때문). `AnalysisControllerTest`는 `@MockitoBean AnalysisPipeline`
- `@EnableAsync` 대신 실행기 직접 호출(거절 처리 위해). AFTER_COMMIT 콜백 안 DB 쓰기는 `REQUIRES_NEW`(`AnalysisService.markFailed`)
- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다. V1·V2·V3(커밋 후) 수정 금지
