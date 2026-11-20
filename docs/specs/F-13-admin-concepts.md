# F-13 관리자 카드 관리 (카드 등록·수정, AI 초안 검수)

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-13 |
| 우선순위 | P0 |
| 영역 오너 | B 김준섭 (카드·소셜) |
| 관련 필수 요구사항 | ⑦ (개념 카드 데이터 관리) |
| 의존 spec | F-00b (concept 공통 기반·관리자 레이아웃·`app.admin.bootstrap-email`), F-08 (AI 초안 `DRAFT·AI` 생성), F-09·F-10 (공개 화면은 `PUBLISHED`만). 시드 카드 85개(`R__seed_concepts.sql`, 카테고리 6종)는 이미 들어가 있어 이 spec은 **관리자 CRUD와 초안 검수**만 다룬다 |
| 관련 이슈 | #51 (`referenceUrl`은 http(s)만 저장, AC5에 흡수). 구현 PR에 `Closes #51` |
| 승인 | 김준섭 2026-11-20 |

## 1. 목적

AI가 분석 중에 만든 초안 카드(F-08, `DRAFT`)는 검수 전까지 아무에게도 보이지 않는다(NF-04, UC24). 관리자가 초안을 고쳐 공개하거나 숨기고, 잘못 추출된 초안은 지운다.
시드에 없는 개념은 관리자가 직접 카드를 만들고, 이미 공개된 카드(시드 포함)의 내용을 고치거나 숨긴다 (UC50).
공개 이력이 있는 카드의 찜·리뷰가 CASCADE로 사라지지 않게, 물리 삭제는 검수 대기(`DRAFT`) 카드에만 허용한다 (결정로그 11-18 "공개 후 제거는 HIDDEN").

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_상태_필터로_DRAFT만_조회한다()`. 모든 API는 ADMIN 토큰(`createAccessToken(id, "ADMIN")`)으로 부른다 (AC10 제외).

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | `DRAFT·AI` 2장, `PUBLISHED·SEED` 3장, `PUBLISHED·ADMIN` 1장, `HIDDEN·SEED` 1장이 있을 때 | `GET /api/admin/concepts?status=DRAFT`, `?source=SEED`, `?status=PUBLISHED&source=ADMIN`, 조건 없이 요청하면 | 각각 2·4·1·7건. 페이지 응답 `{items, page, size, total}`(size 기본 20, 최대 100), 정렬 `updated_at DESC, id DESC`. 항목은 3절 `AdminConceptSummaryResponse`(body·exampleCode 없음). `keyword`는 이름·요약 부분 일치(대소문자 무시, trim 후 50자 이하, `%`·`_`는 이스케이프, F-09와 같은 규칙). 허용되지 않은 `status`·`source` 값은 400 `COMMON_400`과 `details.status`·`details.source`. 범위를 넘은 page는 `items: []`와 실제 `total` |
| AC2 | AC1 데이터에서 | `GET /api/admin/concepts/status-counts`를 요청하면 | 200 `{"DRAFT": 2, "PUBLISHED": 4, "HIDDEN": 1}` (카드가 없는 상태도 0으로 세 키 모두) |
| AC3 | `DRAFT·AI` 카드 D가 서로 다른 분석 2개에 연결되어 있을 때 | `GET /api/admin/concepts/{D}`를 요청하면 | 200과 3절 `AdminConceptDetailResponse`: 모든 컬럼(`body`·`exampleCode`·`referenceUrl`은 NULL이면 null) + `linkedAnalysisCount: 2`(`analysis_concepts`의 그 카드 행 수. PK가 `(analysis_id, concept_id)`라 분석 수와 같고, soft delete된 분석은 연결이 이미 지워져 있다) + `reviewCount`. 상태와 상관없이 조회된다. 없는 id(`999999`)는 404 `CONCEPT_404` |
| AC4 | 유효한 값으로 | `POST /api/admin/concepts`를 보내면 | 201 `{"id": n}`. `source=ADMIN`, `wish_count=0`, `avg_rating=0.0`으로 저장된다. 문자열 필드는 앞뒤 공백을 지우고, 빈 `body`·`exampleCode`·`referenceUrl`은 NULL로 저장한다. `status`는 `DRAFT`·`PUBLISHED`만 받는다(`HIDDEN`이면 400 `details.status`). `PUBLISHED`로 만든 카드는 바로 `GET /api/concepts/{id}`(F-10)로 보인다 |
| AC5 | 생성·수정 요청에서 | 다음 값을 보내면: `name` 없음·공백·101자, `category`가 `java·spring·web·security·mybatis·vue` 밖, `difficulty`가 `BEGINNER·JUNIOR·SENIOR` 밖, `summary` 없음·501자, `body`·`exampleCode` 10,001자, `referenceUrl` 501자 또는 `javascript:alert(1)`·`ftp://x`·`//evil.com` (#51) | 400 `COMMON_400`, `details`에 해당 필드별 사유. 카드는 생기거나 바뀌지 않는다. `referenceUrl`은 `^https?://`로 시작해야 한다(대소문자 무시) |
| AC6 | 이름이 `JWT 인증`인 카드가 있을 때 | 다른 카드를 같은 이름(앞뒤 공백 제거 후 같음)으로 생성·수정하면 | 409 `CONCEPT_002` "같은 이름의 개념 카드가 이미 있습니다.", `details.name`. 동시 요청이 UNIQUE에 걸려도 같은 응답(500 아님). 자기 이름을 그대로 둔 수정은 409가 아니다 |
| AC7 | `DRAFT·AI` 카드 D가 있을 때 | `PUT /api/admin/concepts/{D}`로 내용을 고치고 `status: PUBLISHED`를 보내면 | 200과 수정된 `AdminConceptDetailResponse`. `source`는 `AI` 그대로이고 `wish_count`·`avg_rating`은 바뀌지 않는다(요청에 있어도 무시, DTO에 필드 없음). 이후 F-09 목록·F-10 상세·F-08 분석 개념 탭(`items`)에 D가 나오고 `pendingCount`가 1 줄어든다. 같은 방법으로 `DRAFT→HIDDEN`, `PUBLISHED↔HIDDEN`, `DRAFT→DRAFT`(내용만 저장)가 된다 |
| AC8 | `PUBLISHED`·`HIDDEN` 카드가 있을 때 | `status: DRAFT`로 `PUT` 하면 | 409 `CONCEPT_003` "공개되었거나 숨긴 카드는 검수 대기로 되돌리거나 삭제할 수 없습니다."이고 카드는 바뀌지 않는다 (DRAFT로 되돌린 뒤 지우는 우회로 찜·리뷰가 CASCADE로 사라지는 것을 막는다) |
| AC9 | `source=SEED` 카드 S가 있을 때 | `PUT`으로 `name`을 바꾸면 | 400 `COMMON_400`, `details.name` "시드 카드의 이름은 바꿀 수 없습니다." (시드 upsert가 `name` 기준이라 새 카드가 생긴다). 이름을 그대로 두면 다른 필드·상태는 고칠 수 있다 |
| AC10 | `DRAFT` 카드 D(분석 연결 2개), `PUBLISHED` 카드 P, `HIDDEN` 카드 H가 있을 때 | `DELETE /api/admin/concepts/{id}` 하면 | D는 204, 카드와 `analysis_concepts` 연결이 함께 지워진다(FK CASCADE). P·H는 409 `CONCEPT_003`, 없는 id는 404 `CONCEPT_404`. 지운 초안과 같은 이름은 다음 분석에서 다시 초안이 될 수 있고, 숨긴 카드는 F-08 AC5에 따라 다시 만들어지지 않는다 |
| AC11 | 토큰 없음 / `USER` 토큰으로 | 3절의 모든 `/api/admin/concepts` 요청을 보내면 | 401 `AUTH_401` / 403 `AUTH_403` (`SecurityConfig`의 `/api/admin/**` 규칙. 엔드포인트별로 확인) |
| AC12 | 시드 카드 S를 관리자가 `HIDDEN`으로 숨긴 뒤 | `R__seed_concepts.sql`이 다시 적용되면 | S는 `HIDDEN` 그대로다. 시드 upsert의 `ON DUPLICATE KEY UPDATE`에서 `status`를 빼서, 새 시드 카드는 파일의 `PUBLISHED`로 들어가고 기존 카드의 상태는 관리자 결정을 따른다. 내용(요약·본문 등)은 지금처럼 파일 기준으로 덮어써진다 |
| AC13 | (FE) 관리자가 `/admin/concepts`를 열면 | 상태 탭·출처 필터·검색·page를 바꿀 때 | 상태 탭(검수 대기 n·공개 n·숨김 n·전체, 개수는 AC2), 출처 필터(전체·시드·AI 초안·직접 작성), 검색, 페이지가 URL 쿼리(`?status=&source=&keyword=&page=`)와 함께 바뀌고 새로고침·뒤로 가기에도 유지된다. 기본 탭은 검수 대기(`DRAFT`). 표에는 이름·카테고리·난이도·출처·상태·찜·평점·수정일과 "검수"(DRAFT)/"수정" 버튼. 탭에 카드가 없으면 탭별 문구("검수할 초안이 없습니다." 등). 375px에서는 표 대신 카드형 행 |
| AC14 | (FE) 편집 화면(`/admin/concepts/new`, `/admin/concepts/:id/edit`)에서 | 값을 고치고 "저장"·"공개"·"숨김"을 누르면 | 각각 현재 상태 유지·`PUBLISHED`·`HIDDEN`으로 `PUT`(새 카드는 "초안으로 저장"·"공개"로 `POST`) 후 목록으로 돌아가고 성공 안내를 보인다. 본문은 `MarkdownBody`로 오른쪽 미리보기(375px에서는 "편집/미리보기" 탭). `source=AI`이면 위쪽에 "AI가 만든 초안입니다. 사실관계·예제를 확인한 뒤 공개하세요." + "연결된 분석 n개", `source=SEED`면 "시드 카드입니다. 이름은 바꿀 수 없고, 시드 파일이 바뀌면 내용이 파일 기준으로 다시 덮어써집니다."를 보이고 이름 칸을 읽기 전용으로 둔다. `PUBLISHED`·`HIDDEN` 카드에는 "초안으로 저장"이 없다 |
| AC15 | (FE) 편집 화면에서 | 400 `COMMON_400`·409 `CONCEPT_002`를 받으면 / `DRAFT` 카드의 "삭제"를 누르면 | 서버 `details`의 사유를 해당 필드 아래에 보이고 입력값은 유지된다 / 확인 대화상자("이 초안과 분석 n개의 연결을 지웁니다. 다시 추출되지 않게 하려면 숨김을 쓰세요.") 확인 후 `DELETE` → 목록. "삭제" 버튼은 `DRAFT`에만 보인다. 없는 id(404)는 "카드를 찾을 수 없습니다." + 목록 링크 |

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다. Swagger Tag: **`관리자`** (`docs/conventions.md` 8절). Controller `concept/admin/AdminConceptController`. 권한은 모두 **ADMIN** (`/api/admin/**`).

