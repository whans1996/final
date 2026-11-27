# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 17:49 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/P-1a-project-versions

## 현재 기능

**P-1a 프로젝트·버전 묶음·새 커밋 배지** `docs/specs/P-1a-project-versions.md` (🟡 BE 구현 중: AC13·1·4·5 green, wip 커밋) · 브랜치 `feature/P-1a-project-versions` · 로드맵 P-1a (넘겨주기 12/02 A→A·B)

## 진행

- ✅ AC13 `V15__projects.sql`·`project/ProjectBackfillMigrationTest`, AC1·4·5 `analysis/AnalysisProjectRequestTest` 통과 (`ProjectService.assignUrl/assignZip`, `CreateAnalysisRequest.projectId`, 응답 `projectId`, `PROJECT_404`)
- 🟡 AC2·3·11 구현만 됨, 테스트 없음: `RepositoryArchiveFetcher.latestCommit`·`download(repo, sha, target)`·`parseCommit`, `RepositoryCommit`, `AnalysisPipeline.fetch` → `AnalysisService.attachVersion` → `ProjectService.versionFor`
- ⬜ AC6~10 API, AC12 필드, AC14~16 FE, openapi.json, 산출물

## 바로 다음 할 일

1. 전체 BE 테스트 `node scripts/verify.mjs be`: 이번에 테스트 20곳 INSERT에 `project_id`를 넣었다(`common/TestProjects.zip/url`). 깨진 것이 있으면 고친 뒤 `OpenApiExportTest`로 `docs/api/openapi.json` 갱신 → `feat(P-1a): AC13·AC1·AC4·AC5 ...` 커밋
2. 새 파일 `backend/src/test/java/com/vibenote/analysis/fetch/RepositoryCommitFetchTest.java` (F-05b AC 번호와 섞이지 않게 분리): `FakeRepositoryServer`(이번에 `Recorded.rawQuery` 추가)로 AC2 GitHub `/repos/octo/todo-app/commits` → `zipball/{sha}`, GitLab `repository/commits` → `archive.zip?sha=`, 메시지 첫 줄 200자·시각 KST 변환 / AC3 429·403(한도0)·5xx·404·409(빈 저장소)·`[]`·SHA 형식 아님·시간 초과(`properties(token, timeout, commitTimeout)`) → empty / AC11 토큰+비공개 → ANALYSIS_004, 커밋 목록 요청 안 함
3. AC2 파이프라인: `AnalysisPipelineTest`(가짜 서버 `REPOSITORY`)에 같은 커밋 두 수준 → 같은 버전, 새 커밋 → 번호+1, 조회 실패 → 커밋 없는 새 버전, 받기 실패 → 버전 없음
4. AC6~10: `analysis/project/ProjectController`·dto·`ProjectMapper` 목록/상세/이름/배지(`analysis.project.latest-commit-ttl` 5m, `projects.latest_*` 캐시) + `ProjectControllerTest` (`@TestBean Clock`) · 5. AC12: `findViewableDetail`·`findByUser`에 프로젝트·버전 (columns에 별칭 없으니 JOIN 말고 서브쿼리), `AnalysisRerunApiTest`에 프로젝트 유지 단언

## 막힌 점 / 결정 필요
- 없음 (사람: 노션 초대, F-05b liveTest, #251 `--move-env`)

## 주의
- 마이그레이션 V15 (develop 미머지). `analyses.project_id NOT NULL` → 분석을 SQL로 직접 넣는 테스트는 `TestProjects.zip(jdbc, userId)` 필요. 상대 영역 `post/PostApiTestBase`·공통 테스트 4개·`ErrorCode`·V15 변경 → PR에서 @JoonseobKim 멘션 (B 새 테스트도 영향)
- 새 설정 `analysis.fetch.commit-timeout`(5s, FetchProperties 마지막 인자) · 인라인 node -e에 작은따옴표 금지(scratchpad edit.mjs) · 셸 내비 "프로젝트"는 공통
