# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-27 15:01 · 작업자: JoonseobKim · PC: js-ssafy · 브랜치: feature/F-29b-app-shell

## 현재 기능

**F-29b 앱 셸** `docs/specs/F-29b-app-shell.md` (APPROVED, AC14) · 브랜치 `feature/F-29b-app-shell` (develop a234309에서, F-29a PR #244 머지됨) · ~12/02 넘겨주기. PR 본문 `Roadmap: F-29`

## 진행

- ✅ AC1 `navGroups()`·`NAV_GROUPS` (`utils/navItems.js`), `utils/navIcons.js`(`navIcon()`, 모르면 Circle), route 9개 `meta.nav`에 `group`·`icon` (A 파일 5개: dashboard·analysis·reviewplan·team·challenge)
- 이번 세션 목표: AC1 → AC2·AC3(`AppSidebar`) → AC8·AC9(`HeaderSearch`·`searchTargets`) → AC11·AC12 → AC7(App 셸 선택)

## 바로 다음 할 일

1. AC2·AC3: `src/components/shell/__tests__/AppSidebar.spec.js` red (그룹 제목·아이콘·이름, `aria-current="page"`, 하위 경로 `/analyses/12` → 내 분석 활성, 접기 → `localStorage` `vibenote.sidebar`) → `src/components/shell/AppSidebar.vue` (240px / 접힘 64px, `navGroups`·`navIcon`)
2. 그다음 `HeaderSearch.vue`+`utils/searchTargets.js` (AC8·AC9), `ThemeToggle`, `UserMenu`(Reka `DropdownMenu`), `AppHeader`(`#notifications` 슬롯), `AppBottomTabs`·`NavDrawer`(Reka `Dialog`), `AppShell`·`GuestShell`, `App.vue` 셸 선택 + `App.spec.js` 고치기

## 막힌 점 / 결정 필요
- **AC10 결정 필요**: 토글은 라이트→다크→OS 3단계인데 `FOLLOW_SYSTEM=false`라 OS=라이트로 보여 눌러도 안 바뀜. 안: 라이트↔다크 2단계(FOLLOW_SYSTEM 켜면 3단계 복귀) + 결정로그. AC10 전에 사용자 확인
- GMS 키 만료 12/09 18:00 → EV-2 전에 재발급·연장 확인

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용을 적지 않는다.** 점수·개수·SHA만
- 공통 코드 변경 → PR에서 김동환 @멘션: A route 파일 5개 `meta.nav`(group·icon 두 키만), `App.vue`, `layouts/`, `components/shell/`, `utils/navItems.js`
- Figma는 공동 세션 전까지 건드리지 않는다
