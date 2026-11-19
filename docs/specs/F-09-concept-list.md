# F-09 개념 카드 목록·검색

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-09 |
| 우선순위 | P0 |
| 영역 오너 | B 김준섭 (카드·소셜) |
| 관련 필수 요구사항 | ⑧ 콘텐츠 목록·검색 |
| 의존 spec | F-13 시드 카드(`R__seed_concepts.sql`, 85개, DONE). **A 김동환의 concept 공통 기반 PR**(결정로그 11-19: concept 도메인·`ConceptMapper`·카드 응답 DTO·`ConceptCard.vue`·`PageNav`·라우터 영역 분리, **PR #42로 11-19 develop 머지됨**). 목록 항목 DTO 계약은 이 spec 3절이 정하고, 공통 기반 PR이 이 계약대로 만든다 |
| 관련 이슈 | 없음 |
| 승인 | 김준섭 2026-11-19 |

## 1. 목적

학습자가 분석 결과와 상관없이 **공개된 개념 카드 전체**를 둘러보고, 키워드·카테고리·난이도로 좁혀서 공부할 카드를 찾는다.
비로그인 방문자도 볼 수 있어 랜딩에서 "어떤 개념을 배울 수 있는지"를 보여 주는 진입점이 된다 (SC-09는 공개 화면).
인기순(찜 수 → 평점)을 기본으로 해서 검증된 카드를 먼저 보여 주고, 최신순으로 새로 공개된 카드(AI 초안 검수분)를 볼 수 있다.

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_파라미터_없이_요청하면_공개_카드를_인기순으로_받는다()`
> BE 테스트는 시드 개수(85)에 기대지 않는다. 테스트가 고유한 이름의 카드를 직접 넣고 그 카드들만 걸리는 조건으로 확인한다 (시드는 계속 늘어난다).

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 누구나(비로그인 포함) | `GET /api/concepts`를 파라미터 없이 요청하면 | 200과 페이지 응답 `{items, page:1, size:20, total}`을 받는다. `items`는 `status=PUBLISHED`인 카드만, 기본 정렬(인기순)로 담긴다. 항목 형식은 3절 `ConceptSummaryResponse`. 로그인 토큰이 있어도 결과는 같다 |
| AC2 | `DRAFT`·`HIDDEN` 카드가 섞여 있을 때 | 어떤 필터·정렬로 조회해도 | `PUBLISHED`가 아닌 카드는 `items`에도 `total`에도 들어가지 않는다 (NF-04) |
| AC3 | 이름이나 요약에 키워드가 들어간 카드가 있을 때 | `keyword=jwt`처럼 요청하면 | `name` 또는 `summary`에 키워드가 부분 일치하는 카드만 받는다 (대소문자 무시). `body`·`example_code`만 일치하는 카드는 나오지 않는다. 앞뒤 공백은 trim하고, trim 후 빈 문자열이면 키워드 조건이 없는 것으로 본다. `%`·`_`는 와일드카드가 아니라 글자 그대로 찾는다 (`keyword=100%`가 모든 카드를 반환하지 않음) |
| AC4 | 여러 카테고리·난이도의 카드가 있을 때 | `category=security&difficulty=JUNIOR`처럼 요청하면 | 두 조건을 **모두** 만족하는 카드만 받는다. 키워드와도 AND로 결합된다. 목록에 없는 카테고리(예: `python`)는 400이 아니라 빈 목록(`items: []`, `total: 0`)이다 |
| AC5 | 찜 수·평점·등록일이 다른 카드가 있을 때 | `sort=POPULAR`(또는 생략) / `sort=LATEST`로 요청하면 | POPULAR는 `wish_count DESC, avg_rating DESC, id ASC`, LATEST는 `created_at DESC, id DESC` 순서다. 동점에도 순서가 항상 같아서 페이지를 넘겨도 카드가 겹치거나 빠지지 않는다 |
| AC6 | 조건에 맞는 카드가 25개일 때 | `page=2&size=10` / `page=4&size=10`으로 요청하면 | page 2는 11~20번째 10개와 `total: 25`, 범위를 넘은 page 4는 `items: []`과 `total: 25`를 받는다 |
| AC7 | 누구나 | 규칙을 어긴 값(`difficulty=EXPERT`, `sort=NAME`, `page=0`, `size=101`, 51자 이상 `keyword`, 51자 이상 `category`)으로 요청하면 | 400 `COMMON_400`을 받고 `details`에 필드별 사유가 담긴다 (4절 검증 규칙). enum 값은 대문자만 받는다 (`difficulty=junior`도 400) |
| AC8 | 시드 카드 85개가 적재된 DB에서 | 키워드·카테고리 조건을 넣어 목록을 요청하면 | 한 번 데운 뒤 잰 응답 시간이 500ms 이내다 (NF-03). 통합 테스트에서 측정한다 |
| AC9 | 누구나 | `/concepts` 화면(SC-09)을 열면 | 검색창, 카테고리 선택(전체 + 6종), 난이도 선택(전체 + 3단계), 정렬 선택(인기순·최신순), 카드 그리드, 이전·다음 페이지를 본다. 카드는 이름, 카테고리, 난이도 배지, 요약, 찜 수, 평점을 보여 주고, 누르면 `/concepts/:id`(F-10)로 간다. 상단 nav에 "개념 카드" 링크가 있다 |
| AC10 | `/concepts` 화면에서 | 검색어를 입력하고 Enter나 검색 버튼을 누르거나, 필터·정렬을 바꾸거나, 페이지를 넘기면 | 조건이 URL 쿼리(`?keyword=jwt&category=security&difficulty=JUNIOR&sort=LATEST&page=2`)에 반영되고 그 조건으로 다시 조회한다. 검색·필터·정렬을 바꾸면 page는 1로 돌아간다. 쿼리가 달린 URL을 바로 열거나 뒤로 가기를 하면 그 조건이 입력란과 목록에 복원된다. 기본값(`sort=POPULAR`, `page=1`, 빈 값)은 쿼리에 넣지 않는다 |
| AC11 | `/concepts` 화면에서 | 로딩 중 / 결과가 0건 / 요청이 실패하면 | 로딩 문구 / "조건에 맞는 개념 카드가 없습니다" + "필터 초기화" 버튼(누르면 `/concepts`로 이동) / 서버 `message`를 보여 준다. 375px 폭에서는 카드가 한 열이고 가로 스크롤이 없다 (NF-02) |

### 검증 규칙

| 파라미터 | 규칙 | 에러 메시지 (`details` 값) |
|---|---|---|
| keyword | 선택. trim 후 ≤50자. 빈 문자열이면 조건 없음 | "검색어는 50자 이하로 입력하세요." |
| category | 선택. ≤50자 (`concepts.category VARCHAR(50)`). 값 목록 검증은 하지 않는다 (AC4) | "카테고리는 50자 이하여야 합니다." |
| difficulty | 선택. `BEGINNER`·`JUNIOR`·`SENIOR` | "난이도는 BEGINNER, JUNIOR, SENIOR 중 하나여야 합니다." |
| sort | 선택. `POPULAR`(기본)·`LATEST` | "정렬은 POPULAR, LATEST 중 하나여야 합니다." |
| page·size | `PageQuery` 그대로 (page ≥1, 1 ≤ size ≤100, 기본 1·20) | `PageQuery`의 메시지 |

- enum 파라미터는 문자열로 받아 검증한다 (F-01 `level`과 같은 방식). Spring 기본 변환 실패(`MethodArgumentTypeMismatchException`)에 맡기면 `details`에 필드가 담기지 않는다.
- 카테고리 6종(`java`·`spring`·`web`·`security`·`mybatis`·`vue`, 결정로그 11-18)은 FE 상수로 둔다. 화면 표시명은 Java·Spring·Web·Security·MyBatis·Vue.

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다 (contract-checker, CI가 검사). Swagger Tag: `개념 카드`. Controller `com.vibenote.concept.ConceptController` (`/api/concepts`). 이 Controller의 패키지·파일 뼈대는 A의 공통 기반 PR이 만들 수 있다. 그때는 그 파일에 이 엔드포인트를 추가한다.

### `GET /api/concepts?keyword=&category=&difficulty=&sort=&page=&size=`

- 권한: 공개. `SecurityConfig.PUBLIC_GET_PATHS`의 concept 도메인 블록에 `/api/concepts`를 추가한다 (공통 파일 → PR에서 @whans1996 멘션). `/api/concepts/{id}`(F-10)는 이 spec에서 열지 않는다
- 요청 예: `GET /api/concepts?keyword=jwt&category=security&sort=POPULAR&page=1&size=20`
- 응답 `200`

```json
{
  "items": [
    {
      "id": 57,
      "name": "JWT 기반 인증",
      "category": "security",
      "difficulty": "SENIOR",
      "summary": "JWT는 사용자 정보와 만료 시간을 담고 서명한 토큰이다. 서버는 세션을 저장하지 않고, 요청마다 Authorization 헤더의 토큰 서명과 만료를 검증해 사용자를 식별한다.",
      "wishCount": 12,
      "avgRating": 4.5
    }
  ],
  "page": 1,
  "size": 20,
  "total": 1
}
```

- 목록 항목 `ConceptSummaryResponse` (record, **공통 DTO**: F-08 분석의 개념 탭, F-12 내 찜 목록, F-16 추천도 같은 형식을 쓴다)

| 필드 | 타입 | 출처 | 비고 |
|---|---|---|---|
| id | long | `concepts.id` | |
| name | string | `name` | |
| category | string | `category` | 소문자 그대로 |
| difficulty | string | `difficulty` | `BEGINNER`·`JUNIOR`·`SENIOR` |
| summary | string | `summary` | ≤500자. 목록에서 자르는 것은 FE가 CSS로 한다 |
| wishCount | int | `wish_count` | |
| avgRating | number | `avg_rating` | 소수 첫째 자리 (`DECIMAL(2,1)`). 리뷰가 없으면 `0.0` |

- `body`·`exampleCode`·`referenceUrl`·`status`·`source`는 목록에 넣지 않는다 (상세 F-10, 관리자 F-13).
- 로그인 사용자의 "내가 찜했는지"(`wished`)는 이 spec에 넣지 않는다 (F-12에서 결정).
- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 400 | COMMON_400 | 검증 실패 (AC7, `details`에 필드별 사유) |

- 새 에러 코드는 없다 (`CONCEPT_404`는 F-10).

## 4. DB 변경

- Flyway: 없음
- 사용 테이블: `concepts` (읽기만)
- 쿼리 요지 (`ConceptMapper.xml`의 `findPublished`·`countPublished`, 같은 `<sql>` WHERE 조각 공유)
  - `WHERE status = 'PUBLISHED'` + 선택 조건 `category = #{category}`, `difficulty = #{difficulty}`, `(name LIKE CONCAT('%', #{keyword}, '%') ESCAPE '!' OR summary LIKE ...)`
  - keyword의 `!`·`%`·`_`는 Service가 `!` 접두로 이스케이프한 뒤 넘긴다 (MySQL 기본 모드에서 백슬래시를 피함, 시드 파일 주의 사항과 같은 이유)
  - 정렬은 `<choose>`로 고정 문자열만 쓴다 (`${}` 금지, SQL 주입 방지)
  - 대소문자 무시는 collation에 맡긴다. V1은 collation을 명시하지 않아 MySQL 8 기본값 `utf8mb4_0900_ai_ci`(대소문자 무시)를 쓴다. 서버 설정이 달라도 AC3 테스트가 잡는다
- 인덱스: 기존 `idx_concepts_status_category`로 충분하다 (카드 수백 개 규모. 85개 기준 AC8로 확인)

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | `/concepts` → `views/concept/ConceptListView.vue` (공개, `meta` 없음). 라우트는 공통 기반이 나눈 concept 블록에 추가 |
| 컴포넌트 | `components/concept/ConceptCard.vue`(공통 기반 제공: 이름·카테고리·난이도 배지·요약·찜 수·평점, 클릭 → `/concepts/:id`), `components/common/PageNav.vue`(공통 기반 제공, `page·size·total` props, `change` 이벤트), `components/common/EmptyState.vue`(0건 문구 + `action` 슬롯에 "필터 초기화"), 필터 바는 `components/concept/ConceptFilterBar.vue` (B 영역 새 파일) |
| store | 없음. 조건의 정본은 라우트 쿼리이고, 화면이 `watch(route.query)`로 다시 조회한다 |
| API 함수 | `src/api/concepts.js`(공통 기반에 없어 이 기능에서 새로 만든다)의 `fetchConcepts({ keyword, category, difficulty, sort, page, size })`. 빈 값은 쿼리에서 뺀다 |
| 상수 | `CONCEPT_CATEGORIES`(6종 value·label, 새로 만든다). 난이도는 `utils/authValidation`의 `LEVELS`(ConceptCard와 같은 표기)를 쓴다 |
| 상태별 UI | 로딩 / 0건(필터 초기화 버튼) / 에러(서버 message) — AC11 |
| 반응형 | 카드 그리드 `repeat(auto-fill, minmax(260px, 1fr))` (SC-06과 같은 규칙), 필터 바는 모바일에서 줄바꿈 |
| nav | `router/routes/concept.js`의 `/concepts` route에 `meta.nav = { label: "개념 카드", order, show: "all" }`. F-00b `utils/navItems.js`에는 `show`가 `guest`·`user`·`admin`뿐이라 **모두에게 보이는 `all`을 추가한다** (공통 파일, 테스트 `navItems.spec.js`에 케이스 추가, PR에서 @whans1996 멘션) |

## 6. LLM (AI 기능만)

해당 없음.

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1, AC2 | `ConceptListApiTest#AC1_...`, `#AC2_...` | MockMvc + Testcontainers 통합 (테스트가 `DRAFT`·`HIDDEN`·`PUBLISHED` 카드를 직접 넣음) |
| AC3 | `ConceptListApiTest#AC3_...` (name 일치, summary 일치, body만 일치 제외, 대소문자, 공백 trim, `%`·`_` 리터럴) | MockMvc 통합 |
| AC4 | `ConceptListApiTest#AC4_...` (AND 결합, 없는 카테고리 빈 목록) | MockMvc 통합 |
| AC5 | `ConceptListApiTest#AC5_...` (wish_count·avg_rating·created_at을 직접 넣어 순서 검증, 동점 tie-break) | MockMvc 통합 |
| AC6 | `ConceptListApiTest#AC6_...` | MockMvc 통합 |
| AC7 | `ConceptListApiTest#AC7_...` (`@ParameterizedTest`, `details` 필드 확인) | MockMvc 통합 |
| AC8 | `ConceptListApiTest#AC8_...` (시드 적재 상태, 1회 워밍업 후 측정) | MockMvc 통합 |
| AC3 이스케이프 | `ConceptServiceTest#keyword_이스케이프` | 단위 |
| AC9 | `views/__tests__/ConceptListView.spec.js` (`vi.mock('@/api/concepts')`) | Vitest |
| AC10 | `ConceptListView.spec.js` (쿼리 → API 인자, 조작 → `router.push` 쿼리, page 1 리셋, 기본값 생략) | Vitest |
| AC11 | `ConceptListView.spec.js` (로딩·0건·에러), ui-verifier 375px·1280px | Vitest + ui-verifier |
| API 함수 | `api/__tests__/concepts.spec.js` (빈 값 제외, URL·파라미터 이름) | Vitest |

## 8. 범위 밖

- 카드 상세, 내 코드 등장 위치, 관련 카드 (F-10). `GET /api/concepts/{id}` 공개 설정도 F-10
- 찜 버튼·`wished` 표시 (F-12), 리뷰 작성 (F-11). 목록은 `wish_count`·`avg_rating` 숫자만 보여 준다
- 관리자의 DRAFT·HIDDEN 카드 조회 (F-13 `/api/admin/concepts`)
- 전문 검색(FULLTEXT·형태소), 자동완성, 검색어 하이라이트, 무한 스크롤
- 카테고리 목록 API (카테고리는 6종 고정, FE 상수)
- 리뷰 수(`reviewCount`) 표시 (컬럼 없음. 필요하면 F-11에서 결정)

## 9. 결정 필요

- ✅ (11-19 해결) F-00b PR #42 머지 후 대조: `ConceptSummaryResponse` 필드·이름 일치(`difficulty`는 `Level` enum). 이름이 다른 공통 부품(`PageNav`·`EmptyState`·`LEVELS`·`meta.nav`)과 `navItems` `show: "all"` 추가를 5절에 반영
- ~~(구현 착수 전) A의 공통 기반 PR에서 `ConceptSummaryResponse` 필드·이름이 3절과 다르면 이 spec을 그에 맞춰 고친다. 공통 기반 PR이 F-09 착수 때까지(W2 11/24) 머지되지 않으면 김동환과 Mattermost로 일정을 정한다~~

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] A 공통 기반 PR develop 머지 확인 → `git merge origin/develop` (PR #42, 11-19)
- [x] 브랜치 `feature/F-09-concept-list`
- [x] Flyway 마이그레이션 (없음)
- [x] AC별 실패 테스트 작성 (red 확인)
- [x] BE 구현 (green): `ConceptController#list`, `ConceptService#search`, `ConceptMapper` `findPublished`·`countPublished`, `ConceptSearchRequest`(검증), `PUBLIC_GET_PATHS`
- [x] FE 구현 (green): `fetchConcepts`, `ConceptListView`, `ConceptFilterBar`, 라우트 `meta.nav`, `navItems.js` `show: "all"`
- [x] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [ ] contract-checker 통과
- [ ] ui-verifier 통과 (데스크톱·모바일)
- [ ] reviewer 지적 사항 반영
- [ ] 산출물 반영: `06-화면설계서.md` SC-09 절 + 스크린샷(`screens/SC-09-*.png`), CODEOWNERS에 새 FE 폴더(`views/concept/`·`components/concept/`)가 없으면 추가
- [ ] PR CI 초록 (공통 파일 변경 → 본문에서 @whans1996 멘션)
- [ ] 상태 DONE (PR 머지 전 마지막 커밋)
