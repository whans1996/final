# F-27 GMS 공급자: OpenAI 호환 클라이언트와 구조화 출력

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-27 |
| 우선순위 | T0 (2차 백로그, 12/14 동결 대상) |
| 영역 오너 | A 김동환 (AI·분석·학습 루프) |
| 관련 필수 요구사항 | ⑬ AI 활용 (주 공급자 교체, 응답 형식 보장) |
| 의존 spec | F-00 LLM 클라이언트 (DONE, `LlmClient`·폴백·사용량 기록을 이 spec이 넓힌다), F-05a·F-05b 분석 요청 (DONE, 동의 문구), F-06~F-08·F-14·F-16 (DONE, 단계별 출력 형식). F-27b(단계별 모델 배정)·F-28(사용량 한도)·V-2(12/04 기준선 GMS v1)가 이 spec 위에 선다 |
| 관련 이슈 | 없음 |
| 승인 | whans1996 2026-11-25 |

## 1. 목적

11-25 결정으로 주 AI 공급자가 Gemini 무료 등급에서 SSAFY **GMS**(OpenAI 호환 게이트웨이)로 바뀌었다. 지금 체인은 Gemini → Ollama이고, 응답 형식은 "JSON으로 답하라"는 프롬프트와 `responseMimeType`에만 기대어 형식 오류(`LLM_502` 재시도)가 남는다. 사용자는 결과가 로컬 소형 모델로 만들어졌는지 알 수 없고, 동의 문구는 여전히 Gemini를 고지한다.

F-27은 (1) GMS 클라이언트를 더해 서비스 체인을 **GMS → Ollama**로 바꾸고, (2) 단계별 **JSON 스키마**를 GMS(`response_format: json_schema, strict`)와 Ollama(`format`)에 넘겨 형식을 강제하며, (3) 단계별로 모델을 바꿀 수 있는 설정을 만들고, (4) Ollama로 만든 결과를 **간이 모드**로 분석 결과 화면에 표시하고, (5) 동의 문구를 GMS 기준으로 고친다. Gemini는 서비스 체인에서 빠지고 평가 비교용 단독 공급자로만 남는다.

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC3_GMS_요청은_단계_스키마를_strict로_보낸다()`
> HTTP 동작은 `MockRestServiceServer`로 검증한다. 실제 GMS·Ollama를 부르는 테스트는 `@Tag("live")`만 쓴다 (backend/CLAUDE.md). 스키마 검증 fixture는 기존 mock fixture(dev 데이터)만 쓴다 (절대 규칙 11).

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | `llm.provider`가 각 값일 때 | `LlmConfig`가 빈을 조립하면 | `gms` → `Fallback(Logging(Gms), Logging(Ollama))`, `ollama` → `Fallback(Logging(Ollama))`, `gemini` → `Fallback(Logging(Gemini))` (**폴백 없음**, 평가 비교용), `mock` → `Logging(Mock)`. 그 밖의 값은 기동 실패하고 메시지에 `mock, gms, ollama, gemini`를 적는다 |
| AC2 | `provider=gms`인데 `GMS_API_KEY`가 비어 있을 때 | 기동하면 | `IllegalStateException`으로 실패하고 메시지에 키 이름과 해결 방법만 있다. `LlmProperties.Gms#toString()`은 키를 `***`로 가린다 |
| AC3 | 단계 `OVERVIEW` 요청을 | `GmsLlmClient.complete()`가 보내면 | `POST {base-url}/chat/completions`, 헤더 `Authorization: Bearer <키>`, 본문 `model`(4번 규칙), `messages=[{role:user, content:<프롬프트>}]`, `response_format={type:json_schema, json_schema:{name:"overview", strict:true, schema:<overview 스키마>}}`. 응답 `choices[0].message.content`·`usage.prompt_tokens`·`usage.completion_tokens`·`model`이 `LlmResponse`가 된다 (응답 `model`이 없으면 설정 모델) |
| AC4 | `llm.gms.model=A`, `llm.gms.stage-models.quiz=B`일 때 | QUIZ와 FILE 요청을 보내면 | QUIZ는 `model=B`, FILE은 `model=A`로 보내고 사용량 기록의 `model`도 각각 실제 모델이다. `stage-models`에 `LlmStage`에 없는 키(예: `summary`)가 있으면 기동 실패한다 |
| AC5 | GMS가 401·429·5xx·타임아웃, `choices` 없음, `message.refusal` 있음, `finish_reason=length` 중 하나로 응답할 때 | `provider=gms` 체인이 호출하면 | GMS는 `LlmUnavailableException`(메시지에 키·프롬프트 없음)을 던지고 재시도 없이 Ollama가 답한다. Ollama도 실패하면 `BusinessException(LLM_503)` |
| AC6 | 단계 `CONCEPT` 요청을 | `OllamaLlmClient`가 보내면 | 본문 `format`이 `"json"` 문자열이 아니라 concept 스키마 객체다. 나머지(`stream:false`, `options.num_ctx`)는 그대로 |
| AC7 | `llm-schemas/<stage>.json`이 | `LlmStage` 6개 모두에 대해 로드되면 | 파일이 모두 있고, 모든 object 노드가 `additionalProperties:false`이며 `required`가 `properties`의 키 전부다 (OpenAI strict 규칙). `llm-fixtures/<stage>.json`(main·test 모두)이 해당 스키마에 맞는다 |
| AC8 | 스키마에서 선택이던 필드(예: concept `lineHint`)가 | 응답에 `null`로 오면 | 각 단계 파서가 필드가 없을 때와 같은 결과를 낸다 (strict 모드는 선택 필드를 `null`로 채우므로). 선택 필드가 있는 단계마다 테스트 1개 |
| AC9 | 공급자별 호출 시도가 | 성공·실패하면 | `llm_usage_logs.provider`에 `gms`·`ollama`·`gemini`·`mock` 중 실제 공급자를 남긴다 (V13 이전 행은 NULL) |
| AC10 | 분석 A는 성공한 Ollama 호출 기록이 1건 이상, 분석 B는 GMS 성공만 있고 Ollama는 실패 기록만 있을 때 | `GET /api/analyses/{id}`를 주인이나 (공개·완료면) 다른 회원이 부르면 | A는 `liteMode: true`, B·기록 없는 분석·mock 분석은 `false` |
| AC11 | `liteMode=true`인 분석을 | SC-07 분석 결과 화면에서 열면 | 탭 위에 `role="status"` 배너 "일부 결과는 간이 모드(서버의 로컬 AI)로 만들어 설명이 짧거나 덜 정확할 수 있어요."가 보인다. `false`면 배너가 없다 |
| AC12 | SC-05 분석 요청 화면과 `POST /api/analyses`가 | 동의 문구를 보여 주거나 `consent≠true`를 거부하면 | 고지는 GMS 기준(5절 문구)이고 Gemini라는 말이 없다. 거부 메시지 `CONSENT_REQUIRED`·Swagger `consent` 설명도 GMS 기준이다 |
| AC13 | `PromptEvalRunner`(`@Tag("prompt-eval")`)가 | `LLM_PROVIDER=gms|gemini|ollama`로 실행되면 | 그 공급자 하나만 폴백 없이 부른다 (V-2 기준선 GMS v1·Ollama v1을 같은 러너로 만든다). 키가 없으면 건너뛴다 |

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다. Swagger Tag: 기존 `분석`.

