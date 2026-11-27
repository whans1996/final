# F-29a 디자인 기반 (Tailwind v4·토큰·폰트·다크 토글·공통 컴포넌트 7개)

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-29a (로드맵 `F-29`의 앞 절반. PR 본문 `Roadmap: F-29`) |
| 우선순위 | T0 |
| 영역 오너 | B 김준섭 (공통 FE 기반. 머지 뒤 A 김동환도 쓴다 → PR에서 @멘션) |
| 관련 필수 요구사항 | ⑬ (화면·UI) |
| 의존 spec | F-00b (공통 기반) |
| 관련 이슈 | 없음 |
| 승인 | 김준섭 2026-11-27 |

## 1. 목적

인디고 기본 색·화면마다 다른 scoped CSS 때문에 화면이 'AI가 만든 것'처럼 보이고, 다크 모드가 없다 (11-25·11-27 결정).
11/27 Figma 세션에서 정한 토큰·폰트를 Tailwind v4 테마로 고정하고, 두 사람이 화면 개편(12/03~)에 바로 쓸 공통 컴포넌트 7개를 만든다.
앱 셸·검색·메뉴 그룹은 F-29b(12/02)가 이 기반 위에 만든다. F-29a를 먼저 머지(~11/30)해 A가 컴포넌트를 일찍 쓸 수 있게 한다.

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `it('AC3 ...')`

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | Tailwind v4(`@tailwindcss/vite`)·`reka-ui`·`lucide-vue-next`·폰트 패키지를 설치한 FE가 | `npm run build`·`lint:check`·`test:unit`을 돌리면 | 모두 통과하고, 기존 단위 테스트는 하나도 고치지 않고 통과한다 |
| AC2 | `main.css`의 `@theme`에 4절 토큰이 정의되어 있고 | 컴포넌트가 `bg-primary`·`text-danger`·`rounded-md`·`text-hero`·`shadow-1` 같은 클래스를 쓰면 | 4절 값으로 그려진다. 기존 CSS 변수(`--color-primary`·`--color-text`·`--color-danger`·`--radius` 등)는 새 토큰을 가리키는 별칭으로 남아 옮기기 전 화면도 새 색으로 보인다 |
| AC3 | preflight가 켜진 상태에서 | 아직 Tailwind로 옮기지 않은 기존 화면(예: 개념 카드 목록, 분석 상세)을 열면 | `@layer base` 보정으로 h1~h3 크기·굵기, 링크 색, 목록 들여쓰기가 남아 레이아웃이 깨지지 않는다 (ui-verifier 스크린샷으로 확인) |
| AC4 | 처음 방문한 사용자가 (저장된 테마 없음) | OS가 다크 모드이면 | `<html>`에 `dark` 클래스가 붙어 다크 토큰으로 보이고, 라이트이면 붙지 않는다. OS 설정이 바뀌면 새로고침 없이 따라간다 |
| AC5 | `useTheme()`로 테마를 `light`·`dark`·`system` 중 하나로 고르면 | 즉시 반영되고 `localStorage`의 `vibenote.theme`에 저장되며 | 새로고침 뒤에도 유지된다. `index.html`의 인라인 스크립트가 앱 마운트 전에 클래스를 붙여 라이트→다크 깜빡임이 없다. 저장값이 세 값이 아니면 `system`으로 본다 |
| AC6 | 본문·코드 폰트가 | 네트워크가 끊긴 상태(시연장)에서 열어도 | Pretendard Variable·JetBrains Mono가 빌드 결과물(`dist/assets`)에서 로드된다. 외부 CDN 요청이 없다 |
| AC7 | `Button`에 `variant`(`primary`·`secondary`·`ghost`·`danger`)·`size`(`sm`·`md`)·`loading`·`disabled`·`type`을 주면 | 렌더링·클릭하면 | variant별 클래스가 붙고, `loading`·`disabled`면 `click`을 내보내지 않고 `aria-busy`/`disabled`가 붙는다. `type` 기본값은 `button`이다. `to`를 주면 `RouterLink`로 그린다 |
| AC8 | `FormField`에 `label`·`error`·`hint`·`required`를 주고 기본 슬롯에 입력을 넣으면 | 렌더링하면 | label과 입력이 `for`/`id`로 연결되고, `error`가 있으면 입력 아래에 빨간 문구 + 입력에 `aria-invalid="true"`·`aria-describedby`가 붙는다. `error`가 없으면 `hint`를 보인다 |
| AC9 | `AsyncState`에 `loading`·`error`·`empty`·`emptyMessage`를 주면 | 각 상태에서 | loading이면 `#skeleton` 슬롯(없으면 스피너 + "불러오는 중…"), error면 메시지 + "다시 시도" 버튼(`retry` 이벤트), empty면 기존 `EmptyState`와 같은 문구·`#empty-action` 슬롯, 그 밖에는 기본 슬롯을 보인다. 우선순위는 loading > error > empty |
| AC10 | `Badge`(`tone`: `neutral`·`primary`·`success`·`warning`·`info`·`danger`·`highlight`)와 `Card`(`as`, `interactive`, `#header`·`#footer` 슬롯)를 | 라이트·다크에서 렌더링하면 | tone별로 4절 상태 색 쌍(글자/바탕)이 쓰이고 다크에서는 다크 쌍이 쓰인다. `interactive` Card는 hover·focus 링이 보인다 |
| AC11 | `Tabs`(Reka UI `TabsRoot` 감쌈, `v-model`, `items=[{value,label}]`, 탭별 슬롯)에서 | 탭을 클릭하거나 ←/→ 키를 누르면 | 선택이 바뀌고 `update:modelValue`가 나가며, `role="tablist"`/`tab`/`tabpanel`과 `aria-selected`가 맞다 |
| AC12 | `ConfirmDialog`(Reka UI `Dialog` 감쌈, `v-model:open`, `title`·`message`·`confirmLabel`·`danger`·`loading`)가 열렸을 때 | 확인을 누르면 `confirm`, 취소·Esc·바깥 클릭이면 `cancel` + 닫힘 | 포커스가 대화상자 안에 갇히고 닫히면 연 버튼으로 돌아간다. `loading` 동안은 두 버튼이 비활성이고 Esc로 닫히지 않는다 |
| AC13 | 공통 컴포넌트 7개를 | 다크 모드·375px 폭에서 | 글자/바탕 대비가 WCAG AA(본문 4.5:1)를 넘고 가로 스크롤이 없다 (`scripts/ui-check.mjs`로 컴포넌트 미리보기 화면 확인) |

