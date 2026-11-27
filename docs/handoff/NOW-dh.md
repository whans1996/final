# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 20:03 · 작업자: whans1996 · PC: dh-home · 브랜치: feature/P-1a-project-versions

## 현재 기능

**P-1a 프로젝트·버전 묶음·새 커밋 배지** `docs/specs/P-1a-project-versions.md` (🟡 BE: AC1~11·13 green, 남은 BE는 AC12) · 브랜치 `feature/P-1a-project-versions` · 로드맵 P-1a (넘겨주기 12/02 A→A·B)

## 진행

- ✅ AC13·1·4·5 (`fa4ce26`) V15, 요청 묶기, PROJECT_404
- ✅ AC2·3·11 수집기 `fetch/RepositoryCommitFetchTest` 21개, 파이프라인 `AnalysisPipelineTest` `P1a_*` 4개
- ✅ AC6~10 (`e9710dc`) `project/ProjectController`·`ProjectService.listMine/getDetail/rename/latestCommit`·`dto/`·`ProjectProperties`(`analysis.project.latest-commit-ttl` 5m)·`ProjectMapper.findVisible…`, `project/ProjectControllerTest` 17개. 보이는 프로젝트 = 내 것 + 삭제 안 한 분석 있음 (PATCH·배지도 같은 기준으로 404)
- ⬜ AC12 BE, AC14~16 FE, 산출물(ERD 03, 화면 설계서 06)

## 바로 다음 할 일

1. AC12: `AnalysisDetailResponse`에 `project`(id·name)·`version`(id·number·commitSha·commitMessage·committedAt, 없으면 null), `AnalysisSummaryResponse`에 `projectId`·`versionNumber`. `AnalysisMapper.xml` `findViewableDetail`·`findByUser`에 서브쿼리로 (columns에 별칭 없음, JOIN 말고). 테스트 `AnalysisControllerTest#AC12_…`(공개·팀 공유로 보는 사람도 같은 값), `AnalysisRerunApiTest`에 재실행 분석이 원본 project_id 유지·버전 null 단언 → `OpenApiExportTest`로 openapi.json
2. FE (새 세션 권장): `src/api/projects.js` + `views/project/ProjectListView.vue`·`ProjectDetailView.vue`·`components/project/LatestCommitBadge.vue`·`ProjectVersionList.vue`, `router/routes/project.js`, `AnalysisRequestView` `?projectId=` 고정, `AnalysisDetailView` 링크(AC16). F-29 토큰·컴포넌트, V-4 `useLatestRequest` 사용. 셸 내비 "프로젝트"는 공통 파일
3. 산출물: `docs/산출물/03-ERD.md` projects·project_versions, `06-화면설계서.md` SC-25·26

## 막힌 점 / 결정 필요
- 없음 (사람: 노션 초대, F-05b liveTest, #251 나머지 PC `--move-env`. dh-home은 11-27 옮김)

## 주의
- V15 (develop 미머지). 분석을 SQL로 넣는 테스트는 `TestProjects.zip/url(jdbc, …)`. 공통 변경(`ErrorCode`, `application.yml`, Flyway·ErrorCode 테스트, `post/PostApiTestBase` 등 테스트 INSERT, conventions.md 에러 표, `IntegrationTestBase` MySQL `--max-connections=500`(컨텍스트가 늘어 Too many connections, `950893d`)) → PR에서 @JoonseobKim 멘션
- 커밋 조회 관련 테스트는 구현이 먼저라 red 없이 green (AC6~10은 red 17 → green 확인)
