# F-10 개념 카드 상세

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-10 |
| 우선순위 | P0 |
| 영역 오너 | B 김준섭 (카드·소셜) |
| 관련 필수 요구사항 | ⑨ 콘텐츠 상세 |
| 의존 spec | **F-00b 공통 기반**(A 김동환: `Concept`·`ConceptMapper.findById`·`ConceptQueryService.getPublished`(→ `CONCEPT_404`)·`ConceptSummaryResponse`·`ConceptCard.vue`·concept route 블록), **F-09 카드 목록**(`ConceptController`·`src/api/concepts.js`·`PUBLIC_GET_PATHS`의 concept 블록·SC-09 화면). 두 PR이 develop에 머지된 뒤 구현을 시작한다. 내 코드 등장 위치·함께 등장 기준의 데이터(`analysis_concepts`)는 F-08(A, W3)이 채운다. F-10 테스트는 행을 직접 넣어 검증하므로 F-08을 기다리지 않는다 |
| 관련 이슈 | 없음 |
| 승인 | 김준섭 2026-11-19 |

## 1. 목적

학습자가 카드 하나를 열어 **쉬운 설명 → 예제 코드 → 공식 문서 출처**를 차례로 읽고, 로그인했으면 **내 분석 중 이 개념이 나온 위치**로 바로 가서 내 코드에 비춰 이해한다.
비로그인 방문자도 공개(`PUBLISHED`) 카드는 끝까지 읽을 수 있다 (SC-10 공개 화면).
아래쪽 관련 카드(최대 4개)로 이어서 공부할 카드를 고른다. 리뷰(F-11)와 찜(F-12)은 이 화면에 나중에 붙는다.

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_공개_카드를_비로그인으로_조회하면_상세를_받는다()`
> BE 테스트는 시드 카드에 기대지 않는다. 고유한 카테고리 이름(예: `f10-test-a`)의 카드·회원·분석·`analysis_concepts` 행을 직접 넣고 그것만 확인한다.

### 백엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 누구나(비로그인) | `PUBLISHED` 카드를 `GET /api/concepts/{id}`로 요청하면 | 200과 3절 `ConceptDetailResponse`를 받는다. `body`·`exampleCode`·`referenceUrl`이 NULL인 카드는 그 필드가 `null`이다. `appearances`는 `[]`이다. `status`·`source`·`createdAt`은 응답에 없다 |
| AC2 | `DRAFT`·`HIDDEN` 카드, 없는 id(`999999`)가 있을 때 | 비로그인이나 로그인 회원이 상세를 요청하면 | 모두 404 `CONCEPT_404` "개념 카드를 찾을 수 없습니다."를 받는다 (NF-04, 존재 여부를 구분해 주지 않는다) |
| AC3 | 로그인 회원 U가 상태 `DONE`인 분석 2개에서 카드 C와 연결되어 있을 때 (`analysis_concepts`) | U의 토큰으로 C 상세를 요청하면 | `appearances`에 두 분석이 분석 `created_at DESC, id DESC` 순서로 담긴다. 항목은 `{analysisId, analysisTitle, fileId, filePath, lineHint, contextNote}`. `analysisTitle`은 `title`, 없으면 `project_name`, 둘 다 없으면 `"제목 없는 분석"`이다. `file_id`가 NULL인 연결은 `fileId`·`filePath`가 `null`이다 |
| AC4 | U의 분석 외에 다른 회원의 분석(공개 포함), U의 삭제된 분석(`deleted_at` 있음), U의 `DONE`이 아닌 분석(진행 중·`FAILED`)도 C와 연결되어 있을 때 | U가 C 상세를 요청하면 | U의 삭제되지 않은 `DONE` 분석만 나온다. 연결된 분석이 11개 이상이면 최근 10개만 담는다 |
| AC5 | 형식이 틀리거나 만료된 토큰을 보내면 | C 상세를 요청하면 | 401이 아니라 200이고, 비로그인과 같게 `appearances: []`이다 (공개 경로라 인증 필터가 토큰을 무시한다) |
| AC6 | 카드 C와 같은 분석에 함께 연결된 `PUBLISHED` 카드 X(3회)·Y(2회)·Z(2회), `DRAFT` 카드 D(5회)가 있을 때 | C 상세를 요청하면 | `related`는 X, 그다음 Y·Z가 `wish_count DESC, avg_rating DESC, id ASC` 순서로 나온다. C 자신과 D는 나오지 않는다. 함께 등장 횟수는 **모든 회원**의 삭제되지 않은 `DONE` 분석에서 센 서로 다른 분석 수다. 항목 형식은 `ConceptSummaryResponse`이고 최대 4개다 |
| AC7 | 함께 등장한 `PUBLISHED` 카드가 4개보다 적을 때 (0개 포함) | C 상세를 요청하면 | 남은 자리를 C와 **같은 카테고리**의 `PUBLISHED` 카드로 인기순(`wish_count DESC, avg_rating DESC, id ASC`)으로 채운다. 이미 고른 카드와 C 자신은 빼고, 채울 카드가 모자라면 4개보다 적게(카테고리에 C뿐이면 `[]`) 돌려준다 |
| AC8 | 시드 카드 85개가 적재된 DB에서 | 로그인 회원이 상세를 요청하면 | 한 번 데운 뒤 잰 응답 시간이 500ms 이내다 (NF-03 기준을 상세에도 적용) |

### 프론트엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC9 | 누구나 | `/concepts/:id`(SC-10)를 열면 | 헤더(이름, 카테고리, 난이도 배지, ♥ 찜 수, ★ 평점 소수 1자리), 요약, "쉬운 설명"(본문 Markdown 렌더링), "예제 코드"(코드 블록 + "복사" 버튼), "출처"(`referenceUrl`, `target="_blank" rel="noopener noreferrer"`)를 본다. 값이 `null`인 절(본문·예제·출처)은 제목째 숨긴다. "복사"를 누르면 예제 코드가 클립보드에 들어가고 버튼이 2초간 "복사됨"이 된다. 실패하면 "복사하지 못했습니다." |
| AC10 | 로그인 회원의 상세 응답에 `appearances`가 있을 때 | 화면을 보면 | "내 코드에서 등장한 위치" 절에 항목마다 "분석 제목 · 파일 경로 · 줄 힌트 · 맥락"이 보이고(빈 값은 건너뜀), 누르면 `fileId`가 있으면 `/analyses/{analysisId}?tab=files&file={fileId}`, 없으면 `/analyses/{analysisId}`로 간다. `appearances`가 비어 있으면(비로그인 포함) 절 전체를 숨긴다 |
| AC11 | `related`가 있을 때 | 화면을 보면 | "관련 카드" 절에 `ConceptCard`가 최대 4개 보이고, 누르면 그 카드 상세로 간다. 같은 화면에서 `:id`만 바뀌어도 새 카드를 다시 조회하고 맨 위로 스크롤한다. `related`가 비어 있으면 절을 숨긴다 |
| AC12 | 상세 화면에서 | "← 목록으로"를 누르면 | 직전 화면이 SC-09(`/concepts` + 쿼리)였으면 뒤로 가서 그 검색 조건을 그대로 복원하고, 아니면(직접 진입, 다른 화면에서 진입) `/concepts`로 간다 |
| AC13 | 상세 화면에서 | 로딩 중 / 404 `CONCEPT_404` / 그 밖의 실패이면 | "불러오는 중…" / "개념 카드를 찾을 수 없습니다." + "목록으로" 버튼(`/concepts`) / 서버 `message`를 보여 준다. 375px 폭에서 페이지 가로 스크롤이 없고 긴 예제 코드는 코드 블록 안에서만 가로로 스크롤한다. 관련 카드는 한 열이다 (NF-02) |
| AC14 | Markdown 문자열을 | 본문 렌더러(`renderMarkdown`)에 넣으면 | 지원 문법(5절)만 요소로 바꾸고 나머지는 글자 그대로 보인다. `<script>alert(1)</script>`·`<img onerror>` 같은 HTML은 태그가 아니라 텍스트로 보인다 (렌더러는 `v-html`을 쓰지 않는다) |

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다 (contract-checker, CI가 검사). Swagger Tag: `개념 카드`. `com.vibenote.concept.ConceptController`(F-09가 만든 파일)에 엔드포인트를 추가한다.

### `GET /api/concepts/{id}`

- 권한: 공개, 로그인하면 `appearances`가 채워진다. `SecurityConfig.PUBLIC_GET_PATHS`의 concept 블록에 `/api/concepts/{id:\\d+}`를 추가한다 (공통 파일 → PR에서 @whans1996 멘션). Controller는 `@AuthenticationPrincipal AuthUser user`를 받고 `null`이면 비로그인으로 처리한다
- 요청 예: `GET /api/concepts/57` (`Authorization: Bearer <token>` 선택)
- 응답 `200` (로그인 회원)

```json
{
  "id": 57,
  "name": "JWT 기반 인증",
  "category": "security",
  "difficulty": "SENIOR",
  "summary": "JWT는 사용자 정보와 만료 시간을 담고 서명한 토큰이다. 서버는 세션을 저장하지 않고, 요청마다 Authorization 헤더의 토큰 서명과 만료를 검증해 사용자를 식별한다.",
  "body": "## 핵심\n- 토큰은 header.payload.signature 세 부분이다.\n- 서버는 **서명**과 만료(`exp`)만 검증한다.",
  "exampleCode": "String token = Jwts.builder()\n    .subject(email)\n    .expiration(expiry)\n    .signWith(key)\n    .compact();",
  "referenceUrl": "https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html",
  "wishCount": 12,
  "avgRating": 4.5,
  "appearances": [
    {
      "analysisId": 31,
      "analysisTitle": "쇼핑몰 과제",
      "fileId": 412,
      "filePath": "src/main/java/com/shop/security/JwtProvider.java",
      "lineHint": "L24-L40",
      "contextNote": "로그인 성공 시 토큰을 만든다"
    },
    {
      "analysisId": 28,
      "analysisTitle": "todo-app",
      "fileId": null,
      "filePath": null,
      "lineHint": null,
      "contextNote": null
    }
  ],
  "related": [
    {
      "id": 58,
      "name": "Spring Security 필터 체인",
      "category": "security",
      "difficulty": "JUNIOR",
      "summary": "요청은 DispatcherServlet에 닿기 전에 보안 필터들을 차례로 지난다.",
      "wishCount": 9,
      "avgRating": 4.2
    }
  ]
}
```

- `ConceptDetailResponse` (record, `com.vibenote.concept.dto`)

| 필드 | 타입 | 출처 | 비고 |
|---|---|---|---|
| id, name, category, difficulty, summary, wishCount, avgRating | | `concepts` | `ConceptSummaryResponse`와 같은 이름·형식 |
| body | string \| null | `body` | Markdown 원문. 렌더링은 FE |
| exampleCode | string \| null | `example_code` | 언어 정보 없음 (컬럼 없음) |
| referenceUrl | string \| null | `reference_url` | |
| appearances | `ConceptAppearanceResponse[]` | `analysis_concepts` ⋈ `analyses` ⋈ `analysis_files` | 비로그인은 `[]` (null 아님). 최대 10개 (AC3·AC4) |
| related | `ConceptSummaryResponse[]` | `concepts` | 최대 4개 (AC6·AC7) |

- `ConceptAppearanceResponse`: `analysisId`(long), `analysisTitle`(string), `fileId`(long \| null), `filePath`(string \| null, `analysis_files.path`), `lineHint`(string \| null), `contextNote`(string \| null)
- `wished`(내가 찜했는지)·`reviewCount`·리뷰 목록은 넣지 않는다 (F-12·F-11이 이 응답에 필드를 추가하거나 별도 API로 정한다)
- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 404 | CONCEPT_404 | 없는 카드, `DRAFT`·`HIDDEN` 카드 (AC2. F-00b `ConceptQueryService.getPublished`가 던진다) |

- 새 에러 코드는 없다. 숫자가 아닌 id(`/api/concepts/abc`)는 공개 경로 패턴(`\\d+`)에 걸리지 않으므로 BE 계약에 넣지 않고, FE 라우트가 `:id(\\d+)`로 막아 not-found 화면으로 보낸다.

## 4. DB 변경

- Flyway: 없음
- 사용 테이블: `concepts`, `analysis_concepts`, `analyses`, `analysis_files` (모두 읽기만)
- 쿼리 요지 (`ConceptMapper.xml`. F-00b가 만든 파일에 B가 쿼리를 추가한다)
  - 카드: F-00b `ConceptQueryService.getPublished(id)` 재사용
  - `findAppearances(conceptId, userId)`: `analysis_concepts ac JOIN analyses a ON a.id = ac.analysis_id LEFT JOIN analysis_files f ON f.id = ac.file_id WHERE ac.concept_id = ? AND a.user_id = ? AND a.status = 'DONE' AND a.deleted_at IS NULL ORDER BY a.created_at DESC, a.id DESC LIMIT 10`. 제목은 `COALESCE(a.title, a.project_name)`로 가져오고 둘 다 NULL이면 Service가 "제목 없는 분석"으로 채운다
  - `findCoOccurring(conceptId, limit=4)`: `analysis_concepts`를 자기 조인(같은 `analysis_id`, 다른 `concept_id`)하고 `analyses`(`DONE`, `deleted_at IS NULL`)·`concepts`(`PUBLISHED`)와 조인해 `COUNT(DISTINCT analysis_id) DESC, wish_count DESC, avg_rating DESC, id ASC`로 정렬
  - `findSameCategoryPopular(category, excludeIds, limit)`: `status='PUBLISHED' AND category = ? AND id NOT IN (...)`, 인기순. `excludeIds`에는 C 자신과 앞에서 고른 카드가 들어가므로 비지 않는다
  - Service가 함께 등장 결과를 먼저 담고 모자란 수만큼 같은 카테고리로 채운다 (쿼리 최대 3번 + 카드 1번)
- 인덱스: 기존 `idx_analysis_concepts_concept(concept_id)`, PK `(analysis_id, concept_id)`, `idx_concepts_status_category`로 충분하다. 카드 수백 개·분석 수백 개 규모라 AC8로 확인한다

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | `/concepts/:id(\\d+)` → `views/concept/ConceptDetailView.vue` (공개, `meta` 없음). concept route 블록에 추가. 이름 `concept-detail` |
| 컴포넌트 | `ConceptCard.vue`(F-00b, 관련 카드에 그대로 사용. 공통 컴포넌트에 축소형 prop을 새로 만들지 않는다), `components/concept/MarkdownBody.vue`(본문 렌더러), `components/concept/CodeBlock.vue`(예제 코드 + 복사), `components/concept/ConceptAppearances.vue`(내 코드 등장 위치) — B 영역 새 파일 |
| 렌더러 | `components/concept/markdown.js`의 `renderMarkdown(text)` → 블록 배열(`{type:'h3'\|'h4'\|'p'\|'ul'\|'pre', ...}`)을 만들고 `MarkdownBody.vue`가 템플릿·`h()`로 그린다. 지원 문법: `## `→h3, `### `→h4 (페이지 제목이 h1·절 제목이 h2), `- ` 목록과 두 칸 들여쓴 `- ` 한 단계 중첩, ```` ``` ```` 코드 블록, 빈 줄로 나눈 문단, 인라인 `` `코드` ``·`**굵게**`. 링크·표·번호 목록·인용·HTML은 글자 그대로 (시드 85개가 쓰는 문법 기준: `##` 121회, `-` 485회, 중첩 7회, `**` 35회, 링크·표·번호 목록 0회) |
| store | 없음. `useAuthStore`의 토큰은 axios 인터셉터가 붙인다 |
| API 함수 | `src/api/concepts.js`에 `fetchConcept(id)` 추가 (F-09가 만든 파일) |
| 뒤로 가기 | `router.options.history.state.back`이 `/concepts`(쿼리 포함, 상세 경로 제외)이면 `router.back()`, 아니면 `router.push('/concepts')` (AC12) |
| 상태별 UI | 로딩 / 404 안내 + 목록으로 / 서버 message (AC13) |
| 반응형 | 본문 최대 폭 760px, 관련 카드 `repeat(auto-fill, minmax(220px, 1fr))`(375px에서 한 열), 코드 블록 `overflow-x: auto`, 헤더의 찜·평점은 좁은 폭에서 이름 아래 줄로 줄바꿈 |

