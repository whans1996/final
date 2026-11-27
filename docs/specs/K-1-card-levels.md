# K-1 카테고리 12종·카드 수준별 요약·본문·매칭 버그

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | K-1 (로드맵 트랙 3 카드, 11/28~12/02) |
| 우선순위 | T0 (기획서 7.4b) |
| 영역 오너 | A 김동환. `concept` 패키지·FE 카드 화면·`R__seed_concepts.sql`은 공통·B 파일이라 PR에서 @JoonseobKim 멘션 (기획서 7.5절 ⑤) |
| 관련 필수 요구사항 | ⑤ 개념 카드, ⑬ AI |
| 의존 spec | F-08(개념 추출), F-09(목록), F-10(상세), F-13(관리자 카드), K-3a(시드 형식 검사) |
| 관련 이슈 | #163 concept 스키마 enum, #204 카테고리 원문·라벨 혼용 (J3-08). 구현 PR에 `Closes #163`, `Closes #204` |
| 승인 | 김동환 2026-11-27 |

## 1. 목적

카드가 Spring·Vue 백엔드 6종에 묶여 있어 DB·JS·TS·React·도구·CS 개념은 초안이 경고 없이 버려지고(J3-02), 약어로 쓴 이름은 같은 카드를 찾지 못하며(J3-03), 숨긴 카드에도 다시 연결된다(J3-04). 또 카드 본문이 한 수준뿐이라 입문자와 시니어가 같은 글을 읽는다.
K-1은 ① 카테고리를 12종으로 늘리고 ② 카드에 **수준별 한 줄 요약 3개·수준별 본문 3개** 칸과 API를 만들어 12/02에 B(카드 상세 내 수준 화면, CD-1)에게 넘기고 ③ 개념 매칭 버그 세 가지를 고친다.
시드 카드의 수준별 글은 K-3c가 채운다(기획서 6.1절 "시드는 수준별 본문 3개, AI 초안은 수준별 요약 3개만").

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_…`

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | V15까지 적용된 DB와 시드 85장 | V16과 `R__seed_concepts.sql`을 적용하면 | `concepts`에 `summary_beginner`·`summary_junior`·`summary_senior`(VARCHAR(300) NULL)와 `body_beginner`·`body_junior`·`body_senior`(TEXT NULL)가 생기고 기존 카드는 6칸 모두 NULL이다. `SeedConceptsTest`는 시드 카테고리가 12종 **안에** 있는지(6종과 같은지가 아니라)와 난이도 3단계를 검사한다 |
| AC2 | 카드 C에 `summary_junior`·`body_junior`만 있고 로그인 회원의 `users.level`이 `JUNIOR`일 때 | `GET /api/concepts/{C}` 하면 | 200. 기존 필드에 더해 `viewerLevel: "JUNIOR"`와 `levels` 배열이 온다. `levels`는 언제나 3개이고 순서는 `BEGINNER`·`JUNIOR`·`SENIOR`, 각 항목은 `{level, summary, body}`이며 없는 값은 `null`이다. 비로그인·깨진 토큰이면 `viewerLevel: null`이고 `levels`는 같다 (F-10 `wished`와 같은 규칙) |
| AC3 | 관리자가 | `POST`·`PUT /api/admin/concepts[/{id}]`에 `levels`를 보내면 | 보낸 배열로 6칸을 **통째로** 바꾼다(배열에 없는 수준은 두 칸 모두 NULL). 문자열은 앞뒤 공백을 지우고 빈 값은 NULL. `PUT`에 `levels`가 **없거나 null이면 기존 6칸을 그대로 둔다**(지금 관리자 폼이 이 칸을 몰라도 AI 초안 요약이 지워지지 않게). 응답 `AdminConceptDetailResponse`에도 AC2와 같은 `levels` 3개가 온다 |
| AC4 | 관리자 생성·수정 요청에서 | `levels`에 `summary` 301자, `body` 10,001자, 같은 `level` 두 번, `level`이 `BEGINNER·JUNIOR·SENIOR` 밖, 항목 4개 이상을 보내면 | 400 `COMMON_400`, `details`에 `levels`(또는 `levels[i].summary` 등 해당 칸) 사유. 카드는 바뀌지 않는다 |
| AC5 | 카테고리 12종 = `java·spring·web·security·mybatis·vue·database·javascript·tooling·typescript·react·cs` | 관리자가 `category: "typescript"`로 생성하면 / `"kotlin"`으로 생성하면 / `GET /api/concepts?category=react` 하면 | 201 / 400 `details.category`(메시지에 12종 나열) / `react` 카드만 온다. BE `ConceptCategories.ALL`, 관리자 검증 정규식, concept 스키마 `enum`, FE `CONCEPT_CATEGORIES`, 시드 `CATEGORY_ORDER`가 모두 같은 12종이다 (테스트로 묶는다) |
| AC6 | 개념 추출 프롬프트·스키마 | 프롬프트를 만들고 strict 스키마를 보내면 | `llm-schemas/concept.json`의 `new.category`는 12종 `enum`, `new.difficulty`는 `BEGINNER·JUNIOR·SENIOR` `enum`이고(#163), `new`에 `levelSummaries {beginner, junior, senior}`(셋 다 string, required)가 있다. `prompts/concept/{beginner,junior,senior}.md`는 12종을 나열하고, 카테고리마다 무엇을 넣는지(`tooling` = Git·Gradle·npm·Vite·Docker·환경변수, `cs` = HTTP·DNS·트랜잭션 격리·인덱스 같은 원리)와 `levelSummaries` 작성 규칙(6절)을 담는다. `LlmSchemasTest`가 스키마 `enum`과 `ConceptCategories.ALL`이 같은지 검사한다 |
| AC7 | LLM 응답의 새 초안에 `levelSummaries`가 있을 때 | 저장하면 | 세 값을 `summary_beginner/junior/senior`에 넣고 `body_*`는 NULL이다. `levelSummaries`가 없거나 객체가 아니면 **초안은 그대로 저장**하고 세 칸은 NULL. 각 값은 앞뒤 공백을 지우고 비면 NULL, 300자에서 자른다 |
| AC8 | 새 초안의 `category`가 12종 밖일 때 | 파싱하면 | 소문자로 바꾸고 공백·`.`·`-`·`_`를 뗀 뒤 별칭 표(6절)로 바꾼다. 예: `"JS"`→`javascript`, `"Spring Boot"`→`spring`, `"Vue.js"`→`vue`, `"MySQL"`→`database`, `"Docker"`→`tooling`. 그래도 밖이면(`"kotlin"`) 그 초안만 버리고 WARN 로그에 분석 id·초안 이름·원래 카테고리를 남긴다(코드 본문은 남기지 않는다) |
| AC9 | 기존 카드 `의존성 주입 (DI)`, `CSRF (사이트 간 요청 위조)`, `요청 파라미터 바인딩 (@PathVariable·@RequestParam·@RequestBody)`, `가상 스레드 (Java 21)`, `패턴 매칭 switch와 record 패턴 (Java 21)`이 있을 때 | 새 초안 이름이 `DI` / `사이트 간 요청 위조` / `DI (의존성 주입)` / `@PathVariable` / `Java 21` 이면 | 앞의 셋은 그 카드에 연결된다(괄호 **안** 키, 5.4절 개정). `@PathVariable`은 괄호 안에 `·`가 있어 안 키를 만들지 않으므로 새 초안이 되고, `Java 21`은 두 카드가 같은 안 키를 가져 그 키를 쓰지 않으므로 새 초안이 된다. 이름이 정확히 `DI`인 카드가 따로 있으면 그 카드가 이긴다(전체 이름 > 앞부분 > 괄호 안 순서) |
| AC10 | 숨긴(`HIDDEN`) 카드 H와 검수 대기(`DRAFT`) 카드 D가 있을 때 | 새 초안 이름이 H와 같으면 / D와 같으면 | H: 연결도 새 초안도 만들지 않고 그 항목을 버린다(관리자가 숨긴 개념이 분석·퀴즈·복습에 다시 들어오지 않게, J3-04) / D: 지금처럼 D에 연결한다. F-13 AC10 "숨긴 카드는 다시 만들어지지 않는다"는 그대로다 |
| AC11 | `mock` 프로필 | 분석을 끝까지 돌리면 | mock fixture `llm-fixtures/concept.json`의 새 초안이 `levelSummaries` 3개와 함께 `summary_*`에 저장된다. 관리자가 그 초안을 공개하면 `GET /api/concepts/{id}`의 `levels`에 요약 3개가 보인다 (B가 CD-1 화면을 이 데이터로 만든다) |
| AC12 | (FE) 카테고리 라벨 | 카드 목록 카드·카드 상세·커뮤니티 첨부 카드·첨부 선택 목록을 그리면 | 원문 값 대신 `categoryLabel()`의 라벨을 보인다(`mybatis`→`MyBatis`, `cs`→`CS`, 모르는 값은 원문). `categoryLabel`은 `utils/conceptCategories.js`에서 내보내고 관리자 `labels.js`도 이것을 쓴다. 필터·관리자 폼 선택지는 12개다 (#204) |
| AC13 | 시드 카드 Markdown(K-3a 형식) | 선택 절 `## summary-beginner`·`## summary-junior`·`## summary-senior`·`## body-beginner`·`## body-junior`·`## body-senior`를 쓰고 `seed-build`·`seed-check`를 돌리면 | 생성된 `R__seed_concepts.sql`의 INSERT와 `ON DUPLICATE KEY UPDATE`에 6칸이 들어간다(절이 없으면 NULL). 형식 검사는 카테고리 12종, 수준 요약 300자 이하, 수준 본문 10,000자 이하를 본다. 파싱 → 렌더 왕복이 같다. 기존 85장은 절이 없으므로 다시 생성해도 6칸이 NULL이다 |

