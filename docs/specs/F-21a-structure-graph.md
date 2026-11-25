# F-21a 구조 시각화: 파일 의존 그래프 ("구조" 탭)

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-21a (F-21을 a 파일 의존 그래프 / b 메서드 단위 흐름으로 나눔, 11-24) |
| 우선순위 | P1 |
| 영역 오너 | A 김동환 (AI·분석·학습 루프) |
| 관련 필수 요구사항 | 명세서 "확장"(구조 시각화). 공개 분석 읽기는 F-19 AC9·AC10 규칙 그대로 |
| 의존 spec | F-06 개요·파일 (DONE, `analysis_files`, 개요 `apis`·`ApiDetector`), F-19 분석 관리 (DONE, `checkViewable`). F-21b가 이 spec의 파서·URL 해석을 재사용한다 |
| 관련 이슈 | 없음 |
| 승인 | whans1996 2026-11-24 |

## 1. 목적

분석 결과는 글(개요·파일 설명)로만 보여서 "어느 파일이 어느 파일을 부르는지, 화면이 어느 API를 부르는지"를 한눈에 보기 어렵다.
SC-07에 **"구조" 탭**을 추가해 선별 파일(최대 30개) 사이의 의존 관계를 레이어별 mermaid 그림으로 보여 준다.
원칙은 **보여 주는 선은 코드에서 확정한 것만, 확정하지 못한 것은 숨기지 않고 사유와 함께 목록으로** 이다(11-24 결정). 정규식 추측 대신 Java는 JavaParser AST, JS·Vue는 토크나이저 위에서 해석하고, LLM은 쓰지 않으며, 결과는 저장하지 않고 요청 때 계산한다.
선별되지 않은 파일의 코드는 없으므로(ZIP은 분석 뒤 삭제) 체인이 거기서 끊기며, 그 사실을 "확인하지 못한 연결"에 보여 준다.

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_단일_타입_import로_간선을_만든다()`
> 모든 간선은 `line`(그 연결의 근거가 처음 나온 from 파일의 줄 번호, 1부터)을 가진다. "확인하지 못한 연결"(`unresolved`)의 `reason`은 3.1절 표의 값이다.

### Java (`.java`, JavaParser)

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 선별된 `.java` 파일들이 | 구조를 계산하면 | 각 파일을 JavaParser(`LanguageLevel` JAVA_21)로 파싱하고, 코드에 나오는 **타입 이름**(필드·변수·매개변수·반환·제네릭 인자·`extends`·`implements`·`new`·캐스트·어노테이션·`instanceof`·정적 호출 대상 `Foo.bar()`·`Foo.class`·메서드 참조 `Foo::bar`)을 Java 이름 규칙으로 풀어 다른 선별 파일이면 `IMPORT` 간선을 만든다. 해석 순서: 파일 안 선언(중첩 타입) → 단일 타입 import → 같은 패키지 선별 파일 → 와일드카드 import 패키지의 선별 파일. `import static x.y.Foo.m`은 `Foo`로 간선. 이름이 FQN(`x.y.Foo`)이면 그대로 비교. 주석·문자열·지역 변수 이름은 타입 이름이 아니므로 간선이 생기지 않는다. 자기 자신은 제외 |
| AC2 | AC1 해석이 선별 파일로 풀리지 않을 때 | 구조를 계산하면 | 그 FQN의 패키지가 **프로젝트 루트 패키지**(선별된 모든 Java 파일 패키지의 공통 접두, 2단 이상)로 시작하면 `unresolved`에 `{kind: IMPORT, reason: NOT_SELECTED, detail: FQN}`(파일당 같은 FQN 한 번). 그 밖(외부 라이브러리·`java.*`)은 아무것도 남기지 않는다. 루트 패키지가 2단 미만이면 NOT_SELECTED를 만들지 않는다 |
| AC3 | `.java` 파일이 파싱되지 않을 때(문법 오류·지원하지 않는 문법) | 구조를 계산하면 | 그 파일에서 나가는 Java 간선은 없고 `unresolved`에 `{kind: PARSE, reason: PARSE_ERROR, detail: 첫 오류 메시지 200자}`. 다른 파일이 그 파일을 가리키는 간선은 정상(타입 이름은 파일 이름·`package`로 안다, `package`는 파싱 실패 시 첫 `package` 줄에서 읽음) |
| AC4 | MyBatis XML(`layer = mapper`, `.xml`)이 | 구조를 계산하면 | XML을 DOM 파서(DTD·외부 엔티티 비활성)로 읽어 `<mapper namespace>`가 선별된 Java 파일 FQN이면 그 Java → XML `MYBATIS` 간선, `resultType`·`parameterType`·`type`·`javaType`·`ofType` 값이 선별된 Java FQN이면 XML → 그 Java `MYBATIS` 간선(line = 그 요소 줄). namespace가 루트 패키지 아래인데 선별되지 않았으면 NOT_SELECTED. XML을 읽지 못하면 PARSE_ERROR |

### JS·Vue (`.js`·`.mjs`·`.vue`, 토크나이저)

| ID | Given | When | Then |
|---|---|---|---|
| AC5 | JS 코드(`.vue`는 `<script>`·`<script setup>` 블록만, 줄 번호는 원본 기준)를 | 토큰으로 나누면 | 문자열(`'` `"`), 템플릿 리터럴(중첩 `${}` 포함), 주석(`//`, `/* */`), 정규식 리터럴, 식별자, 구두점을 구분한다. 주석·문자열 안의 `import`·`axios.get(` 같은 글자는 코드로 보지 않는다. 끝나지 않은 문자열·주석이면 그 파일은 PARSE_ERROR(AC3과 같은 형태) |
| AC6 | 토큰 위에서 `import … from 'm'`, `import 'm'`, `export … from 'm'`, `import('m')`, `require('m')`(m이 문자열 리터럴)를 찾으면 | 모듈 경로를 풀어 | 선별 파일이면 `IMPORT` 간선. `./`·`../`는 그 파일 폴더 기준, `@/rest`는 그 파일 경로에서 가장 가까운 상위 `src/` 기준. 후보 순서 그대로 → `.js` → `.mjs` → `.vue` → `/index.js`. 상대·`@/` 경로가 선별 파일로 풀리지 않으면 `unresolved` `{kind: IMPORT, reason: NOT_SELECTED, detail: 풀린 경로}`. 패키지 이름(`vue`, `axios`)은 무시. `import(변수)`처럼 리터럴이 아니면 무시 |

