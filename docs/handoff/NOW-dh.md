# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 19:29 · 작업자: whans1996 · PC: dh-home · 브랜치: feature/P-1a-project-versions

## 현재 기능

**P-1a 프로젝트·버전 묶음·새 커밋 배지** `docs/specs/P-1a-project-versions.md` (🟡 BE 구현 중: AC13·1·4·5·2·3·11 green) · 브랜치 `feature/P-1a-project-versions` · 로드맵 P-1a (넘겨주기 12/02 A→A·B)

## 진행

- ✅ AC13·1·4·5 (`fa4ce26`): V15, `AnalysisProjectRequestTest`, `ProjectBackfillMigrationTest`. `FlywayMigrationTest` 테이블 목록·`ErrorCodeTest` 패턴에 PROJECT, conventions.md 에러 표, openapi.json
- ✅ AC2·3·11 수집기 `fetch/RepositoryCommitFetchTest` 21개 (`93ae126`, 구현이 먼저라 red 없음)
- ✅ AC2·3 파이프라인 `AnalysisPipelineTest` `P1a_AC2_*`·`P1a_AC3_*` 4개 (`28a6a9a`)
- ⬜ AC6~10 API, AC12 필드, AC14~16 FE, 산출물

## 바로 다음 할 일

1. AC6~10: `analysis/project/ProjectController`·`dto/`·`ProjectMapper`(목록 `GET /api/projects` 페이지·버전 수·최신 버전·수준 목록 / 상세 `GET /api/projects/{id}` 버전·분석 / `PATCH` 이름 1~100자 / `GET /{id}/latest-commit` `NEW_COMMIT·UP_TO_DATE·UNKNOWN·NOT_SUPPORTED`, `analysis.project.latest-commit-ttl` 5m, `projects.latest_*` 캐시) + `ProjectControllerTest` (`@TestBean Clock`, 가짜 저장소 서버는 `AnalysisPipelineTest` `REPOSITORY`·`@DynamicPropertySource` 방식)
2. AC12: `findViewableDetail`·`findByUser`에 프로젝트·버전 (columns에 별칭 없으니 JOIN 말고 서브쿼리), `AnalysisRerunApiTest`에 프로젝트 유지 단언 → openapi.json
3. AC14~16 FE (`src/api/projects.js`, `views/project/*`, `LatestCommitBadge.vue`)

## 막힌 점 / 결정 필요
- 없음 (사람: 노션 초대, F-05b liveTest, #251 나머지 PC `--move-env`. dh-home은 11-27 옮김)

## 주의
- 마이그레이션 V15 (develop 미머지). `analyses.project_id NOT NULL` → 분석을 SQL로 직접 넣는 테스트는 `TestProjects.zip/url(jdbc, …)`. 상대 영역 `post/PostApiTestBase`·공통 테스트(Flyway·ErrorCode 포함)·`ErrorCode`·V15 변경 → PR에서 @JoonseobKim 멘션
- 새 설정 `analysis.fetch.commit-timeout`(5s, FetchProperties 마지막 인자) · 인라인 node -e에 작은따옴표 금지 · 셸 내비 "프로젝트"는 공통
