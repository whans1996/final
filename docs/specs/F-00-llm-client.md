# F-00 LlmClient 연결 (Gemini·Ollama·Mock, WBS 2.4)

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-00 (인프라, WBS 2.4. 기능 목록에 없는 공통 기반) |
| 우선순위 | P0 (F-05·F-06·F-07·F-08·F-14·F-16의 전제) |
| 영역 오너 | 팀원 A (AI·분석) |
| 관련 필수 요구사항 | NF-04 LLM 출력 JSON, NF-06 장애·한도 시 Ollama 자동 전환, NF-07 모든 LLM 호출 토큰 기록 (`docs/기획서.md` 비기능 요구사항) |
| 의존 spec | 없음 (PR #2의 `LlmClient` 인터페이스·`MockLlmClient` 위에 쌓음) |
| 승인 | whans1996 2026-11-18 |

## 1. 목적

분석 파이프라인(F-05 이후)이 **설정(`llm.provider`)만 바꿔** Mock·Gemini·Ollama 중 하나로 LLM을 호출할 수 있게 한다.
Gemini 무료 등급이 한도 초과·장애로 실패하면 재시도 없이 즉시 로컬 Ollama로 넘어가고, **모든 실제 호출 시도**를 `llm_usage_logs`에 남겨 비용과 폴백 빈도를 볼 수 있게 한다.
실제 API 연결은 자동 테스트에서 하지 않고, 사람이 키를 넣고 `liveTest`로 한 번 확인한다 (CLAUDE.md 절대 규칙 6).

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다.

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | `llm.provider`가 없음·`mock`·`ollama`·`gemini` | 애플리케이션 컨텍스트가 뜨면 | `LlmClient` 빈이 **하나만** 있고 조립이 각각 `Logging(Mock)` · `Logging(Mock)` · `Fallback(Logging(Ollama))` · `Fallback(Logging(Gemini), Logging(Ollama))`이다 (ollama 단독도 실패를 `LLM_503`으로 바꾸려고 공급자 1개짜리 Fallback으로 감싼다, 11-18 구현 시 조정) |
| AC2 | provider=gemini, Gemini가 정상 응답 | `complete(req)` | `POST {gemini.base-url}/v1beta/models/{model}:generateContent`를 `x-goog-api-key` 헤더(쿼리 파라미터 아님)와 `generationConfig.responseMimeType=application/json`으로 호출하고, `candidates[0].content.parts[0].text` → `content`, `usageMetadata.promptTokenCount`·`candidatesTokenCount` → 토큰 수, 설정 모델명 → `model`로 반환한다 |
| AC3 | provider=ollama, Ollama가 정상 응답 | `complete(req)` | `POST {ollama.base-url}/api/generate`를 `{"model", "prompt", "format":"json", "stream":false}`로 호출하고 `response` → `content`, `prompt_eval_count`·`eval_count` → 토큰 수, 설정 모델명 → `model`로 반환한다 |
| AC4 | provider=gemini, Gemini가 429·500·타임아웃·연결 실패 중 하나 | `complete(req)` | Gemini를 **다시 부르지 않고** Ollama 응답을 반환하며 `model`은 Ollama 모델명이다 |
| AC5 | Gemini와 Ollama가 모두 실패 (provider=ollama면 Ollama 실패) | `complete(req)` | `BusinessException(LLM_503)`을 던지고, 예외 메시지·로그에 API 키와 프롬프트 본문이 없다 |
| AC6 | 어떤 provider든 | `complete(req)` 한 번 | **실제 호출 시도마다** `llm_usage_logs`에 1행이 쌓인다: stage(소문자 key), model, input/output tokens, latency_ms, success, error_message(실패 시만, 500자 이하, 키·프롬프트 없음), `req`의 userId·analysisId. AC4 상황이면 Gemini 실패 1행 + Ollama 성공 1행 |
| AC7 | `LlmUsageLogMapper.insert`가 예외를 냄 | `complete(req)` | 경고 로그만 남기고 LLM 응답은 정상 반환된다 (기록 실패가 분석을 막지 않음) |
| AC8 | provider=gemini인데 `GEMINI_API_KEY`가 비어 있음 | 애플리케이션 기동 | 기동이 실패하고 메시지에 `GEMINI_API_KEY`가 비었다는 원인이 나온다 |
| AC9 | `@Tag("live")` 테스트 | `./gradlew test`(CI·git hook·verify) / `./gradlew liveTest` | `test`에서는 제외되고, `liveTest`에서만 실행된다. 키나 Ollama 서버가 없으면 실패 대신 건너뛴다(assumption) |

## 3. API 계약

없음. HTTP 엔드포인트를 추가하지 않는다 (`docs/api/openapi.json` 변경 없음).

- 에러 코드 (내부용, 호출자가 파이프라인 단계를 FAILED로 표시할 때 사용)

| 상태 | 코드 | 언제 |
|---|---|---|
| 503 | LLM_503 | 설정된 LLM(폴백 포함)이 모두 응답하지 못함. 메시지: "AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요." |

`docs/conventions.md` 3절 에러 표의 `LLM` 행에 `LLM_503`을 적는다.

## 4. DB 변경

- Flyway: 없음
- 사용 테이블: `llm_usage_logs` (`V1__init.sql`, INSERT만). `user_id`는 `users` FK(ON DELETE SET NULL), `analysis_id`는 FK 없음

## 5. 프론트엔드

없음.

## 6. LLM (AI 기능만)

이 spec은 프롬프트가 아니라 **호출 계층**을 만든다. 프롬프트 템플릿과 단계별 출력 스키마는 F-06 이후 spec에서 정한다.

### 구성 (`backend/src/main/java/com/vibenote/llm/`)

| 클래스 | 역할 |
|---|---|
| `LlmClient` (기존) | `LlmResponse complete(LlmRequest)` 그대로 |
| `LlmRequest` (변경) | `(LlmStage stage, Level level, String prompt, Long userId, Long analysisId)`. 기존 3인자 생성자는 userId·analysisId를 null로 두는 편의 생성자로 남긴다 |
| `LlmProperties` (새) | `@ConfigurationProperties("llm")` record: `provider`, `gemini{apiKey, model, baseUrl, timeout}`, `ollama{baseUrl, model, timeout}`. `common/security/JwtProperties.java` 패턴 |
| `GeminiLlmClient` (새) | `RestClient`로 Gemini REST 호출 (AC2). 빈이 아니라 `LlmConfig`가 생성 |
| `OllamaLlmClient` (새) | `RestClient`로 Ollama REST 호출 (AC3). 빈이 아님 |
| `MockLlmClient` (변경) | 동작은 그대로(`llm-fixtures/<stage>.json`). `@Component`·`@ConditionalOnProperty`를 떼고 `LlmConfig`가 생성 |
| `LlmUnavailableException` (새) | 개별 공급자 실패(HTTP 4xx·5xx, 타임아웃, 연결 실패, 응답 형식 이상). 메시지는 `"<공급자> <상태 또는 예외 종류>"`만 |
| `FallbackLlmClient` (새) | 공급자 목록을 순서대로 시도. 앞 공급자가 `LlmUnavailableException`이면 재시도 없이 다음 공급자 호출. 모두 실패하면 `BusinessException(LLM_503)` |
| `UsageLoggingLlmClient` (새) | 데코레이터. 위임 호출의 지연·결과를 `LlmUsageLogMapper`로 1행 기록. 기록 예외는 삼키고 warn. 위임 예외는 기록 후 다시 던짐 |
| `LlmConfig` (새) | provider별로 `LlmClient` **빈 하나**만 조립 (AC1). provider=ollama 단독일 때 `Fallback(Ollama)`로 감싸 Ollama 실패도 `LLM_503`으로 바꿈 |
| `LlmUsageLogMapper` (새) | `insert(LlmUsageLog)` (MyBatis XML 또는 어노테이션, 기존 매퍼 관례를 따름) |

### 설정 (`application.yml`, `.env.example`)

```yaml
llm:
  provider: ${LLM_PROVIDER:mock}
  gemini:
    api-key: ${GEMINI_API_KEY:}
    model: ${GEMINI_MODEL:<결정 필요 1>}
    base-url: https://generativelanguage.googleapis.com
    timeout: 60s
  ollama:
    base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
    model: ${OLLAMA_MODEL:<결정 필요 1>}
    timeout: 120s
```

`.env.example`에 `GEMINI_MODEL`, `OLLAMA_BASE_URL`, `OLLAMA_MODEL`을 추가한다. 키는 환경변수로만 받는다.

| 항목 | 내용 |
|---|---|
| 파이프라인 단계 | 모든 단계(`LlmStage` 6종)에서 공통 사용 |
| 프롬프트 파일 | 이 spec에서 만들지 않음 |
| 입력 | 호출자가 채운 프롬프트 문자열(JSON으로 답하라는 지시 포함) |
| 출력 JSON 스키마 | 이 spec에서 검증하지 않음. `content`는 원문 문자열. 파싱·1회 재시도는 호출자 몫(NF-04) |
| Mock fixture | 기존 `llm-fixtures/<stage>.json` 그대로. 새 fixture 없음 |
| 캐시·재사용 | 없음 (각 기능 spec) |

## 7. 테스트 계획

HTTP는 `RestClient.Builder` + `MockRestServiceServer`(spring-test, 의존성 추가 없음)로 스텁한다. 실제 네트워크는 `liveTest`에서만 쓴다.

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1 | `LlmConfigTest#AC1_provider별로_LlmClient가_하나만_조립된다` | `ApplicationContextRunner` 단위 |
| AC2 | `GeminiLlmClientTest#AC2_...` | MockRestServiceServer 단위 |
| AC3 | `OllamaLlmClientTest#AC3_...` | MockRestServiceServer 단위 |
| AC4 | `FallbackLlmClientTest#AC4_Gemini_429_500_타임아웃이면_Ollama로_전환한다` (`@ParameterizedTest`), `GeminiLlmClientTest`에서 상태별 `LlmUnavailableException` 변환 | 단위 |
| AC5 | `FallbackLlmClientTest#AC5_둘_다_실패하면_LLM_503이고_키와_프롬프트가_없다` | 단위 |
| AC6 | `UsageLoggingLlmClientTest#AC6_...`(가짜 매퍼), `LlmUsageLogMapperTest#AC6_insert`(IntegrationTestBase) | 단위 + Testcontainers |
| AC7 | `UsageLoggingLlmClientTest#AC7_기록이_실패해도_응답을_반환한다` | 단위 |
| AC8 | `LlmConfigTest#AC8_gemini인데_키가_없으면_기동_실패` | `ApplicationContextRunner` |
| AC9 | `backend/build.gradle`: `test { useJUnitPlatform { excludeTags 'live' } }` + `liveTest` 태스크. `LlmLiveTest`(`@Tag("live")`, Gemini·Ollama 각 1회, `Assumptions`로 키·서버 없으면 skip). `./gradlew test` 결과에 LlmLiveTest가 없는지 확인 | Gradle 설정 |

`liveTest`는 사람이 키를 넣고 한 번 실행한다. 결과(모델, 지연, 토큰)를 `docs/산출물/08-외부API-생성형AI-활용.md` §1.5·§2 예시에 적는다.

## 8. 범위 밖

- 프롬프트 템플릿(`prompts/<stage>/<level>.md`), 단계별 출력 JSON 스키마·검증 → F-06 이후
- JSON 파싱 실패 시 1회 재시도, 단계 FAILED 처리 → F-05 파이프라인
- 사용자별 일일 분석 횟수 제한(`ANALYSIS_429`) → F-05
- 결과 캐시(카드, 흐름, 추천 이유) → 각 기능 spec
- 레이트리미터·백오프, Gemini 재시도, 스트리밍 응답
- 사용량 집계 화면·관리자 API

## 9. 결정 필요

1. **기본 모델명.** 구현할 때 공식 문서로 현재 무료 등급 모델을 확인해 `application.yml` 기본값으로 정한다. 후보는 Gemini `gemini-2.5-flash`, Ollama `qwen2.5-coder:7b`이다. 환경변수로 바꿀 수 있으므로 구현을 막지는 않는다.
2. **(사람) Ollama 구동 확인.** 두 PC(집, SSAFY 교육장)에서 Ollama를 설치·구동할 수 있는지 확인한다 (기획서 미결 사항). 안 되면 폴백 대상을 다시 논의한다.

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/F-00-llm-client`
- [ ] Flyway 마이그레이션 (없음)
- [ ] AC별 실패 테스트 작성 (red 확인) — AC2~7 완료
- [ ] BE 구현 (green)
- [ ] `application.yml`·`.env.example`·`build.gradle`(liveTest) 갱신
- [x] `ErrorCode.LLM_503` + `docs/conventions.md` 에러 표
- [ ] reviewer 지적 사항 반영
- [ ] 산출물 반영: `08-외부API-생성형AI-활용.md` §1.1 구성도, WBS 2.4 실적
- [ ] (사람) `./gradlew liveTest` 1회 → 08 산출물 §1.5·§2에 결과 기록
- [ ] PR CI 초록, Codex COMPLETE
- [ ] 상태 DONE (PR 머지 후)
