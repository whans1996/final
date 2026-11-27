# F-29b 앱 셸 (회원 옆 메뉴 3폭·비회원 윗쪽 메뉴·헤더 검색·메뉴 그룹)

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-29b (로드맵 `F-29`의 뒤 절반, 12/02 넘겨주기. PR 본문 `Roadmap: F-29`) |
| 우선순위 | T0 |
| 영역 오너 | B 김준섭 (공통 셸. A 영역 route 파일의 `meta.nav`를 고치므로 PR에서 김동환 @멘션) |
| 관련 필수 요구사항 | ⑬ (화면·UI) |
| 의존 spec | F-29a (토큰·컴포넌트·`useTheme`), F-00b (`meta.nav`·`navItems`) |
| 관련 이슈 | 없음 |
| 승인 | 김준섭 2026-11-27 |

## 1. 목적

헤더 한 줄에 메뉴 11개가 들어가지 않고(11-27), 모바일에서 복습·퀴즈를 엄지로 바로 누를 수 없다.
화면 설계서 2-1절의 앱 셸(회원 옆 메뉴 3폭, 비회원 윗쪽 메뉴, 헤더 검색·다크 토글·닉네임 메뉴)을 만들어 12/02에 두 사람의 화면 개편이 같은 틀에서 시작하게 한다.
메뉴는 계속 각 route의 `meta.nav`로 정한다 (F-00b 방식 유지, `group`·`icon`만 추가).

## 2. 수용 기준 (Acceptance Criteria)

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | route에 `meta.nav = { label, order, show, group, icon }`이 있을 때 | `navGroups(routes, auth)`를 부르면 | `dashboard`·`learn`·`together`·`prepare` 순서의 그룹 배열 `[{ key, label, items: [{ label, to, icon }] }]`을 돌려준다. 그룹 안은 `order` 순, 보이는 항목이 없는 그룹은 빠진다. `group`이 없는 항목(로그인·회원가입·관리자)은 그룹에 들어가지 않는다. 기존 `navItems()` 결과는 바뀌지 않는다 |
| AC2 | 로그인한 회원이 폭 ≥1024에서 | 아무 회원 화면을 열면 | 왼쪽에 그룹 제목 + 아이콘 + 이름의 옆 메뉴, 위에 얇은 헤더(로고·검색·알림 자리·다크 토글·닉네임 메뉴)가 보이고 지금 경로의 메뉴에 `aria-current="page"`가 붙는다. 하위 경로(`/analyses/12`)도 상위 메뉴(`내 분석`)가 활성이다 |
| AC3 | 폭 ≥1024에서 | 접기 버튼을 누르면 | 옆 메뉴가 아이콘만 남고(이름은 `title`·`aria-label`), 상태가 `localStorage` `vibenote.sidebar`에 저장돼 새로고침 뒤에도 유지된다 |
| AC4 | 폭 640~1024에서 | 회원 화면을 열면 | 옆 메뉴가 항상 아이콘만 보이는 좁은 메뉴이고, 마우스를 올리거나 포커스하면 이름 툴팁이 보인다. 접기 버튼은 없다 |
| AC5 | 폭 <640에서 | 회원 화면을 열면 | 옆 메뉴 대신 하단 탭 5칸(대시보드 `/dashboard`·분석 `/analyses`·복습 `/review-plans`·카드 `/concepts`·더보기)이 고정되고 본문이 탭에 가리지 않는다. 더보기를 누르면 드로어에 나머지 메뉴(그룹별)와 프로필·설정·관리자(ADMIN만)·로그아웃이 열리고, 메뉴를 고르거나 Esc·바깥을 누르면 닫힌다 |
| AC6 | 비회원이 | 회원 셸 밖 화면(개념 카드·커뮤니티·챌린지·로그인)을 열면 | 옆 메뉴 없이 윗쪽 메뉴(로고·개념 카드·커뮤니티·챌린지·검색·다크 토글·로그인·회원가입)가 보인다. 640 미만에서는 메뉴가 햄버거 드로어로 접힌다 |
| AC7 | route에 `meta.layout`이 | `'bare'`이면(랜딩용, F-29b에서는 실제 사용 route 없음) / `meta.wide: true`이면 | bare는 셸 없이 본문만 전체 폭으로, wide는 본문 최대 폭 1440px, 그 밖에는 1080px로 그린다. `/analyses/:id`·팀 분석 상세는 `wide: true` |
| AC8 | 헤더 검색에서 대상 `개념 카드`·카테고리 `Spring`·검색어 `트랜잭션`을 넣고 | Enter를 누르면 | `/concepts?keyword=트랜잭션&category=spring`으로 이동한다. 대상 `커뮤니티`면 `/community?keyword=트랜잭션`. 카테고리 선택은 대상이 개념 카드일 때만 보인다. 검색어 앞뒤 공백은 지우고 빈 검색어면 이동하지 않는다 |
| AC9 | 검색 대상 목록이 | 렌더링되면 | 지금 `keyword` 검색 API가 있는 대상(개념 카드 `GET /api/concepts`, 커뮤니티 `GET /api/posts`)만 보인다. 대상 정의는 `utils/searchTargets.js` 배열 한 곳이고, 챌린지·팀은 API가 생기면 이 배열에 한 줄을 더한다. 640 미만에서는 🔍 버튼을 누르면 검색 줄이 헤더 아래로 펼쳐진다 |
| AC10 | 헤더의 다크 토글을 | 누르면 | 라이트 → 다크 → OS 순으로 바뀌고 아이콘(Sun·Moon·Monitor)과 `aria-label`("테마: 다크")이 현재 값을 알린다 (`useTheme` 사용) |
| AC11 | 닉네임 메뉴를 | 열면 | 프로필(`/users/:id`)·설정(`/me/settings`)·관리자(ADMIN만)·로그아웃이 보이고, 로그아웃하면 `home`으로 이동한다 (지금 App의 동작 유지) |
| AC12 | 헤더에 | 알림 자리가 | `AppHeader`의 `#notifications` 슬롯으로만 있고 F-29b에서는 아무것도 그리지 않는다 (T1이 채운다) |
| AC13 | `/admin` 아래 화면을 | ADMIN이 열면 | 회원 셸 안에 `AdminLayout`(관리자 사이드 메뉴)이 본문으로 들어가 기존 관리자 기능이 그대로 동작한다 (`AdminLayout.spec.js` 통과) |
| AC14 | 셸 전체를 | 키보드만으로 쓰면 | 첫 Tab에 "본문으로 건너뛰기" 링크가 보이고, 메뉴·검색·토글·닉네임 메뉴·드로어에 포커스 링이 보이며 드로어는 포커스를 가둔다 |

