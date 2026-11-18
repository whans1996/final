# 외부 API 및 생성형 AI 활용 — 바이브노트

> 작성 상태: 🟡 3절(개발 과정 AI 활용 구조) 작성, 1·2절은 구현 후 작성 (WBS 5.5)
> 소재 수집: `docs/작업로그.md`의 `AI 활용` 줄, git log, `llm_usage_logs` 통계, 프롬프트 평가 결과(`.harness/prompt-eval/`)

이 프로젝트에서 생성형 AI는 두 곳에 쓰인다.
1. **서비스 기능**: 사용자의 코드를 수준에 맞게 설명하고 개념 카드·퀴즈·추천 이유를 만든다.
2. **개발 과정**: 2인 릴레이 팀이 Claude Code를 "하네스"로 구성해 AI가 같은 규칙으로 개발·검증하게 했다.

서비스 주제("AI가 쓴 코드를 이해하기")와 개발 방식("AI가 코드를 쓰되 사람이 이해·검증할 수 있게 통제하기")이 같은 문제의식을 공유한다.

---

## 1. 서비스 내 생성형 AI 활용

### 1.1 사용 모델과 구조

| 항목 | 내용 |
|---|---|
| 기본 모델 | Google Gemini `gemini-3.5-flash-lite` (무료 등급 안정 모델. `GEMINI_MODEL`로 교체) |
| 폴백 | Ollama `qwen2.5-coder:7b` (로컬 모델) — 장애·한도 초과 시 재시도 없이 즉시 전환 |
| 개발·테스트 | Mock 클라이언트 (결정적인 fixture 응답) |
| 추상화 | `LlmClient` 인터페이스 — 설정(`llm.provider`)만 바꿔 교체 |

호출 흐름 (provider=gemini, `backend/.../llm/LlmConfig.java`가 조립):

```
호출부 ─▶ FallbackLlmClient
            ├─ 1) UsageLoggingLlmClient ─▶ GeminiLlmClient ─▶ POST /v1beta/models/{model}:generateContent
            │      (4xx·5xx·타임아웃·연결 실패·응답 형식 오류 → LlmUnavailableException, llm_usage_logs 실패 1행)
            └─ 2) UsageLoggingLlmClient ─▶ OllamaLlmClient ─▶ POST /api/generate (format=json)
                   (성공·실패 모두 1행, 실패면 BusinessException(LLM_503))
```

| provider | 조립 |
|---|---|
| `mock` (기본) | `Logging(Mock)` |
| `ollama` | `Fallback(Logging(Ollama))` |
| `gemini` | `Fallback(Logging(Gemini), Logging(Ollama))` |

- JSON 출력 강제: Gemini `generationConfig.responseMimeType=application/json`, Ollama `format: "json"`
- 키 보호: Gemini 키는 `x-goog-api-key` 헤더로만 보내고(URL에 남지 않음), 실패 예외·로그·`error_message`에는 `"Gemini 429"`처럼 공급자와 상태만 남긴다
- 키가 비었는데 provider=gemini면 기동이 실패한다 (원인에 `GEMINI_API_KEY` 표시)

설정 예시 (`application.yml`, 값은 저장소 루트 `.env`의 환경변수로 바꾼다):

```yaml
llm:
  provider: ${LLM_PROVIDER:mock}
  gemini:
    api-key: ${GEMINI_API_KEY:}
    model: ${GEMINI_MODEL:gemini-3.5-flash-lite}
    timeout: 60s
  ollama:
    base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
    model: ${OLLAMA_MODEL:qwen2.5-coder:7b}
    timeout: 120s
```

### 1.2 기능별 활용

| 기능 | AI가 하는 일 | 입력 | 출력 형식 | 캐시·재사용 |
|---|---|---|---|---|
| F-06 개요·파일 설명 | 수준별 프로젝트 개요, 파일 역할 설명 | 선별된 주요 파일 | JSON | 분석 결과 저장 |
| F-07 실행 흐름 | 요청→응답 단계별 설명 | 선택한 API 관련 파일 | JSON(steps) | `analysis_flows` |
| F-08 개념 추출 | 코드에 쓰인 개념 추출, 없는 개념의 초안 카드 | 파일 설명 | JSON | 카드로 저장·재사용 |
| F-14 퀴즈 생성 | 내 코드 기반 객관식 문항·해설 | 개념 + 코드 조각 | JSON | `quiz_questions` |
| F-16 추천 이유 | 추천 카드별 한 줄 이유 | 점수 근거 | 텍스트 | `recommendation_reasons` |

### 1.3 프롬프트 설계

(구현 후: 수준 3종 × 단계별 템플릿 구조, JSON 스키마 강제, 재시도 정책, 비밀 정보 제거, 프롬프트 평가 결과와 개선 사례)