## 3. API 계약

> Swagger Tag: `개념 카드`, `관리자` (기존). `docs/api/openapi.json`을 함께 커밋한다. 12/02 넘겨주기 대상은 이 절과 openapi.json이다.

### `GET /api/concepts/{id}` (F-10 확장)

- 권한: 공개 (토큰이 있으면 `viewerLevel`·`wished`를 채움)
- 응답 `200` (기존 필드는 그대로, 아래 두 필드 추가)

```json
{
  "id": 21,
  "name": "의존성 주입 (DI)",
  "category": "spring",
  "difficulty": "BEGINNER",
  "summary": "객체가 필요한 협력 객체를 직접 만들지 않고 스프링 컨테이너가 넣어 주는 방식이다.",
  "body": "## 핵심\n- 생성자 주입을 기본으로 쓴다.",
  "exampleCode": "public TodoService(TodoMapper todoMapper) { this.todoMapper = todoMapper; }",
  "referenceUrl": "https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html",
  "wishCount": 3,
  "wished": false,
  "avgRating": 4.5,
  "appearances": [],
  "related": [],
  "viewerLevel": "JUNIOR",
  "levels": [
    { "level": "BEGINNER", "summary": "필요한 부품을 내가 사 오지 않고 스프링이 가져다 끼워 주는 방식이에요.", "body": null },
    { "level": "JUNIOR", "summary": "생성자 파라미터로 받은 빈을 컨테이너가 주입하고, @RequiredArgsConstructor가 그 생성자를 만들어 줍니다.", "body": "## 핵심\n- 빈은 타입으로 찾고, 같은 타입이 둘이면 @Qualifier로 고른다.\n## 주의\n- 필드 주입은 테스트에서 대역을 넣기 어렵다." },
    { "level": "SENIOR", "summary": "생성 책임을 컨테이너로 옮겨 구현 교체·테스트 대역을 쉽게 하는 대신, 객체 그래프가 런타임 설정에 숨는다.", "body": null }
  ]
}
```