## 6. LLM (AI 기능만)

해당 없음.

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1, AC2 | `ConceptDetailApiTest#AC1_...`, `#AC2_...` (NULL 필드, 제외 필드, DRAFT·HIDDEN·없는 id × 비로그인·로그인) | MockMvc + Testcontainers 통합 |
| AC3, AC4, AC5 | `ConceptDetailApiTest#AC3_...`~`#AC5_...` (회원 2명, 분석 상태·삭제·공개 조합, 연결 11개, `file_id` NULL, 깨진 토큰) | MockMvc 통합 |
| AC6, AC7 | `ConceptDetailApiTest#AC6_...`, `#AC7_...` (함께 등장 횟수·동점 tie-break, DRAFT·삭제·미완료 분석 제외, 같은 카테고리 채우기·중복 없음·4개 미만) | MockMvc 통합 |
| AC3 제목 대체 | `dto/ConceptAppearanceResponseTest#AC3_제목_대체` (대체는 DTO `from`이 한다) | 단위 |
| AC8 | `ConceptDetailApiTest#AC8_...` (시드 적재, 1회 워밍업 후 측정) | MockMvc 통합 |
| AC9~AC13 | `views/concept/__tests__/ConceptDetailView.spec.js` (`vi.mock('@/api/concepts')`, null 절 숨김, 복사 성공·실패, 등장 위치 링크 2종, `:id` 변경 시 재조회, 뒤로 가기 2종, 로딩·404·에러) | Vitest |
| AC13 반응형 | ui-verifier 375px·1280px (시드 카드 중 긴 예제 코드) | ui-verifier |
| AC14 | `components/concept/__tests__/markdown.spec.js` (지원 문법별, 중첩 목록, 지원하지 않는 문법 글자 그대로, HTML 텍스트 처리) | Vitest 단위 |
| API 함수 | `api/__tests__/concepts.spec.js`에 `fetchConcept` 추가 | Vitest |