### 1.4 AI 알고리즘: 맞춤 개념 카드 추천 (F-16)

- 카드별 점수 = 퀴즈 오답 수 ×3 + 내 분석에 등장 ×2 + 찜한 카드와 같은 카테고리 ×1 + 수준 적합도(±1). 복습 완료 카드는 제외.
- 상위 N개 카드의 추천 이유만 LLM이 생성하고 캐시한다 (규칙 기반 점수 + 생성형 설명의 결합).

### 1.5 비용·한도 관리

연결 실측 (`cd backend && ./gradlew liveTest`, 2026-11-18 SSAFY 교육장 PC, 입력: 두 정수를 더하는 메서드 요약 요청)

| 공급자 | 모델 | 지연 | 입력 토큰 | 출력 토큰 | 응답 |
|---|---|---|---|---|---|
| Gemini | `gemini-3.5-flash-lite` | 1,572ms | 52 | 23 | `{"summary": "두 개의 숫자를 더해서 그 결과를 알려주는 메서드입니다."}` |
| Ollama (로컬) | `qwen2.5-coder:7b` | 7,237ms | 83 | 19 | `{"summary": "두 정수를 더하는 메서드입니다."}` |

- 두 공급자 모두 JSON 출력 강제가 지켜졌다. 로컬 폴백은 Gemini보다 약 4.6배 느리므로 폴백은 장애 대비용으로만 쓰고 기본은 Gemini로 둔다.
- 같은 프롬프트라도 토크나이저가 달라 입력 토큰 수가 다르다(52 vs 83). 사용량 통계는 공급자별로 나눠 본다.

(구현 후: 일일 분석 횟수 제한, 주요 파일 선별, 결과 캐시, `llm_usage_logs` 기반 사용량 통계 표)

---

## 2. 외부 API 활용

| API | 용도 | 방식 | 보안·제한 |
|---|---|---|---|
| GitHub 저장소 아카이브 | 공개 저장소 코드 수집 | `https://codeload.github.com/...` ZIP 다운로드 | 허용 호스트만(SSRF 방어), 크기·파일 수 제한 |
| GitLab 저장소 아카이브 | 공개 저장소 코드 수집 | `/-/archive/` ZIP 다운로드 | 동일 |
| Gemini API | 생성형 AI | REST(JSON), `POST /v1beta/models/{model}:generateContent` | 키는 환경변수·`x-goog-api-key` 헤더, 사용량 기록, 4xx·5xx·타임아웃 시 Ollama 폴백 |

- Gemini 연결은 `LlmLiveTest`(`@Tag("live")`, 일반 `test`·CI에서 제외)로 확인한다. 실측은 §1.5.
- 오류 처리: 4xx(키 오류 400·403, 모델 없음 404, 한도 429)·5xx·타임아웃·연결 실패·응답 형식 오류(안전 필터 차단 등) → `LlmUnavailableException` → 폴백, 모두 실패하면 `LLM_503` (§1.1). 키가 틀려도 조용히 Ollama로 넘어가므로 `llm_usage_logs`의 `error_message`(`Gemini 400` 등)로 확인한다

(구현 후: 저장소 아카이브 요청·응답 예시)

---

## 3. 개발 과정의 생성형 AI 활용 — Claude Code 하네스

### 3.1 배경

- 2인 팀이 **서로 다른 PC에서 릴레이**로 개발한다. AI 도구 토큰이 떨어지면 세션이 갑자기 끊기고 상대가 이어받는다.
- AI가 생성한 코드를 팀이 이해하지 못한 채 쌓이는 문제(= 우리 서비스가 푸는 문제)를 개발 과정에서도 막고 싶었다.
- 그래서 AI에게 일을 맡기되, **AI가 같은 규칙으로 일하고, 스스로 검증하고, 실수가 기계적으로 차단되고, 끊겨도 상태가 남도록** 개발 환경(하네스)을 설계했다.

### 3.2 하네스 구조 (5층)

| 층 | 목적 | 구성 |
|---|---|---|
| 1. 컨텍스트 | AI가 처음에 읽는 규칙과 지도 | 루트·backend·frontend `CLAUDE.md`, `docs/conventions.md`, 기능별 spec(`docs/specs/`), 현재 상태 `docs/handoff/NOW.md` |
| 2. 워크플로 스킬 | 반복 작업을 정해진 절차로 | `/pickup`(이어받기), `/spec`(기능 명세), `/implement`(TDD 구현), `/pr-review`(PR 전 리뷰), `/handoff`(인수인계), `/progress`(진척·산출물 점검) |
| 3. 가드레일 | 위험한 행동을 기계적으로 차단 | Claude Code hooks: main 직접 커밋·강제 push·`--no-verify`·`.env` 접근·커밋된 DB 마이그레이션 수정·API 키 작성 차단. git `commit-msg` hook: 커밋 규칙·테스트 게이트 |
| 4. 피드백 루프 | AI가 스스로 결과를 확인 | `verify.mjs`(테스트·빌드 실패만 요약), `ui-check.mjs`(실제 화면 점검), Testcontainers MySQL, LLM Mock, GitHub Actions CI, Swagger openapi.json 일치 검사 |
| 5. 서브에이전트 | 역할 분리와 토큰 절약 | verifier(검증, Haiku·low), reviewer(spec·보안 리뷰, Opus·high), contract-checker(FE·BE API 계약, Sonnet·medium), ui-verifier(화면, Sonnet·medium), prompt-evaluator(프롬프트 품질, Sonnet·medium) |

