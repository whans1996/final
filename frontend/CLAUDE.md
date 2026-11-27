# Frontend 규칙

> Vue 3 (Composition API, `<script setup>`), Vite 8, Pinia 4, Vue Router 5, Axios, JavaScript, Tailwind CSS v4, Reka UI, lucide-vue-next. 린트는 oxlint + ESLint, 포맷은 Prettier(세미콜론 없음, 작은따옴표, 100자).
> 공통 API 규칙(URL, 필드 이름, 에러 형식)은 `../docs/conventions.md`를 따른다.
> create-vue로 생성했다 (F-00). 이미 있는 것: `api/http.js`(`errorMessage`·`errorCode` 포함), `stores/auth.js`, `router/index.js`의 `authGuard`, `HomeView`·`NotFoundView`, `assets/main.css` 변수.

## 폴더 구조

```
src/
├─ api/          모든 HTTP 호출. 도메인별 파일 (auth.js, concepts.js, analyses.js ...)
│  └─ http.js    Axios 인스턴스, 인터셉터 (토큰 첨부, 401 → refresh → 1회 재시도)
├─ stores/       Pinia (지금은 auth.js 하나뿐. user.js는 없다)
├─ router/       index.js(합치기·인증 가드), routes/<도메인>.js (route 배열)
├─ layouts/      AppShell.vue(회원 셸)·GuestShell.vue(비회원 셸)·SkipLink.vue (F-29b), AdminLayout.vue (/admin 공통 레이아웃)
├─ views/        화면 단위 컴포넌트 XxxView.vue (라우트와 1:1)
├─ components/   재사용 컴포넌트 (도메인별 하위 폴더, 공통 UI common/, 앱 셸 조각 shell/)
├─ composables/  useXxx.js (폴링, 페이지네이션, 최신 요청만 반영 useLatestRequest 등 재사용 로직)
└─ utils/
```

## 규칙