## 3. API 계약

없음 (FE만).

## 4. DB 변경

없음.

## 5. 프론트엔드

### 5.1 설치

| 패키지 | 용도 |
|---|---|
| `tailwindcss`, `@tailwindcss/vite` | Tailwind v4 (Vite 플러그인, `tailwind.config.js` 없이 CSS `@theme`) |
| `reka-ui` | Tabs·Dialog (헤드리스, 접근성) |
| `lucide-vue-next` | 아이콘 (컴포넌트 단위 import, 트리 셰이킹) |
| `pretendard` | Pretendard Variable woff2 (다이내믹 서브셋, OFL-1.1) |
| `@fontsource-variable/jetbrains-mono` | JetBrains Mono (OFL-1.1) |

폰트는 npm 패키지를 CSS에서 import해 Vite가 `dist/assets`로 복사한다 ("저장소 포함" = CDN 없이 우리 빌드에서 서빙). 라이선스 표기는 루트 `NOTICE`를 새로 만들어 두 줄 적는다 (LP-1의 DS-02-04 Apache-2.0 표기도 이 파일에 이어 적는다).

### 5.2 토큰 (`src/assets/main.css` `@theme`. 이 표와 main.css가 정본)

| 토큰 | 라이트 | 다크 |
|---|---|---|
| `--color-primary` (딥 그린·틸) | `#0f766e` | `#2dd4bf` |
| `--color-primary-soft` | `#e6f4f2` | `#123a37` |
| `--color-accent` (코랄, 재미 화면) | `#f97362` | `#fb8b7c` |
| `--color-highlight` (형광펜) | `#fde68a` | `#a16207` 40% 투명 |
| `--color-bg` (바탕) | `#faf8f5` 오프화이트 | `#1c1a18` 차콜 |
| `--color-surface` | `#ffffff` | `#262321` |
| `--color-surface-raised` (다크 그림자 대신) | `#ffffff` | `#302c29` |
| `--color-text` / `--color-text-muted` | `#1f1d1b` / `#6b645d` | `#f2eee9` / `#a69e95` |
| `--color-border` | `#e7e2dc` | `#3a3532` |
| `--color-code-bg` / `--color-code-text` | `#f6f4f0` / `#1f1d1b` | `#141211` / `#e8e3dc` |
| success 글자/바탕 | `#3f7d3a` / `#e8f1e3` | `#8fca86` / `#1f3320` |
| warning | `#a15c07` / `#fbefd9` | `#f0b45a` / `#3a2a10` |
| info | `#33658a` / `#e3edf4` | `#8db8d9` / `#17293a` |
| danger | `#b4232c` / `#fbe4e2` | `#f2867f` / `#3d1a1a` |

