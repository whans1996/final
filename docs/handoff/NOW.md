# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 14:40 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-00-llm-client

## 현재 기능

WBS 2.4 LlmClient 연결 — spec `docs/specs/F-00-llm-client.md` APPROVED. `/implement` 진행 중. 이번 세션 목표: AC1~AC9 전부

## 진행

- 🔴 AC2·AC3: `GeminiLlmClientTest`·`OllamaLlmClientTest` 작성(MockRestServiceServer), `LlmProperties`·`LlmUnavailableException` 추가, 클라이언트는 빈 시그니처
- 결정: Gemini 기본 모델 `gemini-3.5-flash-lite` (공식 문서 11-18 확인: `gemini-2.5-flash`는 deprecated, 12/1부터 3.5-flash-lite로 자동 전환. 3.8-flash는 무료 한도가 더 적다는 보고). Ollama `qwen2.5-coder:7b`

## 바로 다음 할 일

1. `GeminiLlmClient`·`OllamaLlmClient` 구현(green) → 커밋 `feat(F-00): AC2·AC3 ...`
2. `FallbackLlmClient`(AC4·5) → `UsageLoggingLlmClient`·`LlmUsageLogMapper`(AC6·7) → `LlmConfig`(AC1·8) → `build.gradle` liveTest(AC9)

## 막힌 점 / 결정 필요

- (사람) 두 PC에서 Ollama 구동 가능 여부 확인, 키를 넣고 `liveTest` 1회 실행 (2.4 PR 전후)
- (사람) 제출 zip 이름용 지역·반·이름

## 주의

- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금)
- 셸로 Markdown을 쓸 때 백틱이 명령 치환된다 → Edit 도구 또는 `<<'EOF'`
- V1·V2 수정 금지. 실제 Gemini 호출은 사람 요청 시만 (절대 규칙 6)