### FE → API (`HTTP` 간선)

| ID | Given | When | Then |
|---|---|---|---|
| AC7 | JS 토큰에서 | **HTTP 호출**을 찾으면 | 다음만 HTTP 호출로 본다: ① 받는 쪽 R의 `R.get/delete/head/options(url, …)`, `R.post/put/patch(url, …)`, `R.request(config)`, `R(config)` ② `fetch(url, init?)`. R은 `axios`(import한 이름 그대로), 같은 파일에서 `axios.create(…)`로 만든 const, 또는 **선별 파일에서 import한 이름**이 그 파일의 `export default axios.create(…)`·`export const X = axios.create(…)`·`const X = axios.create(…); export default X`에 연결될 때만이다. 그 밖의 `.get(`(예: `Map.get`, `router.push`, `route.path`)은 HTTP 호출이 아니다. config·init은 객체 리터럴의 `url`·`method`·`baseURL` 속성만 읽는다 |
| AC8 | AC7 호출의 url 인자가 | **URL 식**이면 | 값을 계산한다: 문자열, 템플릿(`${}`는 값을 알면 넣고 모르면 그 자리를 동적 조각으로), `+` 연결, 같은 파일의 `const` 식별자(초기값이 URL 식), 선별 파일에서 import한 `export const`. 동적 조각이 들어간 **세그먼트 전체**는 와일드카드 1세그먼트가 된다. `?`·`#` 뒤는 버리고, `http(s)://호스트[:포트]`는 떼어 낸다. 계산 결과 맨 앞이 동적 조각이면(예: `` `${API}/todos` ``에서 API를 모름) 그 호출은 `unresolved` `{kind: HTTP_CALL, reason: URL_UNKNOWN, detail: 원문 80자}` |
| AC9 | AC8로 계산한 경로 앞에 | baseURL을 붙이면 | 받는 쪽이 인스턴스면 `axios.create({ baseURL })`, `axios`면 선별 파일 어디든 `axios.defaults.baseURL = …`, config의 `baseURL`이 있으면 그것을 우선한다. baseURL이 URL 식으로 계산되지 않으면(예: `import.meta.env.VITE_API_URL`) `unresolved` `BASE_URL_UNKNOWN`이고 간선을 만들지 않는다. baseURL·url이 절대 URL이면 경로 부분만 쓴다 |
| AC10 | url이 감싼 함수의 **매개변수**일 때 (`function request(method, url) { return http({ method, url }) }`) | 한 단계 인라인하면 | 같은 파일·선별 파일에서 그 함수(같은 파일 선언이거나 `export`해 import된 것)를 부른 곳마다 실제 인자로 AC8~AC9를 다시 계산하고, 간선의 from·line은 **호출한 파일·줄**이다. 그 함수를 부르는 곳이 없거나 두 단계 이상 감싸면 `URL_UNKNOWN`. 래퍼 함수 본문의 호출 자체는 간선을 만들지 않는다 |
| AC11 | 계산된 FE 요청(메서드, 경로)과 개요 `apis`를 | 비교하면 | 서버 경로 = `server.servlet.context-path`(선별된 `application.yml`·`application.properties`에 리터럴로 있으면) + API path. 세그먼트 단위 비교: API의 `{name}`·`*`은 1세그먼트(`{name:regex}`는 FE 세그먼트가 리터럴이면 정규식까지 맞아야 함), `**`·`{*name}`은 0개 이상, FE 와일드카드는 API의 리터럴 세그먼트와도 맞을 수 있다. 메서드는 같거나 API가 `ANY`. 후보 중 **가장 구체적인 것**(Spring 순서: 리터럴 세그먼트 많은 것 → 변수 적은 것 → `**` 없는 것)을 고르고, 남은 최상위 후보들의 `fileId`가 **모두 같으면** FE 파일 → 그 controller 파일 `HTTP` 간선. 최상위 후보 파일이 둘 이상이면 `AMBIGUOUS`, 후보가 없으면 `NO_MATCHING_API`(detail `GET /api/todos/*`). 메서드를 계산하지 못한 config 호출은 메서드를 빼고 비교해 같은 규칙을 적용한다 |

### 응답·권한

| ID | Given | When | Then |
|---|---|---|---|
| AC12 | 내 분석에 파일이 저장되어 있을 때 | `GET /api/analyses/{id}/structure` 하면 | 200과 3절 응답. `nodes`는 모든 선별 파일(`fileId` 오름차순). `edges`는 `(from, to, type)` 중복을 없애고(line은 가장 작은 값) `from` → `to` → type(`IMPORT`, `MYBATIS`, `HTTP`) 순으로 앞에서 150개(`analysis.structure.max-edges`), 넘으면 `edgesTruncated: true`. `unresolved`는 `fileId` → `line` 순으로 100개(`max-unresolved`), 넘으면 `unresolvedTruncated: true`. 파일이 없으면 세 배열이 비고(진행 중이어도 200), 개요가 없으면 HTTP 판정은 전부 `NO_MATCHING_API`가 아니라 **건너뛴다**(HTTP 간선·HTTP unresolved 없음). 같은 분석은 몇 번 조회해도 같은 응답 |
| AC13 | 공개·DONE 분석 A를 다른 회원 W가 / A가 비공개·삭제·없음·주인 탈퇴 / 토큰 없음 | `GET /api/analyses/{A}/structure` | W: 200 (주인과 같은 내용). 나머지: 404 `ANALYSIS_404` (`checkViewable`), 401 `AUTH_401` |