### `GET /api/analyses/{id}` (기존, 필드 추가)

- 권한: 기존과 같음 (주인, 또는 공개·완료 분석은 다른 회원)
- 응답 `200`: 기존 필드에 `liteMode` 추가

```json
{
  "id": 42,
  "title": "todo-app",
  "status": "COMPLETED",
  "liteMode": true
}
```

- `liteMode` (boolean, 필수): 이 분석에서 성공한 LLM 호출 중 Ollama(간이 모드)가 1건 이상이면 `true`. 처리 중에도 그 시점까지의 기록으로 계산한다
- 에러: 기존과 같음

### `POST /api/analyses` (기존, 문구만 변경)

- `consent` 설명: "SSAFY GMS를 거친 외부 AI 전송 고지 동의. true여야 한다"
- `400 COMMON_400` `details.consent`: "주요 파일이 SSAFY GMS를 거쳐 외부 AI로 전송되는 데 동의해야 분석할 수 있습니다."

## 4. DB 변경

- Flyway: `V13__llm_usage_logs_provider.sql`
  - `ALTER TABLE llm_usage_logs ADD COLUMN provider VARCHAR(20) NULL COMMENT 'gms|ollama|gemini|mock. V13 이전 행은 NULL' AFTER stage`
  - `ADD KEY idx_llm_usage_logs_analysis (analysis_id)` (`liteMode` 조회용)
