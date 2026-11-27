# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 22:28 · 작업자: whans1996 · PC: dh-home · 브랜치: feature/P-1a-project-versions

## 현재 기능

**P-1a 프로젝트·버전 묶음·새 커밋 배지** `docs/specs/P-1a-project-versions.md` (🟡 BE AC1~13 ✅, FE AC14~16 ✅, 남은 것 산출물·검증·PR) · 브랜치 `feature/P-1a-project-versions` · 로드맵 P-1a 11/27~12/02 (넘겨주기 12/02 A→A: T-1·I-1b·I-2) · 사용자 지시(11-27): 머지 전까지 자동 진행

## 진행

- ✅ BE AC1~13 (`fa4ce26`·`e9710dc`·`ee2b23b` 등)
- ✅ FE AC14~16: `api/projects.js`, `components/project/`(LatestCommitBadge·ProjectVersionList·projectLabels), `views/project/`(목록·상세), `routes/project.js`(메뉴 "프로젝트" learn order 25, FolderGit2), 요청 화면 `?projectId=` 고정, 상세 프로젝트 링크, 목록 "프로젝트별로 보기"
- ✅ 고친 화면 3개(AnalysisRequest·Detail·List) Tailwind·`useLatestRequest` 이전 → #250 칸 체크 필요, ADOPTED에 6개 추가
- ⬜ 산출물(ERD 03 projects·project_versions, 화면 설계서 06 SC-25·26, 요구사항 01 분석 관리 행), contract-checker, ui-verifier, reviewer, `/pr-review`

## 바로 다음 할 일

1. 산출물: `docs/산출물/03-ERD*`에 projects·project_versions·analyses.project_id/version_id, `06-화면설계서.md`에 SC-25·SC-26 행·흐름, `01-요구사항*` 분석 관리 행
2. `/pr-review`: verifier·reviewer·contract-checker, ui-verifier(`/projects`), PR 본문 `Roadmap: P-1a`, 공통 파일(CODEOWNERS·router/index.js·navIcons·App.spec·navItems.spec·latestRequestAdoption.spec·frontend/CLAUDE.md) @JoonseobKim
3. 완료 표시 커밋: 로드맵·WBS·간트 P-1a 끝 날짜 실제로, 뒤 항목(T-1·I-1b·I-2·P-1b) 당김, #250 칸 체크

## 막힌 점 / 결정 필요
- 없음
- 사람: 노션 초대, F-05b liveTest + #226 퀴즈 실패 화면 실제 LLM 점검(12/08 전), #251 js PC `--move-env`, PR 머지

## 주의
- V15 develop 미머지. 공통 변경(ErrorCode·application.yml·IntegrationTestBase·FE 라우터·navIcons·CODEOWNERS 등) PR에서 @JoonseobKim
- 이 PC는 F-29a 뒤 `npm ci`가 안 돼 있었다 (#241 증상, 11-27 해결)