- `viewerLevel`: 로그인 회원의 `users.level`. 비로그인·깨진 토큰이면 `null`.
- 화면 규칙(B, CD-1): `levels`에서 `viewerLevel`의 `body`가 있으면 그것을 본문으로, 없으면 공통 `body` + 그 수준 `summary`를 보인다(기획서 6.1절). 다른 수준으로 바꿔 보기는 B가 정한다.
- 에러: 기존과 같음 (404 `CONCEPT_404`).

### `POST /api/admin/concepts` · `PUT /api/admin/concepts/{id}` (F-13 확장)

- 권한: ADMIN
- 요청: 기존 `AdminConceptRequest`에 선택 필드 `levels` 추가

```json
{
  "name": "의존성 주입 (DI)",
  "category": "spring",
  "difficulty": "BEGINNER",
  "summary": "객체가 필요한 협력 객체를 직접 만들지 않고 스프링 컨테이너가 넣어 주는 방식이다.",
  "body": "## 핵심\n- 생성자 주입을 기본으로 쓴다.",
  "exampleCode": null,
  "referenceUrl": null,
  "status": "PUBLISHED",
  "levels": [
    { "level": "BEGINNER", "summary": "필요한 부품을 스프링이 가져다 끼워 주는 방식이에요.", "body": null },
    { "level": "JUNIOR", "summary": "생성자로 받은 빈을 컨테이너가 주입합니다.", "body": "## 핵심\n- 빈은 타입으로 찾는다." }
  ]
}
```