- 사용 테이블: `llm_usage_logs` (기록·조회), `analyses` (기존 상세 조회)
- 산출물: ERD(`llm_usage_logs.provider`, 인덱스) 같은 PR에서 고친다

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | `/analyses/:id` → `AnalysisDetailView.vue` (SC-07), `/analyses/new` → `AnalysisRequestView.vue` (SC-05) |
| 컴포넌트 | SC-07 간이 모드 배너 (뷰 안의 마크업, 새 컴포넌트 없음) |
| store | 없음 |
| API 함수 | `src/api/analyses.js`의 `fetchAnalysis()` (응답에 `liteMode`가 더해질 뿐 함수 변경 없음) |
| 상태별 UI | `liteMode=true`면 배너, 아니면 없음. 처리 중 폴링으로 상세를 다시 읽을 때 갱신된다 |

SC-05 고지 문구 (AC12):

> 분석을 위해 주요 파일이 SSAFY GMS(교육용 AI 게이트웨이)를 거쳐 외부 AI 모델로 전송됩니다. GMS를 쓸 수 없을 때는 서버의 로컬 AI로 간이 분석하며, 이때는 외부로 전송되지 않습니다. 비공개·민감한 코드는 올리지 마세요. `.env`와 키 패턴은 전송 전에 지웁니다.

산출물: 화면 설계서 SC-05("Gemini 전송 고지" → "GMS 전송 고지")·SC-07(간이 모드 배너) 같은 PR에서 고친다.

## 6. LLM (AI 기능만)

| 항목 | 내용 |
|---|---|
| 파이프라인 단계 | 6단계 전부 (`OVERVIEW`·`FILE`·`FLOW`·`CONCEPT`·`QUIZ`·`RECOMMEND`). 호출 순서·재시도 정책은 그대로 |
| 프롬프트 파일 | 바꾸지 않는다 (수준 기준표·예시는 F-27b) |
| 입력 | 기존과 같음 |
| 출력 JSON 스키마 | **새 파일** `backend/src/main/resources/llm-schemas/<stage>.json` 6개. 기존 mock fixture의 모양을 그대로 옮기고 선택 필드는 `["<type>","null"]`로 둔다. `LlmSchemas`가 기동 때 한 번 읽어 GMS·Ollama 클라이언트에 넘긴다 |
| Mock fixture | 바꾸지 않는다. AC7이 스키마와 어긋나지 않는지 검사한다 |
| 캐시·재사용 | 기존과 같음 |

설정 (`application.yml`, `.env.example`):

```yaml
llm:
  provider: ${LLM_PROVIDER:mock}     # mock | gms | ollama | gemini(평가 비교용, 폴백 없음)
  gms:
    api-key: ${GMS_API_KEY:}
    base-url: ${GMS_BASE_URL:https://gms.ssafy.io/gmsapi/api.openai.com/v1}
    model: ${GMS_MODEL:gpt-4.1-mini}  # 구현 중 실연결로 확정 (9절)
    stage-models: {}                  # 예: quiz: gpt-4.1 (F-27b가 eval로 채운다)
    timeout: 60s
```

GMS 요청 본문 예시:

```json
{
  "model": "gpt-4.1-mini",
  "messages": [{ "role": "user", "content": "<채운 프롬프트>" }],
  "response_format": {
    "type": "json_schema",
    "json_schema": { "name": "concept", "strict": true, "schema": { "type": "object", "additionalProperties": false, "required": ["concepts"], "properties": { "concepts": { "type": "array", "items": { "type": "object", "additionalProperties": false, "required": ["conceptName", "fileNo", "lineHint", "contextNote"], "properties": { "conceptName": { "type": "string" }, "fileNo": { "type": "integer" }, "lineHint": { "type": ["string", "null"] }, "contextNote": { "type": "string" } } } } } }
  }
}
```