## 8. 범위 밖

- 찜 버튼·`wished` 표시·로그인 유도 (F-12). 헤더의 ♥는 숫자만 보여 준다
- 리뷰 작성·목록·리뷰 수, 1인 1리뷰·수정·삭제·길이 제한·목록 페이지 크기 (F-11. 화면 설계서 SC-10 "spec에서 정할 것" 중 리뷰 항목은 F-11 spec으로 넘긴다)
- "이 카드로 글쓰기" 진입 버튼 (F-17에서 결정)
- 관리자의 DRAFT·HIDDEN 카드 미리보기 (F-13)
- 예제 코드 언어 표시·문법 강조 (언어 컬럼이 없음. 화면 설계서의 "언어 표시"는 이번에 뺀다), Markdown 링크·표·이미지
- 카드 간 관계를 저장하는 테이블, 개인화된 관련 카드 (추천은 F-16)
- 조회수 집계

## 9. 결정 필요

- (해결 11-19) 관련 카드 기준: 함께 등장 횟수 우선 + 같은 카테고리 인기순으로 채움 (AC6·AC7, 결정로그)
- (해결 11-19) 본문 렌더링: 의존성 없는 자체 최소 렌더러, `v-html` 금지 (AC14, 결정로그)
- (구현 착수 전) F-00b·F-09 머지 후 `ConceptQueryService`·`ConceptController`·`src/api/concepts.js`의 실제 이름이 이 spec과 다르면 spec을 맞춰 고친다

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] F-00b·F-09 develop 머지 확인 → `git merge origin/develop`
- [x] 브랜치 `feature/F-10-concept-detail`
- [x] Flyway 마이그레이션 (없음)
- [ ] AC별 실패 테스트 작성 (red 확인)
- [x] BE 구현 (green): `ConceptController#detail`, `ConceptService#getDetail`, `ConceptMapper` `findAppearances`·`findCoOccurring`·`findSameCategoryPopular`, `ConceptDetailResponse`·`ConceptAppearanceResponse`, `PUBLIC_GET_PATHS`
- [ ] FE 구현 (green): `fetchConcept`, `markdown.js`·`MarkdownBody`, `CodeBlock`, `ConceptAppearances`, `ConceptDetailView`, 라우트
- [x] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [ ] contract-checker 통과
- [ ] ui-verifier 통과 (데스크톱·모바일)
- [ ] reviewer 지적 사항 반영
- [ ] 산출물 반영: `06-화면설계서.md` SC-10 절(언어 표시 삭제, "spec에서 정할 것" 정리) + 스크린샷(`screens/SC-10-*.png`)
- [ ] PR CI 초록 (공통 파일 변경 → 본문에서 @whans1996 멘션)
- [ ] 상태 DONE (PR 머지 전 마지막 커밋)