- `levels`: 0~3개, `level`은 서로 달라야 한다. 있으면 6칸을 통째로 바꾸고(위 예에서 SENIOR 두 칸은 NULL), 없거나 `null`이면 `PUT`은 기존 값을 유지하고 `POST`는 모두 NULL. `summary` 300자·`body` 10,000자 이하, 빈 값은 NULL.
- 응답: POST `201 {"id": 92}` / PUT `200` + `AdminConceptDetailResponse` (기존 필드 + `levels` 3개, AC2와 같은 형식)
- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 400 | COMMON_400 | AC4·AC5 검증 실패 (`details`에 필드별 사유) |
| 그 밖 | | F-13과 같음 (401·403·404 `CONCEPT_404`·409 `CONCEPT_003`) |

### 카테고리 값 (`GET /api/concepts?category=`, 관리자 요청·응답, 카드 응답의 `category`)

`java`·`spring`·`web`·`security`·`mybatis`·`vue`·`database`·`javascript`·`tooling`·`typescript`·`react`·`cs`. Swagger `description`·`allowableValues`를 12종으로 고친다. 목록 API의 없는 값은 지금처럼 빈 목록.

## 4. DB 변경

- Flyway: `V16__concept_levels.sql`

```sql
ALTER TABLE concepts
    ADD COLUMN summary_beginner VARCHAR(300) NULL COMMENT '수준별 한 줄 요약 (K-1)' AFTER summary,
    ADD COLUMN summary_junior   VARCHAR(300) NULL AFTER summary_beginner,
    ADD COLUMN summary_senior   VARCHAR(300) NULL AFTER summary_junior,
    ADD COLUMN body_beginner    TEXT NULL COMMENT '수준별 본문 (시드·관리자, K-1)' AFTER body,
    ADD COLUMN body_junior      TEXT NULL AFTER body_beginner,
    ADD COLUMN body_senior      TEXT NULL AFTER body_junior;
```

- `category`는 `VARCHAR(50)`이라 바꾸지 않는다(허용값은 애플리케이션이 검사).
- `R__seed_concepts.sql`: 생성기(AC13)로 다시 만들어 INSERT 열 목록과 `ON DUPLICATE KEY UPDATE`에 6칸을 넣는다. 85장 내용은 바뀌지 않는다(6칸 NULL). 시드 upsert가 관리자 수정분을 덮어쓰는 규칙(결정로그 11-18)은 6칸에도 같다.
- 사용 테이블: `concepts`, `users`(`level`), `analysis_concepts`(매칭 결과).
- 산출물: `docs/산출물/03-ERD.md`, 기획서 ERD의 `concepts` 행.

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | 새 화면 없음. 카드 상세의 내 수준 화면은 B(CD-1, F-29 개편)가 만든다 |
| 바꾸는 파일 | `utils/conceptCategories.js`(12종 + `categoryLabel` 내보내기), `components/concept/admin/labels.js`(위 함수 재사용), `components/concept/ConceptCard.vue:22`, `views/concept/ConceptDetailView.vue:133`, `components/community/PostAttachment.vue:36`, `components/community/AttachmentPicker.vue:182` (모두 `categoryLabel()`로) |
| 라벨 | `java` Java · `spring` Spring · `web` Web · `security` Security · `mybatis` MyBatis · `vue` Vue · `database` Database · `javascript` JavaScript · `tooling` Tooling · `typescript` TypeScript · `react` React · `cs` CS |
| API 함수 | `src/api/concepts.js`의 상세 응답 JSDoc에 `viewerLevel`·`levels` 추가 (호출 코드는 그대로) |
| 상태별 UI | 바뀌지 않음 |

고치는 B 화면은 라벨 한 곳씩만 바꾸고 Tailwind 이전은 하지 않는다(그 화면 이전은 B의 CD-1).

## 6. LLM (AI 기능만)

| 항목 | 내용 |
|---|---|
| 파이프라인 단계 | 개념 추출 (F-08) |
| 프롬프트 파일 | `backend/src/main/resources/prompts/concept/{beginner,junior,senior}.md` |
| 입력 | 바뀌지 않음 (카드 목록·파일 목록·코드) |
| 출력 JSON 스키마 | `llm-schemas/concept.json` (아래 차이) |
| Mock fixture | `backend/src/main/resources/llm-fixtures/concept.json`의 새 초안(`GraphQL API (DGS)`)에 `levelSummaries` 3개 추가. 기존 fixture 출처(dev)를 그대로 쓰고 eval 분할은 쓰지 않는다 |
| 캐시·재사용 | 없음 (같은 호출에서 함께 생성, 런타임 생성·캐시 테이블 없음) |