`temperature`·`max_tokens`는 보내지 않는다 (gpt-5 계열은 기본값 외 temperature와 `max_tokens`를 거부한다).

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1·AC2 | `LlmConfigTest#AC1_…`, `LlmPropertiesTest#AC2_…` | 단위 |
| AC3·AC5 | `GmsLlmClientTest` (MockRestServiceServer), `FallbackLlmClientTest#AC5_…` | 단위 |
| AC4 | `GmsLlmClientTest#AC4_…`, `LlmConfigTest#AC4_없는_단계_키는_기동_실패` | 단위 |
| AC6 | `OllamaLlmClientTest#AC6_…` | 단위 |
| AC7 | `LlmSchemasTest` (strict 규칙 + fixture 적합. 쓰는 키워드만 다루는 작은 검사기를 테스트 코드에 둔다, 의존성 추가 없음) | 단위 |
| AC8 | `ConceptExtractorTest`·`OverviewExplainerTest` 등 선택 필드가 있는 단계 파서 테스트 | 단위 |
| AC9 | `UsageLoggingLlmClientTest#AC9_…`, `LlmUsageLogMapperTest#AC9_…` | 단위·MyBatis(Testcontainers) |
| AC10 | `AnalysisControllerTest#AC10_…` (usage log 행을 직접 넣고 상세 조회) | MockMvc 통합 |
| AC11 | `AnalysisDetailView.spec.js` | Vitest |
| AC12 | `AnalysisRequestView.spec.js`, `CreateAnalysisRequestTest`·`AnalysisControllerTest` 기존 consent 테스트 문구 갱신 | Vitest·단위 |
| AC13 | `PromptEvalRunner` 수동 실행 (`./gradlew promptEval -Pstage=… -Pfixture=…`, CI 제외). 공급자 선택 로직은 단위 테스트 1개 | 단위 + 수동 |
| 실연결 | `LlmLiveTest#gms_구조화_출력` (`@Tag("live")`) — 구현 중 사람이 허용한 최소 호출 (9절) | live, 수동 |

## 8. 범위 밖

- 단계별 모델 **배정 값**과 수준 기준표·2차 점검 호출 (F-27b, eval로 결정. 이 spec은 설정 구조만)
- 기능별 일일 한도·남은 횟수·진행 단계 표시 (F-28)
- 퀴즈·추천 이유 화면의 간이 모드 표시, 분석 목록(SC-06) 배지 (사용자 결정: 분석 결과 화면 배너만)
- 스트리밍 응답, Gemini·Claude 등 GMS의 다른 프록시 경로
- 프롬프트 내용 변경, GMS 실패 시 GMS 재시도 (재시도 없이 Ollama로 넘어가는 F-00 정책 유지)
- 배포 환경 설정 (O-1. 서버는 GMS만)

## 9. 결정 필요

- **기본 모델(`GMS_MODEL`)**: 구현 중 최소 실호출(모델 목록 1회 + 구조화 출력 1~2회, 사용자 허용 11-25)로 API 모델 ID를 확인해 확정한다. 후보 `gpt-4.1-mini`(4 크레딧, 구조화 출력 지원). 더 싼 후보: `gpt-4o-mini`(2), `gpt-5-nano`·`gpt-6-luna`(1). 표시 이름("gpt 6 luna")과 API ID가 다를 수 있다. 확정 값은 이 절에서 지우고 6절·결정로그에 적는다
- ~~base URL~~ GMS 안내 문서(11-25 사용자 제공)로 확인: `https://gms.ssafy.io/gmsapi/` + 원래 엔드포인트. OpenAI는 `…/api.openai.com/v1`(Chat Completions·Responses 모두), 인증은 `Authorization: Bearer <GMS 키>`. 6절 기본값 그대로
- ~~키별 사용 한도~~ 문서로 확인: 일·월 한도가 아니라 **팀 키 하나의 크레딧 총량과 만료일**이다. 모델마다 호출 비용(크레딧)이 다르다. 잔여량은 `GET https://gms.ssafy.io/gmsapi/key-info`(Bearer) → `{totalCredit, usedCredit, remainCredit, expiredDate}`. F-28 한도 설계에 넘긴다 (크레딧이 호출당인지 토큰당인지는 실호출 전후 `key-info`로 확인)

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/F-27-gms-provider`
- [x] Flyway `V13__llm_usage_logs_provider.sql`
- [x] AC별 실패 테스트 작성 (red 확인)
- [x] BE 구현 (green): `LlmSchemas`, `GmsLlmClient`, `LlmProperties.Gms`, `LlmConfig`, Ollama `format`, usage `provider`, 상세 `liteMode`, consent 문구, `PromptEvalRunner`
- [x] FE 구현 (green): SC-07 배너, SC-05 문구
- [x] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [ ] `.env.example`·`backend/CLAUDE.md`(LLM 절)·`docs/환경설정.md`·`.claude/agents/prompt-evaluator.md` 공급자 설명 갱신
- [ ] 실연결 확인 (`./gradlew liveTest`, GMS) → 9절 확정
- [ ] contract-checker 통과
- [ ] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일)
- [ ] reviewer 지적 사항 반영
- [ ] 산출물 반영: ERD(`llm_usage_logs.provider`), 화면 설계서 SC-05·SC-07, 산출물 08(AI 공급자)
- [ ] PR CI 초록
- [ ] 상태 DONE (PR 머지 후)