- 다크 값은 결정로그에 없어 이 spec에서 정한다 (같은 색상, 밝기만 올림). 구현 중 AC13 대비가 안 나오면 다크 값만 조정하고 이 표를 고친다
- 글자: `--text-xs` 12 · `sm` 14 · `base` 16 · `lg` 20 · `xl` 28 · `hero` 48 (640 미만 32). 굵기 400·600·700만
- 반경: `--radius-sm` 6 · `--radius-md` 12 · `full`. 그림자: `--shadow-1`(카드)·`--shadow-2`(팝오버·대화상자). 다크에서는 그림자를 약하게 하고 `surface-raised`로 높이를 보인다
- 반응형: Tailwind `sm`을 640, `lg`를 1024로 두고 나머지(`md`·`xl`·`2xl`)는 쓰지 않는다 (`--breakpoint-*: initial`로 지운다)
- 간격은 Tailwind 기본(4px 단위)을 쓴다. 기존 `--space-1~8`은 별칭으로 남긴다
- 다크 전환: `@custom-variant dark (&:where(.dark, .dark *))`. 토큰은 `:root`와 `.dark`에서 CSS 변수로 바꾸고 `@theme inline`으로 Tailwind에 연결한다

### 5.3 파일

| 항목 | 내용 |
|---|---|
| 테마 | `src/composables/useTheme.js` (`theme` ref: `light`·`dark`·`system`, `setTheme()`, `resolved` computed, `matchMedia` 구독). `index.html` `<head>` 인라인 스크립트 (같은 키·규칙, 10줄 이내) |
| 컴포넌트 | `src/components/common/` `BaseButton.vue`·`FormField.vue`·`AsyncState.vue`·`BaseBadge.vue`·`BaseCard.vue`·`BaseTabs.vue`·`ConfirmDialog.vue` (Vue 규칙상 한 단어 이름 금지라 `Base` 접두. 문서에서는 Button·Badge·Card·Tabs로 부른다) |
| 미리보기 | `/dev/components` → `views/dev/ComponentGalleryView.vue` (`import.meta.env.DEV`일 때만 route 등록, 빌드에 포함 안 됨). 7개 × 라이트·다크 토글 · ui-verifier가 이 화면을 찍는다 |
| 기존 공통 | `EmptyState.vue`·`PageNav.vue`는 Tailwind로 옮기고 API(props·slot)는 그대로 둔다 (공통 영역이라 이 PR) |
| 규칙 문서 | `frontend/CLAUDE.md` 규칙 줄 교체: "스타일은 Tailwind 클래스 + 토큰. 새 화면에 `<style scoped>`를 쓰지 않는다(예외: 토큰으로 안 되는 애니메이션·mermaid·코드 하이라이트). 공통 컴포넌트 목록" |
| 상태별 UI | AsyncState가 로딩·에러·빈 상태를 맡는다 (AC9) |

