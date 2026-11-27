# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 21:50 · 작업자: whans1996 · PC: dh-home · 브랜치: feature/P-1a-project-versions

## 현재 기능

**P-1a 프로젝트·버전 묶음·새 커밋 배지** `docs/specs/P-1a-project-versions.md` (🟡 BE AC1~13 ✅, 남은 것 FE AC14~16·산출물) · 브랜치 `feature/P-1a-project-versions` · 로드맵 P-1a 11/27~12/02 (넘겨주기 12/02 A→A: T-1·I-1b·I-2) · 별도 docs PR **#255** (#254 역할·일정 정리) CI 대기

## 진행

- ✅ AC13·1·4·5 (`fa4ce26`), AC2·3·11 (수집기 21·파이프라인 4), AC6~10 (`e9710dc`, `ProjectControllerTest` 17), AC12 (`ee2b23b`), `IntegrationTestBase` max-connections 500
- ✅ #255 (`feature/issue-254-roles-roadmap`): 로드맵 E-2·NT-1·NT-2·AM-1·HP-6, 재조정, 문서 즉시 갱신 규칙, WBS·간트·01, 이슈 댓글
- ⬜ FE AC14~16, 산출물(ERD 03 projects·project_versions, 화면 설계서 06 SC-25·26)

## 바로 다음 할 일

1. #255: `gh pr checks 255` 초록이면 사람이 GitHub에서 머지 (문서만 PR, Codex 생략) → P-1a에서 `git merge origin/develop` (NOW-dh 충돌은 이 파일 쪽으로)
2. FE (새 세션, Opus/high): `src/api/projects.js`(`fetchProjects({page,size})`·`fetchProject(id)`·`fetchLatestCommit(id)`·`updateProject(id,{name})`), `analyses.js` `createAnalysis`에 `projectId`, `views/project/ProjectListView.vue`·`ProjectDetailView.vue`, `components/project/LatestCommitBadge.vue`·`ProjectVersionList.vue`, `router/routes/project.js`, `AnalysisRequestView` `?projectId=` 고정, `AnalysisDetailView` 프로젝트 링크(주인만). 고치는 화면은 Tailwind·토큰·`useLatestRequest`로 옮기고 #250 칸 체크 (11-27 규칙). Vitest는 spec 7절 파일 먼저
3. 완료 표시 커밋에서 로드맵·WBS·간트 P-1a 행 끝 날짜를 실제로, 일찍 끝나면 T-1·P-1b·I-1b·I-2 당김 (CLAUDE.md "일정은 바로 반영")

## 막힌 점 / 결정 필요
- 없음 (idea 이슈·#205 AM-1·#226 처리 시점 11-27 결정, PR #255)
- 사람: 노션 초대, F-05b liveTest + #226 퀴즈 실패 화면 실제 LLM 점검(같은 GMS 세션, 12/08 전), #251 js-ssafy·js-home `--move-env`

## 주의
- V15 develop 미머지. 공통 변경(ErrorCode·application.yml·IntegrationTestBase·PostApiTestBase 등) PR에서 @JoonseobKim
- 바뀌면 즉시 모든 문서·이슈, 새 일정·조기 완료는 바로 로드맵 (11-27 사용자)
