# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-27 15:11 · 작업자: JoonseobKim · PC: js-ssafy · 브랜치: feature/F-29b-app-shell

## 현재 기능

**F-29b 앱 셸** `docs/specs/F-29b-app-shell.md` (APPROVED, AC14) · 브랜치 `feature/F-29b-app-shell` (develop a234309에서, F-29a PR #244 머지됨) · ~12/02 넘겨주기. PR 본문 `Roadmap: F-29`

## 진행

- ✅ AC1 `navGroups()`·`NAV_GROUPS` (`utils/navItems.js`), `utils/navIcons.js`(`navIcon()`, 모르면 Circle), route 9개 `meta.nav`에 `group`·`icon` (A 파일 5개: dashboard·analysis·reviewplan·team·challenge)
- ✅ AC2·AC3 `components/shell/AppSidebar.vue`(+`sidebarState.js` `vibenote.sidebar`, `activeNavPath()` 가장 긴 경로), AC4는 CSS(64px·툴팁 `data-tooltip`)라 ui-verifier로 확인 남음. CODEOWNERS에 shell·navIcons·searchTargets 추가
- ✅ AC8·AC9 `HeaderSearch.vue`+`utils/searchTargets.js`(`searchLocation()`), AC10 `ThemeToggle.vue`+`themeCycle.js`(2단계, 결정로그·spec AC10 수정), AC11 `UserMenu.vue`(Reka DropdownMenu, 복원 중 자리만). 640 미만 🔍 펼침(AC9 뒷부분)은 AppHeader에서
- 이번 세션 목표: AC1 → AC2·AC3(`AppSidebar`) → AC8·AC9(`HeaderSearch`·`searchTargets`) → AC11·AC12 → AC7(App 셸 선택)

## 바로 다음 할 일

1. AC12·AC9(<640 🔍): `components/shell/AppHeader.vue` (로고·`HeaderSearch`(sm 이상)·🔍 버튼(sm 미만, 헤더 아래 검색 줄)·`#notifications` 빈 슬롯·`ThemeToggle`·`#account` 슬롯) + `AppHeader.spec.js`
2. AC5 `AppBottomTabs.vue`(5칸)·`NavDrawer.vue`(Reka Dialog, 그룹 메뉴+프로필·설정·관리자·로그아웃), AC6 `GuestShell.vue`, `AppShell.vue`, AC7 `App.vue` 셸 선택(`meta.layout`·`meta.wide`, `/analyses/:id`·팀 분석 상세 `wide: true`) + `src/__tests__/App.spec.js` 새 셸 기준으로 고치기, AC14 건너뛰기 링크

## 막힌 점 / 결정 필요
- GMS 키 만료 12/09 18:00 → EV-2 전에 재발급·연장 확인

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용을 적지 않는다.** 점수·개수·SHA만
- 공통 코드 변경 → PR에서 김동환 @멘션: A route 파일 5개 `meta.nav`(group·icon 두 키만), `App.vue`, `layouts/`, `components/shell/`, `utils/navItems.js`
- Figma는 공동 세션 전까지 건드리지 않는다
