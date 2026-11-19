# F-00b 공통 기반 (concept 도메인·도메인 블록·라우터 분리·공용 컴포넌트·관리자 레이아웃)

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-00b (인프라. 기능 목록에 없는 공통 기반, 결정로그 11-19) |
| 우선순위 | P0 |
| 영역 오너 | A 김동환 (공통 기반은 A가 먼저 만든다, `docs/conventions.md` 9절) |
| 관련 필수 요구사항 | ⑦ ⑧ ⑨ (카드 기능의 기반. 직접 충족하는 요구사항은 없음) |
| 의존 spec | F-02 (JWT role, `authGuard`의 `requiresAdmin`) |
| 관련 이슈 | 없음 |
| 승인 | 김동환 2026-11-19 |

## 1. 목적

두 사람이 concept 패키지를 함께 쓰는데(F-08은 A, F-09~13은 B), 공통 enum·배열·라우터·nav 끝에 각자 항목을 붙이면 같은 줄에서 충돌한다. B가 F-09를 시작하기 전에 A가 **공통 뼈대**를 develop에 먼저 넣는다. 카드 도메인 모델·공통 조회·카드 요약 DTO, 도메인 블록으로 나눈 ErrorCode·ActivityType·공개 경로, 영역별 route 파일과 route 메타로 만드는 nav, 공용 컴포넌트(ConceptCard·PageNav·EmptyState), 관리자 레이아웃, 로컬 관리자 계정 승격을 이 spec에서 만든다. 새 API는 없다.

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_시드_카드의_모든_컬럼을_매핑한다()`

### 백엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 시드 카드가 들어 있는 DB에서 | `ConceptMapper.findById(id)`를 부르면 | `Concept`의 모든 필드(id, name, category, difficulty=`Level`, summary, body, exampleCode, status=`ConceptStatus`, source=`ConceptSource`, referenceUrl, wishCount, avgRating, createdAt, updatedAt)가 채워진다. 없는 id면 `Optional.empty()` |
| AC2 | PUBLISHED 카드 1장, DRAFT 1장, HIDDEN 1장이 있을 때 | `ConceptQueryService.getPublished(id)`를 부르면 | PUBLISHED는 `Concept`를 돌려주고, DRAFT·HIDDEN·없는 id는 `BusinessException(CONCEPT_404)`를 던진다 (NF-04) |
| AC3 | PUBLISHED 카드 a·b, DRAFT 카드 c가 있을 때 | `findPublishedSummaries([b, c, 999, a])`를 부르면 | `[b, a]` 순서의 `ConceptSummaryResponse` 목록을 돌려준다 (PUBLISHED만, 입력 순서 유지, 없는 id 무시). 빈 목록을 넘기면 쿼리 없이 빈 목록 |
| AC4 | 평점 4.5, 찜 12인 카드로 | `ConceptSummaryResponse`를 JSON으로 직렬화하면 | `{id, name, category, difficulty, summary, avgRating: 4.5, wishCount: 12}`만 나온다 (avgRating은 숫자, body·exampleCode·status 없음) |
| AC5 | `ErrorCode` 전체에 대해 | `ErrorCodeTest`를 돌리면 | 모든 코드 이름이 `(COMMON\|AUTH\|USER\|ANALYSIS\|CONCEPT\|QUIZ\|REVIEW_PLAN\|POST\|LLM\|CHALLENGE)_\d{3}` 형식이고 message가 비어 있지 않으며, `CONCEPT_404`가 404다 (도메인 블록으로 재배치한 뒤 회귀 방지) |
| AC6 | `app.admin.bootstrap-email`에 가입한 ACTIVE 회원 이메일이 설정되면 | 애플리케이션이 기동하면 | 그 회원의 role이 `ADMIN`이 된다. 설정이 비어 있으면 아무것도 하지 않고, 해당 회원이 없거나 탈퇴했으면 WARN 로그만 남기고 기동은 계속된다. 이미 ADMIN이면 그대로다 |

### 프론트엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC7 | `router/index.js`가 `router/routes/{auth,user,analysis,concept,community,admin}.js`를 합칠 때 | 라우트 목록을 보면 | 기존 경로·이름(`home`, `login`, `signup`, `user-profile`, `my-settings`, `analyses`, `analysis-new`, `analysis-detail`, `not-found`)이 그대로 있고, 이름 중복이 없으며, `not-found`가 마지막이다 |
| AC8 | route에 `meta.nav = { label, order, show: 'guest'\|'user'\|'admin' }`가 달려 있을 때 | 헤더 nav를 만들면 (`navItems(routes, auth)`) | 비로그인은 로그인·회원가입, 로그인은 분석 요청·내 분석 (+ 닉네임·로그아웃은 App 고정), ADMIN은 여기에 관리자가 `order` 순서로 보인다 |
| AC9 | `/admin`이 `AdminLayout.vue` 아래 자식 라우트로 정의되어 있을 때 | 비로그인·USER·ADMIN이 `/admin`에 가면 | 비로그인은 `/login?redirect=/admin`, USER는 홈으로 가고, ADMIN은 관리자 홈(자식 route의 `meta.adminNav`로 만든 사이드 메뉴 + "관리할 항목을 고르세요" 안내)을 본다. 자식 라우트가 없으면 메뉴 자리에 "아직 관리 메뉴가 없습니다." |
| AC10 | 카드 요약 객체 1개로 | `ConceptCard`를 렌더링하면 | 이름, 카테고리, 난이도 라벨(입문·주니어·숙련, `LEVELS` 재사용), 요약(2줄 말줄임), 평점(소수 1자리)·찜 수가 보이고 카드 전체가 `/concepts/{id}` 링크다 |
| AC11 | 공용 `PageNav`(page, size, total)와 `EmptyState`(message, action 슬롯)로 | 렌더링·클릭하면 | `PageNav`는 total ≤ size면 숨고, 1페이지에서 이전·마지막 페이지에서 다음이 비활성이며, 클릭 시 `change(page)`를 emit한다. `EmptyState`는 문구와 슬롯을 보인다. `AnalysisListView`가 두 컴포넌트를 쓰도록 바뀌어도 기존 `AnalysisListView.spec.js`가 통과한다 |
| AC12 | 375px·1440px 폭에서 | `/`, `/analyses`, `/admin`(ADMIN)을 열면 | 가로 스크롤이 없고, 관리자 사이드 메뉴는 375px에서 본문 위 가로 메뉴로 접힌다 (NF-02, ui-verifier) |

## 3. API 계약

> 새 엔드포인트 없음. `docs/api/openapi.json` 변경 없음 (ErrorCode는 응답 스키마에 나오지 않는다).

- 에러 (이번에 추가하는 코드. 실제 응답은 F-10·F-11·F-12 등이 이 코드를 던질 때 생긴다)

| 상태 | 코드 | 언제 |
|---|---|---|
| 404 | CONCEPT_404 | 없는 카드, 또는 PUBLISHED가 아닌 카드(DRAFT·HIDDEN)를 일반 사용자가 조회. 메시지 "개념 카드를 찾을 수 없습니다." |

- `ConceptSummaryResponse` (카드 목록 F-09, 분석의 개념 F-08, 찜 목록 F-12, 복습 F-15, 추천 F-16이 공유하는 카드 요약)

```json
{
  "id": 12,
  "name": "JWT 인증",
  "category": "security",
  "difficulty": "JUNIOR",
  "summary": "서버가 서명한 토큰으로 요청한 사람을 확인하는 방식이다.",
  "avgRating": 4.5,
  "wishCount": 12
}
```

## 4. DB 변경

- Flyway: 없음 (V1 `concepts` 그대로)
- 사용 테이블: `concepts`(읽기), `users`(AC6 role 갱신)

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | `/admin` → `layouts/AdminLayout.vue` + 자식 `''` → `views/admin/AdminHomeView.vue`. 기존 라우트는 `router/routes/*.js`로 이동만 |
| 컴포넌트 | `components/concept/ConceptCard.vue`, `components/common/PageNav.vue`, `components/common/EmptyState.vue` (`Pagination`은 eslint `vue/multi-word-component-names` 때문에 `PageNav`) |
| store | 없음 (`useAuthStore`의 `isLoggedIn`·`isAdmin` 사용) |
| API 함수 | 없음 (`src/api/concepts.js`는 F-09가 만든다) |
| 유틸 | `utils/navItems.js`의 `navItems(routes, auth)`, `adminNavItems(adminRoute)` |
| 상태별 UI | 관리 메뉴 없음 → "아직 관리 메뉴가 없습니다." / 비관리자 → 가드가 홈으로 |

### route 파일 규칙

- `router/routes/<도메인>.js`가 route 배열을 export하고 `index.js`는 합치기만 한다. 기능 PR은 **자기 도메인 route 파일만** 고친다 (`analysis.js` A, `community.js` B, `concept.js`·`user.js`·`auth.js` 공통).
- 헤더 메뉴는 `meta.nav`, 관리자 사이드 메뉴는 `/admin` 자식 route의 `meta.adminNav = { label, order }`로 정한다. 관리자 자식 route는 각 도메인 파일이 `adminChildren` 배열로 export하고 `admin.js`가 합친다 (F-13 카드 관리는 `concept.js`, F-24 챌린지 관리는 `challenge.js`).
- `/admin` 부모 route에 `meta: { requiresAuth: true, requiresAdmin: true }`를 달면 vue-router가 자식의 `to.meta`에 합쳐 주므로 자식은 다시 달지 않는다.

## 6. LLM (AI 기능만)

해당 없음.

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1 | `concept/ConceptMapperTest#AC1_...` | MyBatis 통합 (Testcontainers, 시드 카드) |
| AC2·AC3 | `concept/ConceptQueryServiceTest#AC2_...`·`#AC3_...` | 통합 (DRAFT·HIDDEN 카드는 테스트에서 INSERT) |
| AC4 | `concept/dto/ConceptSummaryResponseTest#AC4_...` | Jackson 직렬화 단위 |
| AC5 | `common/exception/ErrorCodeTest#AC5_...` | 단위 |
| AC6 | `user/AdminBootstrapTest#AC6_...` | 통합 (설정 있음·비어 있음·회원 없음·탈퇴 회원·이미 ADMIN) |
| AC7 | `router/__tests__/routes.spec.js` | Vitest |
| AC8 | `utils/__tests__/navItems.spec.js`, `__tests__/App.spec.js` | Vitest |
| AC9 | `router/__tests__/authGuard.spec.js`(추가 케이스), `layouts/__tests__/AdminLayout.spec.js` | Vitest |
| AC10 | `components/concept/__tests__/ConceptCard.spec.js` | Vitest |
| AC11 | `components/common/__tests__/PageNav.spec.js`·`EmptyState.spec.js`, 기존 `views/__tests__/AnalysisListView.spec.js` | Vitest |
| AC12 | ui-verifier `/`, `/analyses`, `/admin` 데스크톱·모바일 (`/admin`은 AC6로 승격한 계정) | 화면 점검 |

## 8. 범위 밖

- 카드 목록·검색·상세·리뷰·찜·관리자 카드 관리 화면과 API (F-09~F-13, B), `src/api/concepts.js`
- 카드 상세 DTO(`ConceptDetailResponse`), "내가 찜했는지" 같은 사용자별 필드
- concepts 쓰기: AI 초안 INSERT(F-08), 관리자 CRUD(F-13), `avg_rating`·`wish_count` 갱신(F-11·F-12)
- `PUBLIC_GET_PATHS`에 카드·게시판·챌린지 경로 추가 (각 기능이 자기 블록에 추가)
- 기존 flat views(`LoginView`, `AnalysisListView` 등)를 도메인 폴더로 옮기기
- 관리자 챌린지 관리 화면(F-24), 관리자 계정을 화면에서 지정하는 기능

## 9. 결정 필요

- 없음. 11-19 사용자 결정 반영: ID `F-00b`, nav는 route `meta.nav`, 관리자 계정은 설정값 승격(`docs/결정로그.md`).

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [ ] 브랜치 `feature/F-00b-common-foundation`
- [ ] Flyway 마이그레이션 (없음)
- [ ] AC별 실패 테스트 작성 (red 확인)
- [ ] BE 구현 (green)
  - [ ] `com.vibenote.concept`: `domain/Concept`, `ConceptStatus`, `ConceptSource`, `ConceptMapper`(+`mapper/concept/ConceptMapper.xml`), `ConceptQueryService`, `dto/ConceptSummaryResponse`. Mapper Java·XML은 `// ── 공통 조회 ──` / `// ── F-08 개념 추출 (A) ──` / `// ── F-09~13 카드 (B) ──` 블록으로 나눈다. 다른 도메인은 `ConceptQueryService`로 읽는다(9절 "상대 테이블")
  - [ ] `ErrorCode`: conventions 3절 순서의 도메인 블록 주석(`// ── CONCEPT (A F-08 · B F-09~13) ──` 등, QUIZ·REVIEW_PLAN·POST·CHALLENGE는 주석만), **모든 항목 끝에 `,`, `;`는 단독 줄**. `CONCEPT_404` 추가
  - [ ] `ActivityType`·`SecurityConfig.PUBLIC_GET_PATHS`: 같은 블록·끝 쉼표 방식 (`PUBLIC_GET_PATHS`는 여러 줄로 펴고 도메인 블록 주석만)
  - [ ] `user/AdminBootstrap`(`ApplicationReadyEvent`) + `UserMapper.promoteToAdmin(email)`(ACTIVE만, 갱신 행 수 반환), `AdminProperties`(`app.admin.bootstrap-email`). `application-local.yml`에 `${ADMIN_BOOTSTRAP_EMAIL:}`만 둔다(저장소에 이메일 없음). 승격 후에는 새로고침(앱 시작 refresh가 DB role로 Access 토큰을 다시 발급)으로 반영된다
- [ ] FE 구현 (green)
  - [ ] `router/routes/*.js` 분리, `utils/navItems.js`, `App.vue` nav를 `navItems`로
  - [ ] `layouts/AdminLayout.vue`, `views/admin/AdminHomeView.vue`, `router/routes/admin.js`
  - [ ] `ConceptCard`, `PageNav`, `EmptyState`, `AnalysisListView` 적용
- [ ] Swagger 어노테이션 + `docs/api/openapi.json` 갱신 (새 API 없음 → 변경 없음 확인)
- [ ] contract-checker 통과 (해당 없음이면 생략)
- [ ] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일)
- [ ] reviewer 지적 사항 반영
- [ ] CODEOWNERS: `/frontend/src/router/routes/analysis.js`·`challenge.js` A, `community.js` B, `/frontend/src/layouts/`·`/frontend/src/views/admin/` 공통, `**/vibenote/concept/`·`mapper/concept/`는 이미 공통
- [ ] `docs/환경설정.md`에 로컬 관리자 계정 만드는 법(`ADMIN_BOOTSTRAP_EMAIL`), `.env.example`에 빈 항목, `frontend/CLAUDE.md`·`docs/conventions.md` 9절에 route 파일·`meta.nav` 규칙
- [ ] 산출물 반영 (화면 설계서 SC-20·21에 관리자 레이아웃 공통 구조 한 줄)
- [ ] PR 본문에 `@JoonseobKim` 멘션 (공통 코드)
- [ ] PR CI 초록
- [ ] 상태 DONE (PR 머지 후)