## 6. LLM

해당 없음.

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1 | `node scripts/verify.mjs fe` | 빌드·린트·전체 Vitest |
| AC2 | `src/assets/__tests__/tokens.spec.js` (main.css 텍스트에서 토큰 이름·값·별칭 존재 확인) + ui-verifier | Vitest + 화면 |
| AC3 | ui-verifier: `/concepts`, `/community`, `/analyses/:id` 스크린샷 (변경 전 develop과 비교) | 화면 |
| AC4·AC5 | `src/composables/__tests__/useTheme.spec.js` (matchMedia·localStorage 모킹, 잘못된 저장값) | Vitest |
| AC5 깜빡임 | ui-verifier: `vibenote.theme=dark` 저장 후 새로고침 첫 스크린샷이 다크 | 화면 |
| AC6 | `src/__tests__/fonts.spec.js` (main.css에 `pretendard`·`jetbrains-mono` import, `http`로 시작하는 `@import`·`url()` 없음) + 빌드 후 `dist/assets/*.woff2` 존재 | Vitest + 빌드 |
| AC7~AC12 | `src/components/common/__tests__/<컴포넌트>.spec.js` 각 1개 | Vitest + VTU |
| AC13 | ui-verifier: `/dev/components` 라이트·다크 × 1280·375 | 화면 |

## 8. 범위 밖

- 앱 셸(옆 메뉴·하단 탭·비회원 윗쪽 메뉴)·헤더 검색·다크 토글 버튼 배치·`meta.nav.group` → **F-29b**. F-29a는 기존 헤더를 그대로 두고 토글 버튼도 달지 않는다 (테마 전환은 `/dev/components`에서 확인)
- 기존 화면의 Tailwind 이전·개편 → 각자 영역의 개편 PR (CD-1·V-4·A 화면)
- 랜딩 (LP-1), 알림 (T1), GSAP 연출 (U-1)
- 코드 하이라이트 테마 색 (분석 상세 개편 때 A가 `--color-code-*` 토큰을 쓴다)
- Toast·Dropdown·Select 등 7개 밖의 컴포넌트 (필요한 PR이 만들고 공통이면 @멘션)

## 9. 결정 필요

- (없음. 11-27 결정로그 3줄 + 11/27 spec 질문 4개 반영: 두 PR 분할, preflight + base 보정, 하단 탭 '분석' = 내 분석, 알림 자리만)

## 10. 구현 체크리스트

- [ ] 브랜치 `feature/F-29-design-common` (이 브랜치에서 F-29a. F-29b는 F-29a 머지 뒤 develop에서 `feature/F-29b-app-shell`)
- [ ] 패키지 설치, `vite.config.js`에 `tailwindcss()` 플러그인
- [ ] AC별 실패 테스트 작성 (red 확인)
- [ ] `main.css` 토큰·별칭·base 보정·폰트, `index.html` 테마 스크립트, `useTheme`
- [ ] 공통 컴포넌트 7개 + EmptyState·PageNav 이전 + `/dev/components`
- [ ] NOTICE 폰트 라이선스, `frontend/CLAUDE.md` 스타일 규칙
- [ ] ui-verifier 통과 (데스크톱·모바일, 라이트·다크)
- [ ] reviewer 지적 사항 반영
- [ ] 산출물 반영: 화면 설계서 2-1절 "값은 F-29 spec" → F-29a 경로
- [ ] PR CI 초록 (A @멘션: 공통 컴포넌트)
- [ ] 상태 DONE (PR 머지 후)