- **HTTP 호출은 `src/api/`에서만** 한다. 컴포넌트와 store에서 axios를 직접 import하지 않는다. `contract-checker`가 이 폴더를 기준으로 BE와 비교한다.
- API 함수는 spec의 API 계약과 **URL, 메서드, 필드 이름이 정확히 같아야** 한다. 응답을 가공하는 변환은 store나 composable에서 한다.
- 에러는 `error.response.data.message`를 사용자에게 보여 준다 (BE가 한국어 메시지를 보낸다). 코드 분기는 `error.response.data.code`로 한다.
- 모든 목록 화면은 **로딩 / 빈 목록 / 에러** 세 가지 상태를 처리한다.
- 로그인 상태, Access 토큰, 사용자 수준(level)은 `useAuthStore`에서 관리한다. Access 토큰은 **메모리에만** 두고(localStorage 금지), Refresh 토큰은 HttpOnly 쿠키라 JS가 다루지 않는다. 앱 시작 시 `restore()`가 refresh로 세션을 복원한다. `http.js`는 401 `AUTH_401`이면 `auth.refresh()`(동시 호출 1회) 후 1회 재시도하고, 실패하면 `setUnauthorizedHandler`로 등록한 로그인 화면 이동을 부른다 (F-02).
- **라우트는 `router/routes/<도메인>.js`에 둔다** (F-00b). `index.js`는 합치기만 하고, 기능 PR은 자기 도메인 파일만 고친다 (`analysis.js`·`challenge.js`·`dashboard.js`·`project.js`·`reviewplan.js`·`team.js` A, `community.js` B, `concept.js`·`user.js`·`auth.js`·`admin.js` 공통. 영역 정본은 `.github/CODEOWNERS`). `not-found`는 항상 마지막이다.
- **메뉴는 route의 `meta.nav = { label, order, show: 'all' | 'guest' | 'user' | 'admin', group?, icon? }`**로 정한다 (`utils/navItems.js`, `user`는 ADMIN에게도 보인다). `group`(`dashboard`·`learn`·`together`·`prepare`, `NAV_GROUPS`)이 있으면 회원 옆 메뉴·비회원 윗쪽 메뉴·드로어에 그룹으로 나오고(`navGroups()`), `icon`은 `utils/navIcons.js`의 Lucide 이름(새 아이콘은 맵에 한 줄). `group` 없는 항목(로그인·회원가입·관리자)은 계정 자리에 나온다. 셸·헤더에 링크를 직접 추가하지 않는다.
- **앱 셸** (F-29b, `layouts/AppShell.vue`·`GuestShell.vue`, 조각은 `components/shell/`): `App.vue`가 로그인이면 AppShell(헤더 + 옆 메뉴 640~1024 아이콘만·1024 이상 240px 접기 + 640 미만 하단 탭 `bottomTabs.js`·더보기 드로어), 아니면 GuestShell을 고른다. route `meta.layout: 'bare'`면 셸 없음, `meta.wide: true`면 본문 최대 폭 1440px(기본 1080px). 헤더 검색 대상은 `utils/searchTargets.js` 배열(keyword API가 생기면 한 줄), 알림은 `AppHeader` `#notifications` 슬롯. 본문은 셸의 `<main id="main">`이고 셸이 "본문으로 건너뛰기" 링크를 단다. 화면(View)은 `<main>`을 쓰지 않는다 (랜드마크 중복).
- **관리자 화면은 `/admin` 자식 route**다. 각 도메인 파일이 `export const adminChildren = [...]`로 내보내면 `admin.js`가 합친다. 자식에 `name`과 `meta.adminNav = { label, order }`를 달면 `AdminLayout` 사이드 메뉴에 나온다. `requiresAuth`·`requiresAdmin`은 부모에서 상속되므로 다시 달지 않는다.
- 공용 컴포넌트: 카드 요약 `components/concept/ConceptCard.vue`(BE `ConceptSummaryResponse` 그대로), 페이지 이동 `components/common/PageNav.vue`(`page`·`size`·`total`, `@change`), 빈 목록 `components/common/EmptyState.vue`(`message`, `#action` 슬롯).
- 공통 UI 컴포넌트 (F-29a, `components/common/`, 미리보기는 개발 서버 `/dev/components`): `BaseButton`(`variant` primary·secondary·ghost·danger, `size` sm·md, `loading`, `disabled`, `to`면 RouterLink), `FormField`(`label`·`error`·`hint`·`required`, 기본 슬롯이 `{ id, describedBy, invalid, required }`를 받아 입력에 단다), `AsyncState`(`loading`·`error` 문구·`empty`·`emptyMessage`, `#skeleton`·`#empty-action` 슬롯, `@retry`), `BaseBadge`(`tone`), `BaseCard`(`as`, `interactive`, `#header`·`#footer`), `BaseTabs`(Reka Tabs, `v-model`, `items=[{value,label}]`, 탭별 슬롯), `ConfirmDialog`(Reka Dialog, `v-model:open`, `title`·`message`·`confirmLabel`·`danger`·`loading`, `@confirm`은 닫지 않음·`@cancel`은 닫음). 아이콘은 `lucide-vue-next`에서 하나씩 import한다.
- 테마는 `composables/useTheme.js`(`theme`·`resolved`·`setTheme('light'|'dark'|'system')`, `localStorage` `vibenote.theme`). 다크는 `<html class="dark">`이고 `index.html` 인라인 스크립트가 같은 규칙으로 먼저 붙인다. 화면 이전이 끝날 때까지 `FOLLOW_SYSTEM=false`라 `system`은 라이트로 본다 (11-27 결정, 옮기지 않은 화면의 하드코딩 색 때문). 다시 켤 때는 `useTheme.js`와 `index.html`을 같이 바꾼다.
- 로그인이 필요한 화면은 라우트에 `meta.requiresAuth`, 로그인·가입처럼 비로그인 전용은 `meta.guestOnly`를 붙인다. 돌아갈 주소는 `redirectTarget(route.query)`로 꺼낸다 (외부 주소 차단).
- 폼 검증 실패는 서버 `details`를 `errorDetails(error)`로 꺼내 필드 아래에 보여 준다.
- 에러 메시지는 `errorMessage(error)`, 분기는 `errorCode(error)`(`@/api/http`)로 꺼낸다.
- **대상(id·검색어·쪽)이 바뀌거나 화면을 떠날 수 있는 조회는 `composables/useLatestRequest.js`로** 한다 (V-4): `const req = latest.begin()` → 응답·에러·finally에서 `if (!req.isCurrent()) return`. 이전 요청은 취소(abort)되고 컴포넌트가 사라지면 무효가 된다. `signal`은 받는 api(`fetchConcepts`·`fetchAdminConcepts`·`fetchPosts`)에만 넘긴다. `let lastRequest = 0` 같은 순번 가드를 직접 쓰지 않는다 (`latestRequestAdoption.spec.js`가 검사).
- 분석 진행 상태는 `composables/usePolling.js`로 2~3초 간격 폴링하고, `DONE`/`FAILED`이거나 화면을 떠나면 멈춘다.
- mermaid 렌더링(F-21)은 해당 화면에서만 동적 import한다.
- **스타일은 Tailwind 클래스 + 토큰**으로 쓴다 (F-29a). 토큰 정본은 `src/assets/main.css`(`@theme inline`)와 `docs/specs/F-29a-design-foundation.md` 5.2절: 색 `primary`·`primary-soft`·`accent`·`highlight`·`bg`·`surface`·`surface-raised`·`text`·`text-muted`·`border`·`code-bg`·`code-text`·`on-solid`·`success|warning|info|danger`(+`-soft`), 글자 `text-xs|sm|base|lg|xl|hero`, 굵기 `font-normal|semibold|bold`, 반경 `rounded-sm|md|full`, 그림자 `shadow-1|2`, 반응형 `sm:`(640)·`lg:`(1024)만. Tailwind 기본 팔레트(`indigo-500` 등)는 지웠다. 다크는 토큰이 바꾸므로 대개 `dark:`가 필요 없다.
- **새 화면·컴포넌트에 `<style scoped>`를 쓰지 않는다.** 예외: 토큰으로 안 되는 애니메이션·mermaid·코드 하이라이트. 아직 옮기지 않은 화면의 scoped 스타일과 별칭 변수(`--color-primary`·`--color-background`·`--space-*`·`--radius`)는 각자 개편 PR에서 Tailwind로 옮길 때 지운다.
- preflight가 켜져 있고 `main.css` `@layer base`가 h1~h3 크기·h1~h6 굵기, 링크 색·밑줄, 목록 들여쓰기·불릿, **클래스 없는 `<button>`의 브라우저 기본 모양**을 되돌린다 (Tailwind로 그리는 버튼은 클래스가 있어 영향 없음). 새 코드에서 필요 없으면 `no-underline`·`list-none`·`ps-0`처럼 클래스로 끈다.

