# Backend 규칙

> Java 21, Spring Boot 4.1, Spring Security + JWT(jjwt), MyBatis, MySQL 8, Flyway, springdoc, Jackson 3, Gradle 9.9(wrapper), Spotless(google-java-format AOSP 1.28.0).
> 공통 API 규칙(응답 형식, 에러 코드, 네이밍)은 `../docs/conventions.md`를 따른다.
> F-00에서는 Boot 3.5로 시작했고, 11-18 환경 최신화 결정으로 Boot 4.1·Java 21로 올렸다 (PR #12, `docs/결정로그.md` 11-18). AI가 Boot 3 시절 코드(`spring-boot-starter-web`, `com.fasterxml.jackson.databind`, `@MockBean` 등)를 제안하면 Boot 4 방식으로 바꿔 쓴다.

## 패키지 구조

루트 패키지는 `com.vibenote`이다. **도메인별로 나누고**, 도메인 안에서 레이어를 나눈다.

```
com.vibenote
├─ auth            로그인, JWT, Refresh 토큰 (F-02)
├─ user            회원가입, 프로필, 통계 (F-01, F-03)
├─ follow          팔로우 (F-04)
├─ feed            활동 피드, activities 기록 (F-04, F-19)
├─ analysis        분석 요청, 파이프라인, 파일, 실행 흐름 (F-05~F-07, F-19)
├─ concept         개념 카드, 리뷰, 찜, 관리자. 공통 기반(A) 위에 F-08(A)·F-09~13(B)
├─ quiz            퀴즈 생성, 풀이, 채점 (F-14)
├─ reviewplan      복습 계획 (F-15)
├─ recommendation  맞춤 추천 (F-16)
├─ post            게시글(게시판 3종), 댓글 (F-17, F-18)
├─ challenge       학습 챌린지 관리, 참여·달성도 집계 (F-24, F-25)
├─ team            팀·초대 코드, 팀 분석 공유·이해도, 팀 챌린지 (F-20a~c, T-1 팀 프로젝트. 11-26 합의로 B → A 영역)
├─ llm             LlmClient와 구현체(Mock, GMS, Ollama, Gemini), 단계 출력 스키마, 프롬프트 로더, 사용량 로그
└─ common          예외, GlobalExceptionHandler, 페이지, JsonTypeHandler, 보안 설정, 유틸
```

뼈대(F-00)에서 만든 공통 코드:

| 위치 | 내용 |
|---|---|
| `common.exception` | `ErrorCode`(코드·상태·메시지), `BusinessException`, `ErrorResponse`, `GlobalExceptionHandler` |
| `common.security` | `JwtTokenProvider`(Access 발급·검증), `JwtAuthenticationFilter`, `AuthUser`(`@AuthenticationPrincipal`), 401·403 JSON 응답 |
| `common.config` | `SecurityConfig`(`PUBLIC_PATHS`, `/api/admin/**`은 ADMIN), `OpenApiConfig`(`bearerAuth`) |
| `common.web` | `JsonBodyLimitFilter`(JSON 요청 본문 바이트 상한, 넘으면 413 `COMMON_413`, #73) |
| `common.page` | `PageQuery`(page·size 기본값·검증, `offset()`), `PageResponse` |
| `common.mybatis` | `JsonTypeHandler` |
| `common.Level` | BEGINNER · JUNIOR · SENIOR |
| `llm` | `LlmClient`, `LlmRequest`·`LlmResponse`·`LlmStage`, `MockLlmClient` (PR #2). `LlmConfig`(provider별 빈 1개 조립), `Gms`·`Gemini`·`OllamaLlmClient`, `LlmSchemas`, `FallbackLlmClient`, `UsageLoggingLlmClient`·`LlmUsageLogMapper` (F-00 2.4) |

도메인 내부: `XxxController` → `XxxService` → `XxxMapper` (+ `dto/`).

## 레이어 규칙

- **Controller**: 요청 검증(`@Valid`), 인증 사용자 추출, Service 호출, 응답 반환만 한다. 비즈니스 로직을 두지 않는다.
- **Service**: 트랜잭션(`@Transactional`)과 비즈니스 로직. 다른 도메인의 Mapper를 직접 부르지 말고 그 도메인의 Service를 부른다.
- **Mapper**: MyBatis 인터페이스. SQL은 XML(`src/main/resources/mapper/<도메인>/XxxMapper.xml`)에 둔다. 아주 짧은 쿼리만 어노테이션을 허용한다.
- **DTO**: Java `record`로 쓰고 `XxxRequest` / `XxxResponse`로 이름 짓는다. DB 행을 담는 객체(`domain/Xxx.java`)를 Controller 응답으로 직접 내보내지 않는다.
- **예외**: `BusinessException(ErrorCode)`를 던지고 `GlobalExceptionHandler`가 `{code, message, details}`로 변환한다. `ErrorCode` enum에 코드와 HTTP 상태와 메시지를 함께 정의한다.
- **JSON**: Jackson 3(`tools.jackson.*`)를 쓴다. `com.fasterxml.jackson.databind`·`com.fasterxml.jackson.core`를 import하지 않는다. 어노테이션(`com.fasterxml.jackson.annotation.*`)은 Jackson 3에서도 이 패키지 그대로라 써도 된다. 예외는 unchecked `tools.jackson.core.JacksonException`이다.

## MyBatis

- `mybatis.configuration.map-underscore-to-camel-case=true`
- **SQL에 `${}`를 쓰지 않는다** (SQL 인젝션). 정렬 컬럼처럼 꼭 필요하면 화이트리스트 enum으로 검증한 값만 쓴다.
- JSON 컬럼(`analyses.overview`, `analysis_flows.steps`, `quiz_questions.choices`)은 `common.mybatis.JsonTypeHandler`로 매핑한다. 패키지 스캔으로 등록하지 않으므로 매핑마다 `javaType`과 `typeHandler`를 함께 적는다 (목록은 배열 타입을 JVM 이름으로, 예: `[Lcom.vibenote.analysis.flow.FlowStep;`. `FlowStep[]`은 MyBatis가 클래스를 못 찾는다). 사용 예는 클래스 주석에 있다.
  - JSON 컬럼에 담기는 record의 필드 중 null이 올 수 있는 것은 래퍼 타입(`Integer` 등)으로 선언한다. Jackson 3 기본값에서는 null을 primitive로 읽으면 예외가 난다.
- 목록 조회는 페이지(`LIMIT #{size} OFFSET #{offset}`)와 `COUNT(*)`를 함께 제공한다.
- **분석은 soft delete**다 (V2, F-19). 삭제 시 `analysis_files`·`analysis_flows`·`analysis_concepts`를 지우고 `analyses.deleted_at`을 채운다. 목록·상세·피드·게시글 첨부 조회는 `deleted_at IS NULL`로 거르고, 퀴즈 풀이·정답률·챌린지·일일 한도 집계는 거르지 않는다. 추천(F-16)은 삭제된 분석의 오답·등장을 신호에 넣지 않는다 (spec AC2). 프로필의 "분석 수"는 삭제한 분석을 뺀다 (F-03 AC3).
- **시각**: 서비스 코드는 `Clock` 빈(Asia/Seoul, `common.config.ClockConfig`)으로 `LocalDateTime.now(clock)`을 쓴다. DB의 `DEFAULT CURRENT_TIMESTAMP`·`NOW()`는 연결마다 세션 시간대를 `+09:00`으로 고정해 같은 기준이 된다 (`application.yml` `hikari.connection-init-sql`, #17). 테스트 컨테이너는 UTC로 떠서 이 고정을 검증한다 (`DbSessionTimeZoneTest`).
- 회원도 soft delete(`users.status=WITHDRAWN`)이므로 users 행을 DELETE하지 않는다. 작성물(posts·comments·concept_reviews)의 user FK는 RESTRICT라 실패한다.

## DB 마이그레이션 (Flyway)

- 위치: `src/main/resources/db/migration/`
- 스키마 변경: `V{n}__{설명}.sql` (예: `V3__add_quiz_tables.sql`). **커밋된 V 파일은 수정 금지**이다 (hook이 막는다).
- 시드 데이터: `R__seed_concepts.sql` (반복 실행 마이그레이션. 내용이 바뀌면 다시 적용된다). `INSERT ... ON DUPLICATE KEY UPDATE`로 작성한다 (row alias `AS seed`, MySQL 8.0.19+).
  - `name` 기준 upsert라 파일을 고치면 관리자가 화면에서 수정한 시드 카드 내용이 덮어써진다 (`status`는 검수 전 `DRAFT`일 때만 파일 값으로 바꿔 관리자가 숨긴 카드는 숨김 그대로, F-13 AC12). **카드 이름을 바꾸지 않는다** (새 카드가 생긴다). 공개 후 카드를 빼려면 DELETE 말고 `status='HIDDEN'`으로 둔다 (리뷰·찜 등이 CASCADE로 지워진다).
  - **이 파일은 생성본이다 (K-3a). 직접 고치지 않는다.** 원본은 카드별 Markdown `data/seed/concepts/<category>/<slug>.md`이고 `node scripts/seed-build.mjs`로 다시 만든다. 작은따옴표·백슬래시는 생성기가 이스케이프한다. CI가 `seed-build.mjs --check`로 원본과 같은지 확인한다.
  - 카드 검수: `node scripts/seed-check.mjs [--be] [--card <slug>]` (①형식 + ②예제 실행, 보고서 `.harness/seed-check/report.md`). java·sql·xml·yaml 예제는 `SeedExampleTest`가 javac·Testcontainers MySQL·XML·SnakeYAML로 검사하고 오류가 있으면 실패한다.
  - 출처(`reference_url`)는 `data/datasets.json` DS-01 도메인만 쓴다. 형식 검사와 `SeedConceptsTest`가 검사한다.
- Flyway 플레이스홀더 치환은 꺼 두었다 (`spring.flyway.placeholder-replacement: false`). 시드 본문의 `${}` 예제가 그대로 들어가야 하기 때문이다. 마이그레이션에서 플레이스홀더를 쓰지 않는다.
- 두 사람이 병렬로 마이그레이션을 만들므로 새 V 파일을 만들기 전에 `git merge origin/develop` 후 `node scripts/next-migration.mjs`로 번호를 확인한다 (develop과 원격 feature 브랜치까지 본다). 그래도 상대가 같은 번호를 먼저 머지하면 CI `check-migrations`가 실패한다. 그때는 develop을 합친 뒤 develop에 없는 내 파일만 `git mv`로 다음 번호로 바꾼다 (내용은 그대로). 로컬 DB에 옛 번호로 적용된 기록이 남았으면 `flyway_schema_history`의 그 행을 지우거나 로컬 DB를 다시 만든다.
- 시드 카드 원본(`data/seed/`)은 K-3 기간 A 김동환이 관리한다 (11-26 합의). 생성본 `R__seed_concepts.sql`은 공통 파일이므로 카드를 바꾼 PR은 상대에게 알린다.

## API 문서 (Swagger, springdoc-openapi)

- 의존성: `org.springdoc:springdoc-openapi-starter-webmvc-ui` 3.x (Spring Boot 4 호환 버전)
- 주소: `/swagger-ui/index.html`, `/v3/api-docs`. Security에서 permitAll로 연다.
- `common.config.OpenApiConfig`: 제목 "VibeNote API", 버전, **JWT Bearer `securityScheme`**(이름 `bearerAuth`)와 전역 보안 요구. 공개 API는 `@SecurityRequirements()`로 해제한다.
- Controller: 클래스에 `@Tag(name = "개념 카드", description = ...)`, 메서드에 `@Operation(summary = ...)`, 에러마다 `@ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))`
- DTO record 컴포넌트: `@Schema(description = "닉네임", example = "vibecoder")`. Enum은 허용값이 드러나게 한다.
- **`OpenApiExportTest`** (`IntegrationTestBase` 상속): `/v3/api-docs`를 받아 키를 정렬한 JSON으로 `../docs/api/openapi.json`에 쓴다. API를 바꾸면 이 파일 변경을 같은 커밋에 포함한다. CI가 테스트 후 파일이 달라지면 실패시킨다.
- Tag 이름 목록은 `docs/conventions.md` 8절.

## 파일 업로드 (챌린지 이미지)

- 저장 경로는 설정값 `app.upload-dir`(기본 `./uploads`, gitignore됨). 정적 경로 `/uploads/**`로 제공한다.
- jpg·png·webp만 허용(확장자 + MIME + 매직 바이트 확인), 2MB 이하, 파일 이름은 UUID로 바꾼다. 경로 조작(`..`)을 막는다.

## LLM

- 모든 호출은 `llm.LlmClient` 인터페이스로 한다. 구현체는 `MockLlmClient`, `GmsLlmClient`(SSAFY GMS, OpenAI 호환), `OllamaLlmClient`, `GeminiLlmClient`이다.
- `llm.provider=mock|gms|ollama|gemini`. **기본값과 테스트는 `mock`**이다. 서비스는 `gms`: GMS가 실패(4xx·5xx·타임아웃·거부·잘림)하면 재시도 없이 Ollama(간이 모드)로 폴백하고, 분석 상세의 `liteMode`가 이를 알린다 (F-27). `gemini`는 평가 비교용으로 폴백하지 않는다.
- 출력 형식: `src/main/resources/llm-schemas/<stage>.json`(OpenAI strict 규칙)을 GMS `response_format.json_schema`·Ollama `format`으로 보낸다. 선택 필드는 null로 오므로 파서는 null과 생략을 같게 다룬다. 프롬프트의 "출력 형식"을 바꾸면 스키마도 함께 바꾼다 (`LlmSchemasTest`). 단계별 GMS 모델은 `llm.gms.stage-models`. `format`에 스키마 객체를 넘기므로 **Ollama 0.5 이상**(구조화 출력)이 필요하다 (그 전 버전은 400으로 거절해 폴백까지 `LLM_503`이 된다).
- 프롬프트: `src/main/resources/prompts/<stage>/<level>.md` (stage = overview, file, flow, concept, quiz, recommend / level = beginner, junior, senior)
- Mock 응답: `src/main/resources/llm-fixtures/<stage>.json`. 결정적이어야 하므로 랜덤을 쓰지 않는다.
- LLM 출력은 **JSON으로 받고 파싱 실패를 항상 처리**한다 (재시도 1회 → 실패 시 단계 FAILED).
- 모든 호출은 `llm_usage_logs`에 기록한다.
- **`LlmClient`는 트랜잭션 밖에서 부른다.** 사용량 기록이 호출자의 `@Transactional`에 참여하므로 롤백되면 기록이 사라지고, LLM 응답을 기다리는 동안(최대 180초) DB 커넥션을 잡는다. LLM 호출 → 결과 저장만 짧은 트랜잭션으로 나눈다.
- API 키는 환경변수 `GMS_API_KEY`(·평가용 `GEMINI_API_KEY`)로만 받는다. GMS는 팀 키의 크레딧 총량제다 (잔여: `GET https://gms.ssafy.io/gmsapi/key-info`).

## 분석 파이프라인

- `POST /api/analyses`는 202와 `analysisId`를 즉시 반환하고, `@Async` 작업이 `FETCHING → FILTERING → SELECTING → EXPLAINING → EXTRACTING → QUIZ → DONE/FAILED` 순으로 상태를 DB에 기록한다.
- 업로드 ZIP 처리 시 **Zip Slip 방어**(엔트리 경로 정규화 후 대상 폴더 밖이면 거부), 인덱스 상한(파일 2,000개·합계 30MB, 넘는 파일은 버리고 `dropped_files`에 센다, F-26a), `.env`와 키 패턴 제거를 반드시 한다.
- 원본 ZIP과 압축 해제 폴더는 `finally`에서 삭제한다.

## 테스트

- 통합 테스트는 `common.IntegrationTestBase`를 상속한다: `@SpringBootTest` + `@AutoConfigureMockMvc` + Testcontainers `mysql:8.4` 싱글톤 컨테이너 + Flyway 적용 + `llm.provider=mock` + 테스트용 JWT 키. `mockMvc`, `objectMapper`를 바로 쓸 수 있다.
- 인증이 필요한 요청은 `JwtTokenProvider.createAccessToken(userId, "USER")`로 만든 토큰을 `Authorization: Bearer`로 보낸다 (`SecurityConfigTest` 참고).
- 테스트 전용 컨트롤러는 **static이 아닌 내부 클래스**로 둔다. static 중첩 클래스나 최상위 클래스에 `@RestController`를 붙이면 통합 테스트 컨텍스트와 `openapi.json`에 섞인다 (`GlobalExceptionHandlerTest` 참고).
- Controller: MockMvc로 상태 코드, 응답 JSON, 에러 코드를 검증한다.
- Mapper: 실제 DB로 SQL을 검증한다. H2를 쓰지 않는다.
- Service 단위 테스트는 로직이 복잡할 때만 Mockito로 쓴다 (예: 추천 점수 계산).
- **테스트 메서드 이름에 spec의 AC 번호를 넣는다**: `AC1_파일이_200개를_넘으면_413을_반환한다()`. `@DisplayName`은 쓰지 않아도 된다.
- **실제 외부 서비스(GMS·Gemini·Ollama 등)를 부르는 테스트는 `@Tag("live")`를 단다.** `test`(CI·git hook·verify)는 이 태그를 제외하고, 사람만 `./gradlew liveTest`로 돌린다. 개발 PC의 `~/.vibenote/.env`에 키가 있으므로, 태그 없이 쓰면 red 확인만으로도 무료 한도를 쓴다 (11-18 실제 발생). HTTP 동작 검증은 `MockRestServiceServer`로 한다 (`GeminiLlmClientTest` 참고).
- 테스트끼리 데이터가 섞이지 않게 `@Transactional` 롤백 또는 `@Sql` 정리 스크립트를 쓴다.

## 명령어 (backend 폴더에서)

| 목적 | PowerShell (Git Bash는 `./gradlew`) |
|---|---|
| 테스트 | `.\gradlew.bat test` |
| 특정 테스트 | `.\gradlew.bat test --tests "*ConceptControllerTest"` |
| 포맷 | `.\gradlew.bat spotlessApply` |
| 실행 | `.\gradlew.bat bootRun --args="--spring.profiles.active=local"` |

루트에서 `node scripts/verify.mjs be`를 쓰면 포맷 검사와 테스트를 돌리고 실패만 요약해 준다.

- 설정: `application.yml`(공통, PC마다 저장소 밖 `~/.vibenote/.env`를 자동으로 읽음. 옮기는 동안은 루트 `.env`도 읽고 둘 다 있으면 `~/.vibenote`가 이긴다, #249), `application-local.yml`(개발용 JWT 키, debug 로그). 테스트는 `IntegrationTestBase`의 `properties`가 우선한다.
- Gradle 실행은 **`JAVA_HOME`(JDK 17 이상)** 을 쓰고, 컴파일·테스트는 **Java 21 toolchain**으로 한다. JDK 21이 없으면 첫 빌드 때 Gradle(foojay resolver)이 `~/.gradle/jdks`에 자동으로 받는다. PATH의 `java`가 8이어도 상관없다. 설정 방법은 `docs/환경설정.md`.
- STS에서는 Installed JREs에 **JDK 21**을 추가하고 프로젝트 JRE로 지정한다.
