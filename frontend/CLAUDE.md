# Frontend 규칙

> Vue 3 (Composition API, `<script setup>`), Vite 8, Pinia 4, Vue Router 5, Axios, JavaScript. 린트는 oxlint + ESLint, 포맷은 Prettier(세미콜론 없음, 작은따옴표, 100자).
> 공통 API 규칙(URL, 필드 이름, 에러 형식)은 `../docs/conventions.md`를 따른다.
> create-vue로 생성했다 (F-00). 이미 있는 것: `api/http.js`(`errorMessage`·`errorCode` 포함), `stores/auth.js`, `router/index.js`의 `authGuard`, `HomeView`·`NotFoundView`, `assets/main.css` 변수.

## 폴더 구조

```
src/
├─ api/          모든 HTTP 호출. 도메인별 파일 (auth.js, concepts.js, analyses.js ...)
│  └─ http.js    Axios 인스턴스, 인터셉터 (토큰 첨부, 401 → refresh → 1회 재시도)
├─ stores/       Pinia (auth.js, user.js ...)
├─ router/       index.js, 인증 가드 (meta.requiresAuth, meta.requiresAdmin)
├─ views/        화면 단위 컴포넌트 XxxView.vue (라우트와 1:1)
├─ components/   재사용 컴포넌트 (도메인별 하위 폴더)
├─ composables/  useXxx.js (폴링, 페이지네이션 등 재사용 로직)
└─ utils/
```

## 규칙

- **HTTP 호출은 `src/api/`에서만** 한다. 컴포넌트와 store에서 axios를 직접 import하지 않는다. `contract-checker`가 이 폴더를 기준으로 BE와 비교한다.
- API 함수는 spec의 API 계약과 **URL, 메서드, 필드 이름이 정확히 같아야** 한다. 응답을 가공하는 변환은 store나 composable에서 한다.
- 에러는 `error.response.data.message`를 사용자에게 보여 준다 (BE가 한국어 메시지를 보낸다). 코드 분기는 `error.response.data.code`로 한다.
- 모든 목록 화면은 **로딩 / 빈 목록 / 에러** 세 가지 상태를 처리한다.
- 로그인 상태, Access 토큰, 사용자 수준(level)은 `useAuthStore`에서 관리한다. 토큰 보관 위치와 Refresh 재시도는 F-02에서 정한다 (지금은 메모리, 401이면 `clear()`).
- 에러 메시지는 `errorMessage(error)`, 분기는 `errorCode(error)`(`@/api/http`)로 꺼낸다.
- 분석 진행 상태는 `composables/usePolling.js`로 2~3초 간격 폴링하고, `DONE`/`FAILED`이거나 화면을 떠나면 멈춘다.
- mermaid 렌더링(F-21)은 해당 화면에서만 동적 import한다.
- 스타일은 컴포넌트 `<style scoped>`를 기본으로 한다. 공통 색상·간격은 `src/assets/main.css`의 CSS 변수로 둔다.

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
| 린트 | `npm run lint` (자동 수정) / `npm run lint:check` (검사만, CI·verify가 사용) |
| 빌드 | `npm run build` |

루트에서 `node scripts/verify.mjs fe`를 쓰면 린트, 테스트, 빌드를 돌리고 실패만 요약해 준다.
Edit/Write 후에는 hook이 해당 파일에 Prettier를 자동 적용한다.