## 테스트

- Vitest + Vue Test Utils. 테스트 파일은 대상 옆이나 `src/**/__tests__/`에 `xxx.spec.js`로 둔다.
- API는 `vi.mock('@/api/xxx')`로 대체한다. 실제 백엔드에 의존하는 단위 테스트를 쓰지 않는다.
- **테스트 이름에 spec의 AC 번호를 넣는다**: `it('AC3 빈 목록이면 안내 문구를 보여 준다', ...)`
- 실제 화면 렌더링 확인은 `ui-verifier` 에이전트(`node scripts/ui-check.mjs`, Playwright)로 한다. PC마다 1회 `npx playwright install chromium`이 필요하다.
- API 모킹 예: `http.spec.js`처럼 `http.defaults.adapter`를 바꾸거나 `vi.mock('@/api/xxx')`를 쓴다.

## 명령어 (frontend 폴더에서)

| 목적 | 명령 |
|---|---|
| 개발 서버 | `npm run dev` (http://localhost:5173, `/api`는 8080으로 프록시) |
| 단위 테스트 | `npm run test:unit -- --run` |
| 린트 | `npm run lint` (자동 수정) / `npm run lint:check` (검사만, CI·verify가 사용). oxlint는 `scripts/run-oxlint.mjs`로 부른다: 스마트 앱 컨트롤이 네이티브를 막는 PC는 Docker로, Docker도 없으면 건너뜀 (`docs/환경설정.md` 5절) |
| 빌드 | `npm run build` |

루트에서 `node scripts/verify.mjs fe`를 쓰면 린트, 테스트, 빌드를 돌리고 실패만 요약해 준다.
Edit/Write 후에는 hook이 해당 파일에 Prettier를 자동 적용한다.
