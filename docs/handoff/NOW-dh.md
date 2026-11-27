# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 20:25 · 작업자: whans1996 · PC: dh-home · 브랜치: feature/P-1a-project-versions

## 현재 기능

**P-1a 프로젝트·버전 묶음·새 커밋 배지** `docs/specs/P-1a-project-versions.md` (🟡 BE 끝: AC1~13 green, 남은 것 FE AC14~16·산출물) · 브랜치 `feature/P-1a-project-versions` · 로드맵 P-1a (넘겨주기 12/02 A→A·B)

## 진행

- ✅ AC13·1·4·5 (`fa4ce26`), AC2·3·11 수집기·파이프라인 테스트 25개
- ✅ AC6~10 (`e9710dc`) `project/ProjectController`·`ProjectService`·`dto/`, `ProjectControllerTest` 17개 (보이는 프로젝트 = 내 것 + 삭제 안 한 분석 있음)
- ✅ AC12 (`ee2b23b`) 상세 `project`·`version`(`AnalysisDetail.projectRefName·version*`, 서브쿼리), 목록 `projectId`·`versionNumber`(`AnalysisListItem`), 테스트 `AnalysisProjectRequestTest#AC12_*` 4개·`AnalysisRerunApiTest#P1a_AC12_*`
- ✅ `IntegrationTestBase` MySQL `--max-connections=500` (`950893d`)
- ⬜ FE AC14~16, 산출물(ERD 03, 화면 설계서 06)

## 바로 다음 할 일

1. FE (새 세션, Opus/high): `src/api/projects.js`(`fetchProjects({page,size})`·`fetchProject(id)`·`fetchLatestCommit(id)`·`updateProject(id,{name})`), `analyses.js` `createAnalysis`에 `projectId`. `views/project/ProjectListView.vue`(SC-25)·`ProjectDetailView.vue`(SC-26), `components/project/LatestCommitBadge.vue`(4상태)·`ProjectVersionList.vue`, `router/routes/project.js`. `AnalysisRequestView` `?projectId=`면 입력 방식·주소 고정, `AnalysisDetailView` "프로젝트 이름 · v번호" 링크(주인만, AC16). F-29 토큰·공통 컴포넌트, V-4 `useLatestRequest`. 셸 내비 "프로젝트"·내 분석 목록 "프로젝트별로 보기" 링크. 테스트는 spec 7절 Vitest 파일들 먼저
2. 산출물: `docs/산출물/03-ERD.md` projects·project_versions, `06-화면설계서.md` SC-25·26 → contract-checker·ui-verifier → `/pr-review`

## 막힌 점 / 결정 필요
- 없음 (사람: 노션 초대, F-05b liveTest, #251 나머지 PC `--move-env`. dh-home은 11-27 옮김)

## 주의
- V15 (develop 미머지). 분석을 SQL로 넣는 테스트는 `TestProjects.zip/url(jdbc, …)`. 공통 변경(`ErrorCode`, `application.yml`, Flyway·ErrorCode 테스트, `post/PostApiTestBase` 등 테스트 INSERT, conventions.md 에러 표, `IntegrationTestBase` max-connections) → PR에서 @JoonseobKim 멘션
- 커밋 조회·rerun 테스트는 구현이 먼저라 red 없이 green (AC6~10 red 17·AC12 red 4 → green 확인)
- FE 작업 전 Docker Desktop 켜기 (dh-home oxlint)