### `GET /api/admin/concepts?status=&source=&keyword=&page=1&size=20`

- 쿼리: `status` = `DRAFT|PUBLISHED|HIDDEN`(없으면 전체), `source` = `SEED|AI|ADMIN`(없으면 전체), `keyword`(50자 이하)
- 응답 `200`

```json
{
  "items": [
    {
      "id": 91,
      "name": "낙관적 락",
      "category": "spring",
      "difficulty": "SENIOR",
      "summary": "충돌이 드물다고 보고 저장 시점에 버전으로 충돌을 확인하는 동시성 제어 방식이다.",
      "status": "DRAFT",
      "source": "AI",
      "wishCount": 0,
      "avgRating": 0.0,
      "updatedAt": "2026-11-20T14:20:00+09:00"
    }
  ],
  "page": 1,
  "size": 20,
  "total": 3
}
```

### `GET /api/admin/concepts/status-counts`

```json
{ "DRAFT": 3, "PUBLISHED": 85, "HIDDEN": 2 }
```

### `GET /api/admin/concepts/{id}`

```json
{
  "id": 91,
  "name": "낙관적 락",
  "category": "spring",
  "difficulty": "SENIOR",
  "summary": "충돌이 드물다고 보고 저장 시점에 버전으로 충돌을 확인하는 동시성 제어 방식이다.",
  "body": "## 핵심\n- 엔티티에 버전 컬럼을 두고 UPDATE 조건에 넣는다.\n\n## 주의\n- 충돌이 잦으면 재시도 비용이 커진다.",
  "exampleCode": null,
  "referenceUrl": null,
  "status": "DRAFT",
  "source": "AI",
  "wishCount": 0,
  "avgRating": 0.0,
  "reviewCount": 0,
  "linkedAnalysisCount": 2,
  "createdAt": "2026-11-20T14:20:00+09:00",
  "updatedAt": "2026-11-20T14:20:00+09:00"
}
```

