# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-27 15:25 · 작업자: JoonseobKim · PC: js-ssafy · 브랜치: feature/F-29b-app-shell

## 현재 기능

**F-29b 앱 셸** `docs/specs/F-29b-app-shell.md` (APPROVED, AC14) · 브랜치 `feature/F-29b-app-shell` (develop a234309에서, F-29a PR #244 머지됨) · ~12/02 넘겨주기. PR 본문 `Roadmap: F-29`

## 진행

- ✅ 커밋·push: AC1 navGroups·navIcons·route group/icon (e9acb91), CODEOWNERS shell (189ab5a), AC2~AC4 AppSidebar (fedbec1), AC8~AC11 HeaderSearch·searchTargets·ThemeToggle 2단계(결정로그·spec AC10 수정)·UserMenu (0e3403a)
- 🟡 **미커밋 (테스트 green, FE 전체 verify 진행 중)**: `components/shell/` AppHeader(AC9 모바일 🔍·AC12 슬롯)·AppBottomTabs+bottomTabs.js(AC5)·NavDrawer(Reka Dialog)·useLogout.js, `layouts/` AppShell·GuestShell(AC6)·SkipLink(AC14), `App.vue` 셸 선택(AC7), route `wide: true`(analysis-detail·team-understanding), `src/__tests__/App.spec.js` 새 셸 기준 재작성(AC2·AC6·AC7·AC13·AC14), `frontend/CLAUDE.md` 메뉴·셸 규칙

## 바로 다음 할 일

1. `/pr-review` (verify all → reviewer·ui 스크린샷 → 화면 설계서 2-1절 앱 셸 3폭 스크린샷 `docs/산출물/screens/` → PR 본문 `Roadmap: F-29`, 김동환 @멘션: A route 파일 5개 `meta.nav` group·icon + analysis·team `wide`, 공통 App.vue·layouts·components/shell)
2. ui-verifier 결과: 회원·비회원 1280·800·375·다크·키보드 OK. 비회원 800 검색 찌그러짐은 고침(AppHeader `inlineSearchFrom="lg"`). 미확인: ADMIN 화면(관리자 계정 없음), `/analyses/:id` wide(분석 데이터 없음). 임시 계정 `ui-f29b-31835@example.com` (로컬 DB)

## 막힌 점 / 결정 필요
- 없음. GMS 키 만료 12/09 18:00 → EV-2 전에 재발급·연장 확인

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용을 적지 않는다.** 점수·개수·SHA만
- 공통 코드 변경 → PR에서 김동환 @멘션: A route 파일 5개(`meta.nav` group·icon, analysis·team `wide`), `App.vue`, `layouts/`, `components/shell/`, `utils/navItems.js`·`navIcons.js`·`searchTargets.js`, CODEOWNERS
- push는 pre-push가 미커밋 FE 변경이 있으면 거부한다 → 커밋 후 push, push 중엔 작업 트리 건드리지 않기
- Figma는 공동 세션 전까지 건드리지 않는다
