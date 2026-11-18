# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 20:40 · 작업자: whans1996 (Claude, 집 PC) · 브랜치: feature/F-06-overview-explanation

## 현재 기능

F-06 개요·파일별 설명 — `docs/specs/F-06-overview-explanation.md` **DRAFT, 사람 승인 대기**

## 진행

- ✅ 브랜치 생성, 첫 커밋 `chore(harness): gh issue 명령 허용`(settings.json)
- ✅ spec 초안 AC1~AC15: EXPLAINING 단계(개요 1회 + 파일 6개 묶음), 1회 재시도·부분 성공, 규칙 기반 API 탐지, `GET /api/analyses/{id}`·`/files`·`/files/{fileId}`, V4 `analysis_files.role`, Ollama num_ctx, SC-07 개요·파일 탭
- ✅ 기획서 API 표(`/files/{fileId}`)·결정 로그 갱신

## 바로 다음 할 일

1. 사람이 spec을 읽고 승인 → `/spec approve F-06` (상태 APPROVED, 커밋 `docs(F-06): spec 승인`)
2. `/clear` 후 새 세션(Opus/high)에서 `/implement F-06`: V4 마이그레이션 → `ApiDetectorTest`·`PromptTemplatesTest`·`FileExplainerTest`·`OverviewExplainerTest` red → `AnalysisPipeline.stages`에 EXPLAINING 추가(F-05a AC5 테스트 기대값 수정)
3. 1.4 유스케이스(마감 11/23): `docs/산출물/02-유스케이스.md`에 분석 요청(F-05a) UC 흐름 추가
4. 팀원 B PC에서 `docs/환경설정.md` 6절 체크리스트로 교대 리허설 (사람)

## 막힌 점 / 결정 필요

- 사람: F-06 spec 승인, 이슈 #16(설정 키 키워드 확장 범위), 노션 대시보드 팀원 초대
- 사람: 집 PC 스마트 앱 컨트롤이 oxlint를 막음 → SAC 끄기 또는 wasm 바인딩. FE 커밋 전 필요

## 주의

- Mock fixture(`llm-fixtures/overview.json`·`file.json`)는 dev 데이터(DS-02-01)만 보고 쓴다. 테스트 전용 `src/test/resources/llm-fixtures/overview.json`이 main fixture를 가리므로 새 스키마로 바꾼다
- 집 PC DB는 V1~V3 적용 상태. verify(Gradle)를 bootRun과 동시에 돌리지 않는다. V1·V2·V3 수정 금지
- `.env`는 properties 형식으로 읽힌다: 따옴표·끝 공백이 값에 포함됨
