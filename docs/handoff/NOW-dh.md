# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-23 16:06 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-05b-analysis-url

## 현재 기능

**F-05b** 분석 요청 — 공개 저장소 URL 수집 (`docs/specs/F-05b-analysis-request-url.md`, **APPROVED 11-23**)

## 진행

- ✅ spec 승인(11-23), 브랜치 `feature/F-05b-analysis-url`
- ✅ AC2 형식: `analysis/fetch/RepositoryUrl`(+`InvalidRepositoryUrlException`), `RepositoryUrlTest` 43개 green
- ✅ AC4~AC10 수집기: `RepositoryArchiveFetcher`·`FetchProperties`, ErrorCode ANALYSIS_004·502·503, yml `analysis.fetch`, `.env.example` GITHUB_TOKEN. `RepositoryArchiveFetcherTest`(가짜 서버 `FakeRepositoryServer`) 23개 green
- 교훈: 시간 초과로 취소한 작업 스레드가 끊긴 본문을 EOF로 읽고 파일을 남김 → `download()`가 스레드 종료 뒤 실패면 직접 삭제
- ✅ AC1~3·12 요청 API: `CreateAnalysisRequest.of(file, sourceUrl, level, consent)`, `AnalysisService.request`(requestZip 이름 변경), `Analysis.newUrl`, 상세 `sourceUrl`, openapi.json 갱신. `AnalysisControllerTest` F05b_* 7개
- ✅ AC4·5·6·11 파이프라인: `AnalysisPipeline.fetch` URL 분기. `AnalysisPipelineTest` F05b_* 5개(static 가짜 서버 + `@DynamicPropertySource`). analysis 패키지 316개 green
- ✅ `RepositoryArchiveFetcherLiveTest`(@Tag live, DS-02-01 dev) 작성, 아직 실행 안 함. conventions 3절 코드 3개

- ✅ FE AC13·14: `createAnalysis({file, sourceUrl, ...})`, SC-05 방식 라디오·주소 칸, SC-07 "저장소 보기" 링크. verify fe 통과

- ✅ contract-checker 일치(링크 표시 조건만 해석 문제, 아래), 산출물 06 SC-05·SC-07·08 외부API 갱신, #24 덧글

## 바로 다음 할 일

1. ui-verifier SC-05 주소 방식(375·1440px) 결과 확인. 8080의 BE가 옛 빌드라 POST·SC-07 링크 E2E는 BE 재기동 후
2. `/pr-review` (공통 파일 ErrorCode·application.yml 변경 → @JoonseobKim 멘션). liveTest(DS-02-01)는 사람이 `./gradlew liveTest`

## 막힌 점 / 결정 필요

- 사람 확인: "저장소 보기" 링크는 DONE일 때만 보인다(상세는 DONE에서만 불러오는 F-06 AC13 유지). 진행 중·FAILED에도 보여야 하면 상태 응답에 sourceUrl 추가 필요

- 사람: 노션 팀원 초대(대시보드 공유 → 김준섭 이메일, 편집 허용)
- 남은 로컬 브랜치 `feature/F-00-session-ops`·`feature/F-03-wrapup`은 develop 미머지라 `-d`로 안 지워짐 (그대로 둠)

## 주의

- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시)
- 하네스 가드는 Bash 명령 문자열의 `core.hooksPath`를 막는다 → 테스트 파일 편집은 Edit 도구로
- 하네스 동결(결정로그 11-23): 하네스는 버그·작업을 막는 문제만 고친다