### 3.3 릴레이 체크포인트 설계

- 세션 시작 시 hook이 브랜치·git 상태·`NOW.md`·마지막 작업로그를 AI에게 자동 주입한다.
- AI가 코드를 바꾸고 `NOW.md`를 갱신하지 않은 채 턴을 끝내려 하면 Stop hook이 한 번 막는다.
- 토큰이 떨어져 AI가 멈추면 사람이 `node scripts/handoff.mjs` 한 줄로 작업로그 기록·wip 커밋·push를 한다.

### 3.3.1 토큰·컨텍스트 관리 (11-18 실측 후 도입)

AI는 턴마다 대화 전체를 다시 읽으므로 비용은 대화 길이에 비례한다. 세션 하나(이어받기 → F-03 spec → 구현 → 리뷰 반영 → 정리, 273턴)를 `scripts/token-report.mjs`로 집계했다.

| 주체 | 모델 | 평균 컨텍스트 | 최대 | 가중 비용 비중 |
|---|---|---|---|---|
| 메인 에이전트 | Opus | 264k | 463k | 90% |
| reviewer ×1 | Opus | 80k | 128k | 4% |
| verifier ×16 | Haiku | 14k | 23k | 4% |
| contract-checker·ui-verifier | Sonnet | 17~27k | 36k | 2% |

- **발견**: 서브에이전트 모델 분리는 잘 되어 있었지만, 비용의 대부분은 한 세션에 여러 단계를 몰아 하며 커진 메인 컨텍스트였다. 1M 컨텍스트 모델이라 자동 압축이 늦고, 상태줄의 %가 작게 보여 경고도 없었다.
- **조치**: 단계(spec 승인, PR 생성, 머지 후 정리)마다 `/clear`로 새 세션을 시작한다. NOW.md가 상태를 들고 있어 이어받기 비용이 작다. 모델·effort는 세션 시작 때 작업 종류로 고른다(모델을 중간에 바꾸면 캐시를 다시 쓰므로). 서브에이전트 effort를 역할별로 고정하고, Stop hook이 200k를 넘으면 `/clear`를 안내한다.
- **효과 측정**: 다음 기능부터 `token-report.mjs --list`로 세션별 평균 컨텍스트를 비교한다 (W5에 3.6절로 정리).

### 3.4 spec 주도 + TDD

- 기능마다 수용 기준(AC)을 테스트 가능한 문장으로 고정한 spec을 **사람이 승인**한 뒤에만 구현한다.
- AC별로 실패하는 테스트를 먼저 쓰고 통과시킨다. AI가 구현에 맞춰 테스트를 쓰는 것을 막기 위한 순서다.

### 3.5 실제 사례

(개발하며 작업로그 `AI 활용` 줄에서 모아 채운다. 예: 하네스 세팅 테스트 중 guard가 테스트 스크립트 안의 hook 우회 시도(`-c core.hooksPath=...`)까지 차단함)

| 날짜 | 상황 | AI/하네스가 한 일 | 결과 |
|---|---|---|---|
| 2026-11-17 | 하네스 E2E 테스트 | guard-bash가 hook 경로 우회 옵션을 차단 | 우회 없이 정상 경로로 테스트하도록 수정 |
| 2026-11-18 | LlmClient TDD 중 `@Tag("live")` 테스트를 먼저 쓰고 red 확인 | 일반 `test`에 섞여 개발 PC `.env`의 키로 실제 Gemini가 1회 호출됨(규칙 위반). Claude가 결과 XML에서 발견해 바로 보고 | `build.gradle`에서 `live` 태그를 제외하고 `liveTest` 태스크로 분리. 키를 비운 채 `liveTest`로 건너뛰기를 확인. "외부 호출 테스트는 태그 제외를 먼저" 규칙을 backend/CLAUDE.md에 추가 |

### 3.6 효과와 한계

(W5 작성: 커밋·PR 수, CI 실패로 막은 결함 수, 교대 횟수와 복원 시간, 한계 — 문자열 기반 가드의 우회 가능성, 무료 플랜의 브랜치 보호 부재 등)