## 3. API 계약

없음. 검색은 기존 API의 목록 화면으로 쿼리를 넘기기만 한다 (`ConceptListView`·`PostListView`가 이미 `route.query.keyword`·`category`를 읽는다).

## 4. DB 변경

없음.

## 5. 프론트엔드

### 5.1 메뉴 그룹 (`meta.nav`에 `group`·`icon` 추가)

| 그룹 key (label) | 항목 (route 파일 · 오너) | icon (Lucide) |
|---|---|---|
| `dashboard` (대시보드) | 대시보드 (`dashboard.js` · A) | `LayoutDashboard` |
| `learn` (학습) | 분석 요청 · 내 분석 (`analysis.js` · A), 복습 (`reviewplan.js` · A), 개념 카드 (`concept.js` · 공통). 프로젝트는 P-1 route가 생길 때 A가 `group: 'learn'`으로 단다 | `FilePlus2`·`FileSearch`·`RotateCcw`·`BookOpen` |
| `together` (함께) | 팀 (`team.js` · A), 챌린지 (`challenge.js` · A), 커뮤니티 · 피드 (`community.js` · B) | `Users`·`Trophy`·`MessagesSquare`·`Rss` |
| `prepare` (준비) | 면접 준비는 I-1 route가 생길 때 A가 단다. 그 전까지 이 그룹은 보이지 않는다 (AC1) | `Mic` |

- 그룹 순서·label은 `utils/navItems.js`의 `NAV_GROUPS` 상수. `icon`은 문자열 이름이고 셸이 `utils/navIcons.js`(이름 → Lucide 컴포넌트 맵)로 그린다. 맵에 없는 이름은 `Circle`
- 기존 `order` 값은 그대로 두고 그룹 안 정렬에만 쓴다. 로그인·회원가입(`show: 'guest'`)·관리자(`show: 'admin'`)에는 `group`을 달지 않는다 (비회원 윗쪽 메뉴·닉네임 메뉴가 쓴다)
- A 영역 route 파일 5개는 `meta.nav`에 `group`·`icon` 두 키만 더한다 (PR 설명에 목록, 김동환 @멘션)

### 5.2 파일