### FE (SC-07 "구조" 탭)

| ID | Given | When | Then |
|---|---|---|---|
| AC14 | 분석이 `DONE`인 SC-07에서 (주인·공개 분석을 보는 타인 모두) | "구조" 탭(개요 다음, `?tab=structure`)을 열면 | 로딩 문구 → `flowchart LR` 그림: 레이어별 `subgraph`(`LAYER_ORDER` 순, 한국어 라벨), 노드 라벨 = 파일 이름, `IMPORT`·`MYBATIS` 실선, `HTTP` 점선 + `HTTP` 라벨. 간선이 없는 파일은 그림에서 빼고 "연결 없는 파일 n개"(접힘)에 경로를 나열. 그림 위 범례: "실선 = 코드 참조 · 점선 = API 호출 · 코드에서 확정한 연결만 그립니다". `edgesTruncated`면 "연결이 많아 150개까지만 보여 줍니다." |
| AC15 | AC14 화면에서 | 그림 아래 | "연결 목록"(접힘): 줄마다 `TodoView.vue:12 → todoApi.js (코드 참조)`, 양쪽 파일 이름은 파일 탭 링크(`?tab=files&file={id}`). "확인하지 못한 연결 n개"(접힘, `unresolved`가 있을 때만): 줄마다 `파일:줄 · 사유 한국어 문구 · detail`(3.1절). 간선이 0개면 그림 대신 "파일 사이 연결을 찾지 못했습니다."와 레이어별 파일 목록(확인하지 못한 연결 목록은 그대로). API 실패면 서버 `message`와 "다시 시도". 375px에서 페이지 가로 스크롤 없음(그림은 그림 상자 안에서만 가로 스크롤) |
| AC16 | 그림 아래 | "mermaid 코드 보기"(접힘)를 펼치고 "복사"를 누르면 | 원본을 `<pre>`로 보여 주고 클립보드에 복사. 성공 "복사했습니다.", 클립보드가 없거나 실패하면 "복사하지 못했습니다. 코드를 직접 선택해 복사해 주세요." (`InviteCodeBox` 방식) |
| AC17 | 파일 이름에 `"`, `;`, `#`, `<`, `>`, `` ` ``, 줄바꿈이 있거나 mermaid 렌더링이 실패할 때 | 원본을 만들고 그리면 | `utils/mermaid.js`(순수 함수)가 라벨 특수 문자를 엔티티(`#quot;`·`#59;`·`#35;`·`#lt;`·`#gt;`·`#96;`, 라벨 안 `%%{…}%%` 설정을 막는 `%`→`#37;`(11-24 리뷰))로, 줄바꿈을 공백으로 바꾼다. 노드 id는 `f{fileId}`, subgraph id는 `L_{layer}`. mermaid는 `securityLevel: 'strict'`, `startOnLoad: false`로 쓰고 구조 탭을 열 때만 동적 `import()`. 렌더링 예외면 그림 자리에 "다이어그램을 그리지 못했습니다."를 보여 주고 연결 목록·원본은 그대로 |

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다 (contract-checker, CI가 검사). Swagger Tag: `분석`.

### `GET /api/analyses/{id}/structure`

- 권한: 로그인 필요 · 본인 분석 또는 공개·DONE 분석 (F-19 AC9·AC10, `AnalysisService.checkViewable`)
- 응답 `200`

```json
{
  "nodes": [
    { "fileId": 101, "path": "src/main/java/com/example/todo/TodoController.java", "name": "TodoController.java", "layer": "controller", "role": "할 일 API 요청을 받아 서비스로 넘깁니다" },
    { "fileId": 102, "path": "src/main/java/com/example/todo/TodoService.java", "name": "TodoService.java", "layer": "service", "role": "할 일 생성·조회 규칙을 처리합니다" },
    { "fileId": 103, "path": "src/main/java/com/example/todo/TodoMapper.java", "name": "TodoMapper.java", "layer": "mapper", "role": null },
    { "fileId": 104, "path": "src/main/resources/mapper/TodoMapper.xml", "name": "TodoMapper.xml", "layer": "mapper", "role": "할 일 SQL" },
    { "fileId": 105, "path": "frontend/src/api/todoApi.js", "name": "todoApi.js", "layer": "js", "role": "할 일 API 호출 함수" },
    { "fileId": 106, "path": "frontend/src/views/TodoView.vue", "name": "TodoView.vue", "layer": "vue", "role": "할 일 목록 화면" }
  ],
  "edges": [
    { "from": 101, "to": 102, "type": "IMPORT", "line": 18 },
    { "from": 102, "to": 103, "type": "IMPORT", "line": 14 },
    { "from": 103, "to": 104, "type": "MYBATIS", "line": 1 },
    { "from": 105, "to": 101, "type": "HTTP", "line": 7 },
    { "from": 106, "to": 105, "type": "IMPORT", "line": 3 }
  ],
  "unresolved": [
    { "fileId": 102, "line": 5, "kind": "IMPORT", "reason": "NOT_SELECTED", "detail": "com.example.todo.TodoValidator" },
    { "fileId": 105, "line": 15, "kind": "HTTP_CALL", "reason": "URL_UNKNOWN", "detail": "http.get(url)" }
  ],
  "edgesTruncated": false,
  "unresolvedTruncated": false
}
```

