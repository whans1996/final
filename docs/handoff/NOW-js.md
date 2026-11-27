# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-27 15:18 · 작업자: JoonseobKim · PC: js-ssafy · 브랜치: feature/F-29b-app-shell

## 현재 기능

**F-29b 앱 셸** `docs/specs/F-29b-app-shell.md` (APPROVED, AC14) · 브랜치 `feature/F-29b-app-shell` (develop a234309에서, F-29a PR #244 머지됨) · ~12/02 넘겨주기. PR 본문 `Roadmap: F-29`

## 진행

- ✅ 커밋·push: AC1 navGroups·navIcons·route group/icon (e9acb91), CODEOWNERS shell (189ab5a), AC2~AC4 AppSidebar (fedbec1), AC8~AC11 HeaderSearch·searchTargets·ThemeToggle 2단계(결정로그·spec AC10 수정)·UserMenu (0e3403a)
- 🟡 **미커밋 (테스트 green, FE 전체 verify 진행 중)**: `components/shell/` AppHeader(AC9 모바일 🔍·AC12 슬롯)·AppBottomTabs+bottomTabs.js(AC5)·NavDrawer(Reka Dialog)·useLogout.js, `layouts/` AppShell·GuestShell(AC6)·SkipLink(AC14), `App.vue` 셸 선택(AC7), route `wide: true`(analysis-detail·team-understanding), `src/__tests__/App.spec.js` 새 셸 기준 재작성(AC2·AC6·AC7·AC13·AC14), `frontend/CLAUDE.md` 메뉴·셸 규칙

## 바로 다음 할 일

1. (완료) 커밋·push. 기존 헤더 테스트 2개(AnalysisListView AC16, UserProfileView AC9)는 새 셸 셀렉터로 고침
2. ui-verifier: FE 5173·BE 8080 떠 있음. 1280·800·375 × 비회원(`/concepts`)·회원(`/dashboard`, `/analyses/1` wide)·ADMIN(`/admin`) × 라이트·다크. 확인: 사이드바 툴팁(800), 접기(1280), 하단 탭이 본문 가리지 않음(375), 드로어 포커스, 건너뛰기 링크 포커스 링. 로그인 storage는 `.harness/auth.json`(테스트 계정 `ui-f29a-…@example.com`)
3. spec 체크리스트 갱신 → 화면 설계서 2-1절 스크린샷 3폭 → `/pr-review` (PR에서 김동환 @멘션: A route 파일 5개 `meta.nav` group·icon + analysis·team `wide`)

## 막힌 점 / 결정 필요
- 없음. GMS 키 만료 12/09 18:00 → EV-2 전에 재발급·연장 확인

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용을 적지 않는다.** 점수·개수·SHA만
- 공통 코드 변경 → PR에서 김동환 @멘션: A route 파일 5개(`meta.nav` group·icon, analysis·team `wide`), `App.vue`, `layouts/`, `components/shell/`, `utils/navItems.js`·`navIcons.js`·`searchTargets.js`, CODEOWNERS
- push는 pre-push가 미커밋 FE 변경이 있으면 거부한다 → 커밋 후 push, push 중엔 작업 트리 건드리지 않기
- Figma는 공동 세션 전까지 건드리지 않는다