| 항목 | 내용 |
|---|---|
| 셸 | `src/layouts/AppShell.vue` (회원: `AppSidebar` + `AppHeader` + 본문 + `AppBottomTabs`), `src/layouts/GuestShell.vue` (윗쪽 메뉴). `App.vue`는 `meta.layout`·로그인 여부로 셋 중 하나(`AppShell`·`GuestShell`·bare)만 고른다 |
| 셸 조각 | `src/components/shell/` `AppHeader.vue`·`AppSidebar.vue`·`AppBottomTabs.vue`·`NavDrawer.vue`(Reka UI `Dialog`)·`HeaderSearch.vue`·`ThemeToggle.vue`·`UserMenu.vue`(Reka UI `DropdownMenu`) |
| 유틸 | `utils/navItems.js`에 `navGroups()`·`NAV_GROUPS`, `utils/navIcons.js`, `utils/searchTargets.js` (`[{ key: 'concepts', label: '개념 카드', to: '/concepts', filters: ['category'] }, { key: 'posts', label: '커뮤니티', to: '/community' }]`) |
| 셸 선택 규칙 | 비회원 → GuestShell, 회원 → AppShell, `meta.layout === 'bare'` → 셸 없음. 회원이 개념 카드·커뮤니티 같은 공개 화면을 봐도 AppShell이다 |
| 폭 | 본문 래퍼 `max-w-[1080px]`, `meta.wide`면 `max-w-[1440px]`. 옆 메뉴 240px / 접힘·좁은 메뉴 64px / 하단 탭 높이 56px + `env(safe-area-inset-bottom)` |
| 문서 | `frontend/CLAUDE.md` 메뉴 규칙 줄: `meta.nav = { label, order, show, group?, icon? }`, `meta.layout`·`meta.wide` |
| 상태별 UI | 셸은 데이터를 부르지 않는다. 닉네임은 `auth.user`가 아직 없으면(복원 중) 자리만 둔다 |

## 6. LLM

해당 없음.

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1 | `src/utils/__tests__/navItems.spec.js` (`navGroups` 그룹 순서·빈 그룹 제외·group 없는 항목 제외, 기존 `navItems` 테스트 유지) | Vitest |
| AC2·AC3 | `src/components/shell/__tests__/AppSidebar.spec.js` (활성·하위 경로·접기 저장) | Vitest + VTU |
| AC4·AC5·AC6 | ui-verifier: 1280·800·375 × 회원·비회원 스크린샷, `AppBottomTabs.spec.js`(5칸 경로)·`NavDrawer.spec.js`(열기·닫기) | 화면 + Vitest |
| AC7 | `src/__tests__/App.spec.js` (bare·wide·기본 폭, 회원/비회원 셸 선택. 기존 헤더 메뉴 테스트는 새 셸 기준으로 고친다) | Vitest |
| AC8·AC9 | `src/components/shell/__tests__/HeaderSearch.spec.js` (이동 쿼리, 카테고리 표시 조건, 공백·빈 검색어) | Vitest |
| AC10 | `ThemeToggle.spec.js` | Vitest |
| AC11·AC12 | `UserMenu.spec.js`, `AppHeader.spec.js` (슬롯 비어 있음) | Vitest |
| AC13 | 기존 `src/layouts/__tests__/AdminLayout.spec.js` + ui-verifier `/admin` | Vitest + 화면 |
| AC14 | ui-verifier: Tab 순서·포커스 링 스크린샷 | 화면 |

## 8. 범위 밖

- 회원이 `/`에 오면 대시보드로 보내기, 랜딩 전용 윗막대·푸터 → **LP-1** (F-29b는 `meta.layout: 'bare'` 장치만)
- 알림 아이콘·드롭다운·API → **T1** (12/09~12/14, 슬롯만)
- 챌린지·팀 검색 (keyword API 없음), 통합 검색 결과 화면
- K-1 카테고리 12종: 검색 카테고리는 `utils/conceptCategories.js`를 그대로 읽으므로 K-1이 그 상수를 바꾸면 따라온다
- 각 화면 본문의 Tailwind 이전·개편 (영역별 PR)
- 옆 메뉴 그룹 접기·펼치기, 메뉴 순서 사용자 설정

## 9. 결정 필요

- (없음. 결정로그 11-27 "앱 셸" + spec 질문 답(하단 탭 '분석' = 내 분석, 알림은 슬롯만) 반영)

## 10. 구현 체크리스트

- [x] F-29a 머지 확인 → develop에서 브랜치 `feature/F-29b-app-shell`
- [ ] AC별 실패 테스트 작성 (red 확인)
- [ ] `navGroups`·`navIcons`·`searchTargets`, route 파일 `group`·`icon`
- [ ] 셸 조각 7개 + `AppShell`·`GuestShell`·`App.vue` 셸 선택
- [ ] `frontend/CLAUDE.md` 메뉴·layout 규칙
- [ ] ui-verifier 통과 (1280·800·375, 회원·비회원·ADMIN, 라이트·다크)
- [ ] reviewer 지적 사항 반영
- [ ] 산출물 반영: 화면 설계서 2-1절 스크린샷 3폭 (PR 스크린샷 `pr-screens.mjs`)
- [ ] PR CI 초록 (김동환 @멘션: A route 파일 5개 `meta.nav`)
- [ ] 상태 DONE (PR 머지 후)