- `role`은 F-06의 한 줄 역할이며 null일 수 있다. `layer`는 `analysis_files.layer` 소문자 값. MYBATIS의 `line`은 from 파일(Java 인터페이스면 1) 기준.
- 에러

| 상태 | 코드 | 언제 |
|---|---|---|
| 401 | AUTH_401 | 토큰 없음·만료 |
| 404 | ANALYSIS_404 | 없는 id, 삭제된 분석, 남의 비공개 분석, 공개여도 DONE이 아니거나 주인이 탈퇴한 남의 분석 |
| 503 | ANALYSIS_007 | 동시 계산 상한(기본 2)이 차서 자리 대기 시간(기본 10초) 안에 계산을 시작하지 못함 (5.3절 4) |

### 3.1 `unresolved` 값과 FE 문구

| kind | reason | FE 문구 |
|---|---|---|
| IMPORT | NOT_SELECTED | 분석에 포함되지 않은 프로젝트 파일을 참조합니다 |
| PARSE | PARSE_ERROR | 코드를 해석하지 못했습니다 |
| HTTP_CALL | URL_UNKNOWN | 요청 주소가 실행 중에 정해져 코드만으로 확정할 수 없습니다 |
| HTTP_CALL | BASE_URL_UNKNOWN | 기본 주소(baseURL)가 환경 설정에서 와 확정할 수 없습니다 |
| HTTP_CALL | NO_MATCHING_API | 분석한 컨트롤러에서 이 요청을 받는 API를 찾지 못했습니다 |
| HTTP_CALL | AMBIGUOUS | 이 요청을 받을 수 있는 API가 여러 파일에 있습니다 |

## 4. DB 변경

- Flyway: 없음
- 사용 테이블: `analysis_files`(path·layer·role·content), `analyses`(권한, `overview.apis`). 계산 결과는 저장하지 않는다 (11-24 결정: 선별 밖 파일의 심볼 색인도 저장하지 않는다)
- 산출물: ERD 변경 없음

## 5. 백엔드 · 프론트엔드

### 5.1 BE

- 의존성: `com.github.javaparser:javaparser-core` (Apache 2.0·LGPL 이중 라이선스, Apache 2.0으로 사용). 출시 2주 이상 지난 최신 3.27.x 이상 정확한 버전. 심볼 해석기(`symbol-solver`)는 쓰지 않는다(AC1의 이름 해석은 직접 구현, F-21b에서 필요하면 그때 판단). 루트 `README.md` 기술 스택 줄에 추가 (데이터가 아니라 라이브러리라 `datasets.json` 대상 아님)
- 패키지 `com.vibenote.analysis.structure`
  - `java/JavaStructureReader`(AC1~AC3), `xml/MyBatisXmlReader`(AC4), `js/JsTokenizer`·`js/JsModuleReader`(AC5·AC6, export·import·const·함수·HTTP 호출 추출), `http/UrlEvaluator`(AC8~AC10), `http/ApiPathMatcher`(AC11)
  - `StructureAnalyzer.analyze(List<AnalysisFile>, List<ApiEndpoint>, String contextPath)` (순수 함수, AC12 정렬·상한), `StructureService`, `StructureController`(`/api/analyses/{id:\d+}/structure`)
  - DTO `AnalysisStructureResponse`·`StructureNode`·`StructureEdge`·`UnresolvedLink`, enum `EdgeType`·`UnresolvedKind`·`UnresolvedReason`