스키마 `new` 객체 차이:

```json
{
  "category": { "type": "string", "enum": ["java", "spring", "web", "security", "mybatis", "vue", "database", "javascript", "tooling", "typescript", "react", "cs"] },
  "difficulty": { "type": "string", "enum": ["BEGINNER", "JUNIOR", "SENIOR"] },
  "levelSummaries": {
    "type": "object",
    "additionalProperties": false,
    "required": ["beginner", "junior", "senior"],
    "properties": {
      "beginner": { "type": "string" },
      "junior": { "type": "string" },
      "senior": { "type": "string" }
    }
  }
}
```

`levelSummaries` 작성 규칙(프롬프트 세 파일 공통, 기획서 6.1절 수준 기준표): 개념 자체를 그 수준 독자에게 한 문장(300자 이내)으로. beginner = 비유·용어 풀이, junior = 실제 클래스·메서드·동작 순서, senior = 기본 동작을 생략하고 왜 이렇게 설계됐나·대가(포기한 것). 이 프로젝트 코드 얘기는 `contextNote`에만 쓴다.

별칭 표(AC8, 정규화 = 소문자 + 공백·`.`·`-`·`_` 제거 후 비교. `ConceptCategories`에 둔다):

| 결과 | 별칭 |
|---|---|
| database | db, sql, mysql, mariadb, rdb, rdbms, jpa, jdbc |
| javascript | js, ecmascript, node, nodejs |
| typescript | ts |
| react | reactjs |
| vue | vuejs, pinia |
| spring | springboot, springframework |
| tooling | git, docker, gradle, maven, npm, vite, build, tool, tools, devops |
| java | jvm |

머지 순서: 프롬프트를 바꾸므로 **EV-1 기준선(12/01) 측정 뒤 머지**한다(점검 문서 엉킴 방지 규칙 4). 12/02 넘겨주기에 맞춘다.

### 5.4절 이름 비교 개정 (F-08 spec 5.4절을 이 PR에서 고친다)

키 = 소문자 + 공백 제거. 카드·초안마다 ① 전체 이름 ② 끝 괄호 `(…)`를 뗀 앞부분 ③ 끝 괄호 **안**(2~30자, `·`·`,`·`/`가 없을 때만)을 만든다. 기존 카드의 ③ 키는 **다른 카드의 ③ 키와 겹치면 둘 다 쓰지 않는다**. 찾을 때 우선순위는 전체 이름 → 앞부분 → 괄호 안. **괄호 안 키끼리는 비교하지 않는다**(초안 ③은 카드 ①·②와만, 카드 ③은 초안 ①·②와만. `파일 크기 제한 (MultipartFile)`이 `파일 업로드 (MultipartFile)`에 붙지 않게, 리뷰 반영 11-28). 같은 키에 숨긴 카드와 숨기지 않은 카드가 함께 맞으면 숨기지 않은 카드에 연결하고, 맞는 카드가 숨긴 카드뿐일 때만 버린다. 같은 카드로 판정된 카드가 `HIDDEN`이면 그 항목을 버린다(AC10). 저장 때 초안 INSERT가 UNIQUE 충돌(콜레이션만 같은 이름, 카탈로그를 읽은 뒤 숨긴 카드)로 `HIDDEN` 카드에 닿아도 연결하지 않는다(구현 중 추가, AC10과 같은 목적). 같은 응답의 초안끼리는 ①·②만 비교한다(괄호 안만 같은 서로 다른 개념을 합치지 않게).

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1 | `common/SeedConceptsTest` (카테고리 ⊆ 12종, 6칸 NULL), `concept/ConceptMapperTest#AC1_…`(6칸 읽기·쓰기) | 통합 (Testcontainers) |
| AC2 | `concept/ConceptDetailApiTest#AC2_…` (로그인 JUNIOR·비로그인·깨진 토큰, 순서·null) | MockMvc 통합 |
| AC3·AC4 | `concept/admin/AdminConceptApiTest#AC3_…`·`#AC4_…` (통째 교체·PUT 생략 유지·빈 값 NULL / 301자·10,001자·중복·잘못된 level·4개) | MockMvc 통합 |
| AC5 | `concept/admin/AdminConceptApiTest#AC5_…`, `concept/ConceptListApiTest#AC5_…`(category=react), `analysis/concept/ConceptCategoriesTest`(새 파일, 12종·별칭), `utils/__tests__/conceptCategories.spec.js`, `scripts/test/seed-format.test.mjs`(CATEGORY_ORDER) | MockMvc·단위·Vitest·node:test |
| AC6 | `llm/LlmSchemasTest#AC6_…`(enum = ALL, levelSummaries required), `analysis/concept/ConceptPromptsTest#AC6_…`(세 파일에 12종·levelSummaries) | 단위 |
| AC7·AC8·AC9·AC10 | `analysis/concept/ConceptResponseParserTest#AC7_…`~`#AC10_…`, `analysis/concept/ConceptLinkServiceTest#AC7_…`(6칸 INSERT) | 단위·통합 |
| AC11 | `analysis/concept/ConceptExtractorTest#AC11_…`(mock fixture 파싱) + `concept/ConceptDetailApiTest#AC11_…` (mock 분석 → 초안 요약 3개 → 공개 → 상세 `levels`) | 통합 |
| AC12 | `components/concept/__tests__/ConceptCard.spec.js`, `views/__tests__/ConceptDetailView.spec.js`, `components/community/__tests__/PostAttachment.spec.js`·`AttachmentPicker.spec.js`, `AdminConceptForm.spec.js`·`ConceptListView.spec.js`(선택지 12개) | Vitest |
| AC13 | `scripts/test/seed-format.test.mjs`·`seed-build.test.mjs`·`seed-check.test.mjs` (선택 절 파싱·렌더 왕복·SQL 6칸·길이 검사) | node:test |