### `POST /api/admin/concepts` · `PUT /api/admin/concepts/{id}`

- 요청 (`AdminConceptRequest`, 두 API 공용. PUT은 전체 교체)

```json
{
  "name": "낙관적 락",
  "category": "spring",
  "difficulty": "SENIOR",
  "summary": "충돌이 드물다고 보고 저장 시점에 버전으로 충돌을 확인하는 동시성 제어 방식이다.",
  "body": "## 핵심\n- 엔티티에 버전 컬럼을 두고 UPDATE 조건에 넣는다.",
  "exampleCode": "UPDATE orders SET status = ?, version = version + 1 WHERE id = ? AND version = ?",
  "referenceUrl": "https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html",
  "status": "PUBLISHED"
}
```

| 필드 | 규칙 (trim 후) |
|---|---|
| name | 필수, 1~100자. SEED 카드는 바꿀 수 없음(AC9) |
| category | 필수, `java·spring·web·security·mybatis·vue` (`ConceptCategories` 재사용) |
| difficulty | 필수, `BEGINNER·JUNIOR·SENIOR` |
| summary | 필수, 1~500자 |
| body · exampleCode | 선택, 10,000자 이하, 비면 NULL |
| referenceUrl | 선택, 500자 이하, `^https?://`(대소문자 무시), 비면 NULL (#51) |
| status | 필수. POST는 `DRAFT·PUBLISHED`, PUT은 셋 다(단 AC8) |

> 허용값 검증 실패를 `details`에 필드별로 담으려고 `category`·`difficulty`·`status`는 문자열로 받아 검증한다 (F-09 `ConceptSearchRequest`와 같은 방식).

- 응답: POST `201 {"id": 92}` / PUT `200` + `AdminConceptDetailResponse`

### `DELETE /api/admin/concepts/{id}`

- 응답 `204` (DRAFT만)

### 에러 (모든 엔드포인트 공통 + 해당하는 것)

| 상태 | 코드 | 언제 |
|---|---|---|
| 400 | COMMON_400 | 검증 실패(AC1·AC4·AC5), 시드 카드 이름 변경(AC9). `details`에 필드별 사유 |
| 401 | AUTH_401 | 토큰 없음·만료 (AC11) |
| 403 | AUTH_403 | ADMIN이 아님 (AC11) |
| 404 | CONCEPT_404 | 없는 카드 (AC3·AC10, PUT 포함) |
| 409 | **CONCEPT_002** (신규) | 같은 이름의 카드가 있음, `details.name` (AC6) |
| 409 | **CONCEPT_003** (신규) | `PUBLISHED`·`HIDDEN` 카드를 `DRAFT`로 되돌리거나 삭제 (AC8·AC10) |

새 코드는 `ErrorCode` CONCEPT 블록에 추가하고 `docs/conventions.md` 3절 CONCEPT 행에도 적는다.

## 4. DB 변경

- Flyway: **`V7__concept_source_admin.sql`** (구현 직전 `node scripts/next-migration.mjs`로 번호 재확인)

```sql
ALTER TABLE concepts
    MODIFY source ENUM('SEED','AI','ADMIN') NOT NULL COMMENT 'SEED 시드 파일, AI 분석 초안(F-08), ADMIN 관리자 작성(F-13)';
```

- `ConceptSource`에 `ADMIN` 추가 (concept 공통 기반 파일 → PR에 @whans1996 멘션).
- `R__seed_concepts.sql`: `ON DUPLICATE KEY UPDATE`에서 `status = seed.status` 한 줄을 뺀다 (AC12). 파일 머리 주석의 "주의"도 고친다. 체크섬이 바뀌어 다음 기동 때 한 번 다시 적용된다.
- `ConceptMapper`(Java·XML) **F-09~13 (B) 블록 안에** 추가: `findForAdmin`·`countForAdmin`·`countByStatus`·`insertByAdmin`·`updateByAdmin`·`deleteDraft`(`WHERE id = ? AND status = 'DRAFT'`, 영향 행 수로 판단)·`countLinkedAnalyses`·`findByIdForUpdate`(PUT·DELETE에서 상태 확인과 변경 사이 경쟁 방지).
- 사용 테이블: `concepts`(쓰기), `analysis_concepts`(개수 읽기, A 테이블 — `concept_id` 기준 COUNT만 하므로 ConceptMapper에서 읽는다. CASCADE 삭제는 FK가 한다), `concept_reviews`(개수 읽기, B).

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | `router/routes/concept.js`의 `adminChildren`: `concepts` → `views/admin/concept/AdminConceptListView.vue` (`name: 'admin-concepts'`, `meta.adminNav: { label: '개념 카드', order: 10 }`), `concepts/new` → `AdminConceptEditView.vue` (`admin-concept-new`), `concepts/:id(\\d+)/edit` → 같은 뷰 (`admin-concept-edit`) |
| 컴포넌트 | `components/concept/admin/AdminConceptTable.vue`(표·375px 카드형 행), `AdminConceptForm.vue`(필드·필드별 에러), `AdminConceptStatusTabs.vue`. 재사용: `MarkdownBody`, `CodeBlock`, `PageNav`, `EmptyState`, `utils/conceptCategories.js` |
| store | 없음 (화면 로컬 상태) |
| API 함수 | `src/api/adminConcepts.js`: `fetchAdminConcepts(params)`, `fetchAdminConceptCounts()`, `fetchAdminConcept(id)`, `createAdminConcept(body)`, `updateAdminConcept(id, body)`, `deleteAdminConcept(id)` |
| 상태별 UI | 로딩 "불러오는 중…" / 탭별 빈 문구 / 실패는 서버 `message` + "다시 시도" / 저장 중 버튼 비활성(중복 제출 방지) / 필드 에러는 필드 아래 |

- 권한 처리(비로그인 → `/login?redirect=…`, USER → 홈)는 F-00b 관리자 레이아웃·가드를 그대로 쓴다.
- 산출물: `docs/산출물/06-화면설계서.md` SC-20의 "spec에서 정할 것"을 이 spec 결정으로 바꾸고 스크린샷(목록·편집 × 1280·375)을 넣는다. `03-ERD.md` concepts 행에 `ADMIN` 출처, `02-유스케이스.md` UC50에 삭제·숨김 규칙.

## 6. LLM (AI 기능만)

해당 없음 (초안 생성은 F-08. 이 기능은 LLM을 부르지 않는다).

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1, AC2 | `concept/admin/AdminConceptApiTest#AC1_...`, `#AC2_...` (상태·출처·조합·keyword 이스케이프·정렬·잘못된 값·범위 밖 page / 0개 상태 포함) | MockMvc + Testcontainers 통합 |
| AC3 | `AdminConceptApiTest#AC3_...` (같은 분석 중복 없이 세기, NULL 필드, 404) | MockMvc 통합 |
| AC4, AC5 | `AdminConceptApiTest#AC4_...`, `#AC5_...` (trim·빈 값 NULL·source ADMIN·HIDDEN 생성 거부, 필드별 details, #51 URL 3종) | MockMvc 통합 |
| AC6 | `AdminConceptApiTest#AC6_...` (생성·수정 중복, 자기 이름 유지) + `AdminConceptServiceTest#AC6_UNIQUE_충돌은_CONCEPT_002` (DuplicateKeyException 변환) | 통합 |
| AC7, AC8 | `AdminConceptApiTest#AC7_...`, `#AC8_...` (공개 후 `/api/concepts/{id}` 200·`/api/analyses/{id}/concepts` pendingCount, 전이 4종, DRAFT 되돌리기 409) | MockMvc 통합 |
| AC9 | `AdminConceptApiTest#AC9_...` | MockMvc 통합 |
| AC10 | `AdminConceptApiTest#AC10_...` (DRAFT 삭제 + analysis_concepts 0행, PUBLISHED·HIDDEN 409, 404) | MockMvc 통합 |
| AC11 | `AdminConceptApiTest#AC11_...` (6개 엔드포인트 × 토큰 없음·USER) | MockMvc 통합 |
| AC12 | `common/SeedConceptsTest#AC12_시드_재적용해도_숨긴_카드는_HIDDEN` (시드 SQL을 다시 실행) | 통합 |
| AC13 | `views/__tests__/AdminConceptListView.spec.js` (쿼리 동기화, 탭 개수, 빈 문구) | Vitest |
| AC14, AC15 | `views/__tests__/AdminConceptEditView.spec.js`, `components/concept/admin/__tests__/AdminConceptForm.spec.js` (버튼별 status, 안내문, 이름 읽기 전용, 필드 에러, 삭제 확인·DRAFT에만) | Vitest |
| AC13~15 화면 | ui-verifier `/admin/concepts`, `/admin/concepts/new`, `/admin/concepts/{id}/edit` 1280·375 | Playwright |

## 8. 범위 밖

- 시드 카드 추가·수정 작업 자체 (`R__seed_concepts.sql`은 이미 85개, AC12의 upsert 한 줄만 바꾼다)
- 중복 초안 **병합**(초안의 분석 연결을 기존 카드로 옮기기). 같은 개념의 표기 다른 초안은 숨기거나 지운다
- 처음 추출된 분석으로 가는 링크 (남의 비공개 분석을 관리자에게 보이지 않기 위해 개수만 보인다)
- 헤더의 검수 대기 배지 (개수는 상태 탭에만)
- 관리자의 리뷰 삭제·신고 처리 (F-11 8절에서 넘어온 항목 — 필요하면 follow-up)
- 수정 이력·되돌리기, 여러 카드 일괄 공개, 이미지 첨부
- 편집 중 페이지 이탈 경고
- 공개 시 출처 URL을 DS-01 도메인으로 제한 (시드만 `SeedConceptsTest`로 검사)

## 9. 결정 필요

- 없음 (11-20 사용자 답: DRAFT만 삭제, 출처 ADMIN 추가, 시드 upsert는 status 보존 + 안내, #51 흡수)

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/F-13-admin-concepts`
- [ ] Flyway `V7__concept_source_admin.sql` + `R__seed_concepts.sql` status 보존
- [ ] AC별 실패 테스트 작성 (red 확인)
- [ ] BE 구현 (green)
- [ ] FE 구현 (green)
- [ ] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [ ] contract-checker 통과
- [ ] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일)
- [ ] reviewer 지적 사항 반영
- [ ] 산출물 반영 (화면 설계서 SC-20·ERD concepts·유스케이스 UC50, conventions 3절)
- [ ] PR CI 초록
- [ ] 상태 DONE (PR 머지 후)