- `ApiDetector`는 바꾸지 않는다. AC11의 `{name:regex}` 비교에 쓰는 정규식은 `Pattern.compile` 실패 시 1세그먼트 변수로 보고, 매칭은 세그먼트 하나(200자 이하)에만 적용한다
- **계산량 상한 (11-24 `/pr-review`)**: 이 API는 공개 분석이면 누구나 반복 호출하고 결과를 저장하지 않으므로, 업로드한 코드가 계산량을 불리지 못하게 한다. URL 식 하나의 피연산자 2,000개·값 2,048자(넘으면 그 식은 `URL_UNKNOWN`), config 식별자 따라가기 16단계, 템플릿 리터럴 중첩 64단계와 정규식 후보 실패 스캔 파일당 1천만 자(넘으면 그 파일 `PARSE_ERROR`), `{name:regex}` 매칭은 글자 읽기 10만 회(넘으면 맞지 않음), `**`는 표 채우기 O(패턴×경로²), XML 내부 엔티티 펼침 10만 자(넘으면 `PARSE_ERROR`), 래퍼 인라인 계산 분석당 1만 회(넘으면 남은 래퍼 안 호출은 `URL_UNKNOWN`), 간선은 모을 때 바로 중복 제거. 줄 번호·이름 조회는 색인으로 선형 시간에 한다
- **확정 규칙 보완 (PR #118 Codex 리뷰)**: 래퍼 인라인은 부른 곳에서 보이는 선언이 래퍼 자신일 때만(매개변수·지역 이름이 가리면 제외, AC10). config의 `...전개` 뒤에 직접 적지 않은 url·method·baseURL은 값을 모른다(AC7). `+`에서 숫자끼리 만나면 동적 조각, 정수가 아닌 숫자 리터럴도 동적 조각(AC8). `{name}.json`·`*.json`처럼 섞인 세그먼트는 세그먼트 전체를 정규식으로 비교(AC11). `const axios = require('axios')`도 axios 받는 쪽(AC7). `if`·`while`·`for`·`with` 조건 괄호를 닫은 `)` 뒤의 `/`는 정규식(AC5). 2차: `axios.create`는 그 자리에서 보이는 axios일 때만 인스턴스(이름 가림 제외, AC7), 객체 리터럴로 계산하지 못한 config의 baseURL은 `BASE_URL_UNKNOWN`(AC9), 템플릿 `${}` 안의 호출도 HTTP 호출(AC7), `.vue` 블록 밖 HTML 주석 안의 `<script>`는 코드가 아님(AC5), Java 지역 타입 이름은 그 선언 범위 안에서만 자기 파일(AC1). 3차: `axios.defaults.baseURL` 대입도 그 자리에서 보이는 axios일 때만(AC9), config의 계산된 키·접근자·메서드는 전개처럼 앞 속성을 지우고 값을 모름(AC7), baseURL을 붙인 뒤 `.`·`..` 세그먼트를 정규화(AC8). 4차: `var`는 둘러싼 함수 전체가 범위(`let`·`const`는 블록, AC7), Java 변수 이름은 보이는 범위에서만 타입 이름을 가림(AC1), URL 최종 값에도 2,048자 상한(AC8). 5차: `catch (e)` 매개변수는 그 블록에서 이름을 가림(AC7), 나머지 매개변수(`...x`)는 래퍼 인라인 때 인자를 넣지 않음(AC10), const config도 `url`·`method`·`baseURL`·계산된 키 쓰기·`delete`·`Object.assign`이 있으면 값을 모름(AC7), context-path는 마지막 값(마지막이 플레이스홀더면 모름, AC11). 6차: 구조 분해 매개변수의 이름도 가림(AC7), for 머리의 `let`·`const`는 그 루프 안에서만(AC7), MyBatis XML의 이진 이름(`Dto$Row`)은 정식 이름으로 찾음(AC4). 7차: 본문에서 다시 대입하는 매개변수는 래퍼 인라인 때 인자로 바꾸지 않음(AC10), 식 자리의 `function f`의 f는 그 함수 안에서만 보임(AC7). 8차: 구조 분해 선언(`const {axios} = …`)의 이름도 가림(AC7). 9차: `class X` 이름도 가림(식 자리는 클래스 안에서만, AC7), 객체 리터럴을 닫은 `}` 뒤의 `/`는 나눗셈(AC5), 경로 비교는 이어진 `**`를 하나로 합치고 고정 세그먼트 수가 요청보다 많으면 표를 만들지 않으며 행 두 개로 채움(context-path 길이와 무관하게 O(경로) 메모리, AC11)
- 설정: `analysis.structure.max-edges: 150`, `analysis.structure.max-unresolved: 100`

### 5.3 전수 점검 반영 (PR #118, 11-24 사용자 결정)

Codex가 9회 재리뷰 동안 같은 종류의 구멍을 회차마다 하나씩 찾았다. 그래서 4개 영역(이름 가림, 토크나이저, 계산량, HTTP 연결 의미)을 전수 점검했다. 원인은 두 곳이 "기본 허용"이라는 데 있다: 모델링한 선언만 가림으로 치고, 상한이 식·비교 단위라 분석 전체 예산이 없다. 하나씩 막지 않고 아래처럼 구조를 바꾼다. 남는 드문 P2는 follow-up 이슈로 넘긴다.

1. **이름은 안전하게 실패 (AC7·AC10)**: `JsDeclarations`가 한 번 훑으며 멤버 이름·객체 키가 아닌 IDENT 중 모델링한 선언이 아닌 **선언 자리**(`( , [ { ... * class function catch let const var` 뒤, `= => :` 앞, 구조 분해 패턴·for 머리·메서드 매개변수·TS 타입 주석 안)에 나온 이름을 그 함수·블록(애매하면 함수 전체)에서 `UNKNOWN` 선언으로 등록한다. `JsModule.lookup`이 UNKNOWN을 돌려주면 receiver·fetch·URL 상수·config·래퍼 부른 곳은 모두 unresolved다. 지나치게 잡으면 확인하지 못한 연결이 늘 뿐 틀린 간선은 생기지 않는다. 함께: 객체·클래스 메서드 매개변수(키워드 이름 `delete(id){}` 포함), 구조 분해 catch, `[url] = …`·`({url} = o)`·`for (url of …)` 재대입, 다시 대입되는 `let`·`var` 래퍼는 인라인하지 않음, 다른 파일의 부른 곳은 최상위 래퍼에만, 부른 곳의 `...전개` 뒤 매개변수는 모름, `with` 안 호출은 unresolved, `axiosNames`는 import로 온 이름만 lookup 실패 때 믿는다. **구현(11-24)**: 선언 자리는 토큰 앞뒤 모양 대신 구조로 찾는다 (매개변수 목록·catch 괄호·구조 분해·for 머리). 단순 이름·구조 분해가 아닌 매개변수·catch 이름(`url: string`)이 `UNKNOWN`, `with` 본문 안은 본문 안에서 선언하지 않은 이름 모두가 `UNKNOWN`이다. 이미 모델링한 가림(매개변수·지역 변수·구조 분해)은 지금처럼 조용히 HTTP가 아니고, `UNKNOWN`이 가렸을 때만 `JsModule.lookupKnown`(UNKNOWN·with 무시)으로 바깥 이름을 보고 그것이 HTTP 받는 쪽·fetch·래퍼였으면 `URL_UNKNOWN`이다. 키워드·계산된·문자열·`#` 이름 메서드도 매개변수를 등록한다. 선언 초기화(`let x = …`)는 다시 대입으로 세지 않는다. 템플릿 `${}` 식 안에 선언 토큰(`const`·`let`·`var`·`function`·`class`·`catch`·`=>`)이 있으면 그 식의 이름을 모두 식 범위의 `UNKNOWN`으로 둔다 (Codex 10차 P2, 정확한 범위 모델링은 #121). 테스트 `StructureAnalyzerTest` "5.3절 1" 9개.
2. **TS 블록 제외 (AC5, spec 8절과 일치)**: `.vue`의 `<script lang="ts|tsx">` 블록은 읽지 않고 그 파일을 `PARSE_ERROR`(TS 미지원)로 둔다. **구현(11-24)**: `lang` 속성 이름·값은 대소문자를 무시하고, 따옴표 없는 값도 본다. 오류 줄은 그 여는 태그 줄이다. `data-lang` 같은 다른 속성은 보지 않는다.
3. **분석 전체 작업 예산 (5.1 계산량 상한 보강)**: 요청 하나에 `WorkBudget`(단계 수 상한 + 마감 시각) 하나를 두고 토큰화·선언 읽기·호출 분류·URL 계산·경로 비교·Java 이름 해석의 반복마다 차감한다. 다 쓰면 예외를 파일 단위로 잡아 남은 파일은 `PARSE_ERROR`, 남은 호출은 `URL_UNKNOWN`으로 두고 응답의 truncated를 켠다. 파일마다 `RuntimeException`·`StackOverflowError`를 잡아 `PARSE_ERROR`로 둔다 (500 금지). 메모리: JS 파일당 토큰 수 상한(넘으면 `PARSE_ERROR`), 한 글자 구두점 문자열 공유, 템플릿 원문 복사 대신 위치, JavaParser `setStoreTokens(false)`와 CU를 쓰고 바로 버림, unresolved는 상한 + 넘침 표시만 유지. context-path 세그먼트는 API마다 복사하지 않고 한 목록을 이어 본다 (Codex 10차 P1). 제곱 경로는 색인으로 바꾼다: `lookup`(이름·범위 색인), `importOf`(local→import 맵), `functionScopeOf`(둘러싼 함수 스택), `expressionEnd` 재계산, `callSites`(receiver 이름별 호출 색인, 예산 먼저 확인), receiver·config·const 값 메모, `ApiPathMatcher` 패턴은 분석마다 한 번 파싱·컴파일, Java `declaredAround`·`variableAt`은 노드별 이름 집합 선계산, `scriptBlocks` 한 번 훑기, `excerpt`는 잘라 낸 뒤 공백 정리, 재귀(`declarators`·`dottedName`)는 반복문으로. **구현(11-24)**: 예산 2천만 단계·3초(`analysis.structure.max-work-steps`·`max-work-time`), 켜는 표시는 `edgesTruncated`. JS 토큰은 파일당 50만·분석당 200만, URL 식·config는 4,096토큰 넘으면 계산하지 않음(중첩 호출이 호출마다 남은 전체를 훑지 않게). JavaParser `storeTokens(false)`는 노드 줄 번호가 사라져 쓰지 않고, 대신 AST를 파일마다 두 번 파싱(타입 이름 → 참조)해 바로 버린다. 테스트 `StructureWorkloadTest`(기본 단계 예산 안·잘림 없음).
4. **결과 캐시 (#117 흡수, 8절 변경)**: 분석 id 단위로 계산 결과를 메모리에 둔다 (크기 상한 LRU, 같은 분석 동시 계산은 하나만, 전체 동시 계산 수 제한). 분석이 DONE이 된 뒤 선별 파일이 바뀌는 경로가 없는지 먼저 확인하고, 삭제(soft delete)되면 비운다. DB 저장은 하지 않는다. **구현(11-24)**: 분석 파일은 SELECTING 때 넣고 역할·개요는 EXPLAINING 때 쓴 뒤 삭제만 있어, **DONE 분석만** 캐시한다 (`StructureCache`, LRU `cache-size: 64`). 열람 권한은 캐시와 상관없이 매 요청 `AnalysisService.checkViewableDetail`로 본다. 같은 분석의 동시 요청은 먼저 온 계산을 기다린다. 계산(파일 읽기 포함)은 전체 `max-concurrent: 2`개까지이고, `queue-timeout: 10s` 안에 자리가 나지 않으면 `ANALYSIS_007`(503, 사용자 결정 11-24). 작업 예산을 다 쓴 결과는 시간에 따라 달라질 수 있어 캐시하지 않는다. 삭제는 `AnalysisDeletedEvent`를 커밋 뒤 받아 비운다. 테스트 `StructureServiceTest` 8개, `StructureControllerTest#AC12_분석을_지우면_…`.
5. **흔한 코드의 틀린 간선 (AC8·AC9·AC11)**:
   - `fetch(BASE + p)`처럼 매개변수가 붙은 래퍼는 부른 곳이 있으면 인라인을 먼저 한다. 동적 조각이 호스트 자리에 있으면 `URL_UNKNOWN`.
   - 절대 URL의 호스트가 localhost도 아니고 설정된 baseURL 호스트도 아니면 `NO_MATCHING_API` (detail에 호스트).
   - 요청에 API 리터럴과 맞선 와일드카드가 있거나 메서드를 모르면, 가장 구체적인 후보 고르기는 후보가 모두 한 파일일 때만 한다. 아니면 `AMBIGUOUS`.
   - 순위는 `**`·`{*x}` 없는 것 → 리터럴 많은 것 → 변수 적은 것 순, `*`는 `{v}`보다 무겁게 친다 (Spring 순서).
   - `axios.defaults.baseURL` 쓰기가 둘 이상(값이 다름)이거나 `+=`·`Object.assign(X.defaults…)`·`X.defaults['baseURL']`·`X.defaults = …`·인스턴스의 `api.defaults.baseURL = …`이면 `BASE_URL_UNKNOWN`. 같은 파일에서 `axios.defaults.baseURL`을 쓰고 config 없이 `axios.create`해도 `BASE_URL_UNKNOWN`. `window.fetch =`·`globalThis.fetch =`·`fetch =` 대입, `interceptors.request.use`가 있으면 그 receiver 호출은 unresolved.
   - `ContextPathReader`: `spring.profiles`가 맵(`spring.profiles.active`)이면 프로필 문서로 보지 않는다.
   - `ApiDetector` 50개 잘림·상수 매핑 건너뜀이 있으면 변수 없는 일치만 확정한다.
   - **구현(11-24)**:
     - 경로에 매개변수가 붙어 계산된 요청도 부른 곳이 있으면 인라인하고, 부른 곳이 없으면 와일드카드로 비교한다.
     - 동적 조각은 놓인 URL 구성 요소 안에 머문다고 본다. 호스트 이름 자리에 있을 때만 URL_UNKNOWN(baseURL이면 BASE_URL_UNKNOWN)이고, 스킴·userinfo·포트의 조각은 경로 계산에 영향이 없다.
     - 로컬 호스트는 `localhost`·`127.0.0.1`·`[::1]`이다. baseURL 호스트는 선별 파일의 `axios.defaults.baseURL`·`axios.create` baseURL 중 계산되는 것이고, 대소문자를 무시하고 포트는 보지 않는다. NO_MATCHING_API의 detail은 `GET host/path`다.
     - "API 리터럴과 맞선 와일드카드"는 정규식 변수·섞인 세그먼트와 맞선 경우도 포함한다. 와일드카드가 정규식 없는 변수·`**`와만 맞서도 맞는 후보는 확실한 후보로 본다.
     - 순위의 변수 무게는 `*`가 100, `{v}`가 1이다.
     - `defaults` 쓰기 중 `delete X.defaults.baseURL`도 BASE_URL_UNKNOWN이다. 다른 문자열 키(`['timeout']`)와 `Object.assign(X.defaults.headers, …)`는 제외한다.
     - `create` config에 baseURL이 없으면(config가 없을 때만이 아니라) 같은 파일의 axios 기본값을 물려받는 것으로 본다.
     - 부른 곳이 넘기지 않은 config 매개변수에 기본값이 있으면 config를 확정하지 않는다 (Codex 11차).
     - Codex 12차: 래퍼가 config 매개변수의 url·method·baseURL 속성을 바꾸면 확정하지 않는다. 템플릿 `${}` 안의 인터셉터·defaults 쓰기·fetch 바꿔 끼우기도 모은다. `require`를 가린 지역 이름이 있으면 `require('axios')`가 아니다. 정규식을 끝까지 보지 못한 후보(입력 200자 초과·읽기 예산 초과)가 있으면 AMBIGUOUS. 같은 method+path에 조건만 다른 컨트롤러는 #122.
     - request 인터셉터가 있으면 그 받는 쪽 호출은 래퍼 인라인 없이 그 자리에서 URL_UNKNOWN이고, response 인터셉터는 보지 않는다.
     - `fetch` 바꿔 끼우기는 선별 파일 어디서든 모든 fetch 호출에 적용한다.
     - ApiDetector의 incomplete는 50개 잘림, 경로를 알 수 없는 메서드·클래스 매핑, 190자 넘는 경로 제외 중 하나라도 있을 때다. `StructureService`가 선별 파일을 다시 훑어 구한다. 이때 확정하지 못한 후보는 AMBIGUOUS로 둔다.
     - 테스트는 `StructureAnalyzerTest` "5.3절 5", `ApiPathMatcherTest`, `ApiDetectorTest#F21a_…`에 있다.
6. **토크나이저 (AC5)**: `<script\b`는 `<script(?=[\s>/])`로, 여는 태그 속성 안의 `>`(따옴표 안) 허용, regex 안 `\` 다음 줄바꿈은 실패, CR 단독·U+2028·U+2029 줄 끝, 값 뒤 TS `!` 다음 `/`는 나눗셈, `?.` 뒤 숫자는 `?`+`.5`. **구현(11-24)**: `scriptBlocks`는 여는 태그·주석·닫는 태그 뒤로만 나아가고, 닫히지 않은 여는 태그(`>` 없음)를 만나면 거기서 멈춘다. 따옴표는 `=` 바로 뒤에서만 값을 연다. CRLF는 한 줄이다. U+2028·U+2029는 문자열 안에 그대로 둘 수 있고(ES2019) CR은 문자열을 끝내지 못한 오류다. `!`는 같은 줄의 값 바로 뒤일 때만 non-null 단언으로 본다(줄이 바뀌면 `!/x/`는 정규식). 테스트 `JsTokenizerTest` "5.3절 2·6" 9개.
7. **넘기는 것 (follow-up #121)**: JSX(`.js` 안 React, 지원 스택 밖이라 지금처럼 `PARSE_ERROR`), 템플릿 `${}` 안의 함수·const 범위, 비엄격 모드 블록 함수 선언(Annex B), HEAD↔GET·끝 슬래시·`axios(url, cfg)`·`postForm` 등 간선이 빠지는 쪽만인 것, 여러 줄 토큰의 끝 줄, `ApiDetector` 클래스 위치 정규식(F-07 코드), config 별칭(`const c2 = cfg`) 변경.

각 항목은 실패하는 테스트를 먼저 쓴다 (`StructureAnalyzerTest`·`JsTokenizerTest`·`ApiPathMatcherTest`·`StructureServiceTest` 등, 계산량은 `assertTimeoutPreemptively`).

### 5.2 FE

| 항목 | 내용 |
|---|---|
| 화면/라우트 | `/analyses/:id?tab=structure` → `AnalysisDetailView.vue` `TABS`에 `{ name: 'structure', label: '구조' }`를 개요 다음에 추가 (타인 화면에도 보임) |
| 컴포넌트 | `components/analysis/AnalysisStructureTab.vue`(AC14·AC15), `components/analysis/MermaidDiagram.vue`(동적 import·렌더·실패 문구·원본 보기·복사, AC16·AC17, F-21b 재사용) |
| 유틸 | `utils/mermaid.js`: `buildStructureMermaid(structure)`, `escapeMermaidLabel(text)`. `utils/structure.js`: `UNRESOLVED_REASON_LABELS`(3.1절) |
| store | 없음 (탭 로컬 상태) |
| API 함수 | `src/api/analyses.js`의 `fetchAnalysisStructure(id)` |
| 의존성 | `mermaid` 12.x, 출시 2주 이상 지난 정확한 버전 고정 (`npm view mermaid time`) |
| 상태별 UI | 로딩 "구조를 불러오는 중입니다…" / 간선 0개 / API 오류 + 다시 시도 / 렌더링 실패 |

## 6. LLM (AI 기능만)

해당 없음 (규칙·파서 기반, 11-24 결정).

## 7. 테스트 계획

테스트 입력은 테스트 코드 안의 작은 가짜 프로젝트(문자열 파일)로 만든다. 평가(eval) 데이터는 쓰지 않는다.

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1~AC3 | `analysis/structure/java/JavaStructureReaderTest#AC1_…` (같은 패키지·단일·와일드카드·static import·FQN·중첩 타입·주석/문자열/지역 변수 오탐 없음) | 단위 |
| AC4 | `xml/MyBatisXmlReaderTest#AC4_…` (외부 엔티티 비활성 포함) | 단위 |
| AC5·AC6 | `js/JsTokenizerTest`, `js/JsModuleReaderTest` (템플릿 중첩, 정규식 리터럴, 주석 속 import, `@/`·index.js 해석) | 단위 |
| AC7~AC11 | `http/UrlEvaluatorTest`, `http/ApiPathMatcherTest`, `StructureAnalyzerTest#AC7_…`~`#AC11_…` — **오탐 방지 케이스 필수**: 라우터 `path: '/login'`·`router.push('/x')`·`Map.get('/x')`는 간선 없음, env baseURL은 BASE_URL_UNKNOWN, 메서드 불일치는 NO_MATCHING_API, `/todos/search`는 `/todos/{id}`가 아니라 `/todos/search` API로, 다른 파일의 동률 후보는 AMBIGUOUS, context-path 반영 | 단위 |
| AC12 | `StructureAnalyzerTest#AC12_…`(정렬·중복·상한·개요 없음) + `StructureControllerTest#AC12_…` | 단위 + MockMvc 통합 |
| AC13 | `StructureControllerTest#AC13_…` | MockMvc 통합 |
| AC14·AC15 | `components/analysis/__tests__/AnalysisStructureTab.spec.js` (`mermaid` mock) | Vitest |
| AC16·AC17 | `components/analysis/__tests__/MermaidDiagram.spec.js`, `utils/__tests__/mermaid.spec.js` | Vitest |
| 화면 | ui-verifier·Chrome(로그인·실데이터)으로 구조 탭 데스크톱·375px. 시연용 샘플 저장소(dev split) 분석으로 간선·unresolved를 사람이 대조 | 수동 |

## 8. 범위 밖

- **F-21b**: API 하나를 골라 화면 → API 함수 → 컨트롤러 메서드 → 서비스 → 매퍼 → SQL까지 메서드 단위 결정적 흐름(시퀀스 다이어그램), F-07 흐름 카드의 다이어그램 전환. 그리고 AI 활용 두 가지(11-24 결정): **구조 해설**(확정된 그래프만 입력으로 수준별 설명, "AI 해설 만들기" 버튼으로 생성·저장)과 **F-07 흐름 검증**(정적 흐름으로 LLM 흐름 단계마다 "코드로 확인됨/확인 안 됨")
- AI가 미확정 연결을 추정해 "AI 추정" 선으로 그리는 것 (F-21b 이후 별도 판단)
- 선별되지 않은 파일의 심볼 색인 저장(11-24 결정으로 하지 않음), LLM 다이어그램, 결과 DB 저장 (메모리 캐시는 5.3절 4로 이번에 한다)
- 두 단계 이상 감싼 래퍼, `fetch`·axios 외 HTTP 클라이언트(ky, ofetch 등), `.ts`·TSX 등 지원 스택 밖 파일
- 노드 클릭으로 파일 열기(mermaid `click`은 `securityLevel: 'loose'` 필요. 대신 AC15 링크), 확대·축소, 이미지 내려받기, 다크 테마 전용 색

## 9. 결정 필요

- 없음

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/F-21a-structure-graph`
- [x] Flyway 마이그레이션 (없음)
- [x] AC별 실패 테스트 작성 (red 확인) — BE AC1~AC13, FE AC14~AC17 (11-24)
- [x] BE 구현 (green), `javaparser-core` 정확한 버전 고정 (3.28.2)
- [x] FE 구현 (green), `mermaid` 정확한 버전 고정 (12.3.0)
- [x] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [x] contract-checker 통과 — spec ↔ openapi ↔ FE 불일치 0 (11-24)
- [x] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일) — 로그인 화면이라 Chrome(1280)·Playwright(375)로 직접 확인 (11-24)
- [x] reviewer 지적 사항 반영 — 반드시 4(계산량 상한)·권장 5 반영 `412c2f8`, 캐시 #117 (11-24)
- [x] 산출물 반영: `06-화면설계서.md` SC-07 탭·구조 탭, `01-요구사항정의서.md` F-21 비고(a·b 분할), `07-REST-API-명세.md`는 openapi.json에서 생성하므로 수정 없음, 루트 `README.md` 기술 스택(JavaParser·mermaid)
- [ ] 전수 점검 반영 (5.3절 1~6, 7은 follow-up 이슈) — 11-24 결정, Codex 9차 3건은 `a94d89e`
- [ ] PR CI 초록
- [ ] 상태 DONE (PR 머지 후)