## 8. 범위 밖

- **목록 기본 정렬·추천에서 내 수준 카드를 앞에 두기** → K-2 (사용자 결정 11-27, 로드맵 K-2 행). 추천 F-16의 수준 적합도 점수는 그대로
- 카드 상세의 내 수준 본문·요약 **화면** → B CD-1 (12/03~12/08)
- 시드 카드 수준별 요약·본문 **내용** (기존 85장 포함) → K-3c·시드 2차
- 관리자 폼에서 수준 칸 편집 UI → S-1 (지금은 API로만, AC3 `levels` 생략 시 유지)
- 분석 개념 탭(`GET /api/analyses/{id}/concepts`)에 수준 요약 보이기, 카드 목록 응답에 수준 요약 추가
- 카탈로그를 스택·카테고리로 먼저 거르기(K-2), 별칭(aliases) 칼럼·임베딩 매칭
- 이미 HIDDEN 카드에 연결된 과거 `analysis_concepts` 정리 (조회 쪽은 F-08 AC7이 이미 거른다)
- 고치는 B 화면의 Tailwind 이전 (CD-1)

## 9. 결정 필요

- 없음 (11-27 질문 4개: 열 6개 + `levels` 3개 응답, 관리자 `levels` 생략 시 유지, 별칭 정규화 + 경고 후 버림, 내 수준 정렬은 K-2. 결정로그 11-27)

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/K-1-card-levels`
- [x] Flyway `V16__concept_levels.sql` (`node scripts/next-migration.mjs`로 번호 재확인)
- [x] AC별 실패 테스트 작성 (red 확인)
- [x] BE 구현 (green): ConceptCategories(12종·별칭), 파서(levelSummaries·별칭·괄호 안 키·HIDDEN), ConceptLinkService INSERT, 상세·관리자 DTO·Mapper, 스키마·프롬프트 3개·fixture
- [x] 시드 도구 (card.mjs·seed-build·seed-check) + `R__seed_concepts.sql` 다시 생성
- [x] FE 구현 (green): 카테고리 12종·`categoryLabel` 4곳·JSDoc
- [x] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [x] contract-checker 통과
- [x] ui-verifier 통과 (라벨 바뀐 화면: 카드 목록·상세·커뮤니티 글 첨부, 데스크톱·모바일)
- [x] reviewer 지적 사항 반영
- [x] 산출물·문서 반영: 03 ERD, 07 API(생성), 09 데이터셋 DS-05 카테고리, 01 요구사항 정의서 K-1 행, 기획서 ERD, F-08 spec AC5·5.4절, F-09·F-13·K-3a spec의 6종 문구
- [ ] PR 본문 `Roadmap: K-1`, `Closes #163`, `Closes #204`, 공통 코드 변경 @JoonseobKim
- [ ] EV-1 기준선(12/01) 뒤 머지
- [ ] PR CI 초록
- [ ] 상태 DONE (PR 머지 후)
