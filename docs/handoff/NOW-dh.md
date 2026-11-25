# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-25 21:15 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/F-27-gms-provider

## 현재 기능

**F-27 GMS 공급자** — spec `docs/specs/F-27-gms-provider.md` APPROVED, 브랜치 `feature/F-27-gms-provider`. 이번 세션 목표 AC7 → AC3·AC4·AC5 → AC1·AC2 → AC6 → AC9

## 진행

- ✅ AC7 커밋 `9949340`: V13(`llm_usage_logs.provider` + `idx_llm_usage_logs_analysis`), `llm-schemas/*.json` 6개, `LlmSchemas.load()/of()`, `LlmSchemasTest`(strict 검사 + Mock 출력 적합)
- 스키마는 프롬프트 출력 형식 기준(concept은 `conceptId`·`new`, spec 6절 예시의 conceptName은 Mock 전용 표기라 쓰지 않음 → spec 정리 필요)
- 🔴 미커밋: `LlmProperties.Gms`(키 가림, stageModels null→빈 맵), `GmsLlmClient` 스텁, `GmsLlmClientTest` 13개 red 확인, `LlmPropertiesTest`(AC2) green

## 바로 다음 할 일

1. `GmsLlmClient` 구현: `POST /chat/completions`, Bearer 헤더, `response_format.json_schema{name=stage.key(), strict, schema}`, `modelFor(stage)`=stageModels 또는 model, refusal→"GMS 응답 거부", finish_reason=length→"GMS 응답 잘림", choices·content 없음→malformed → `GmsLlmClientTest` green → 커밋
2. `UsageLoggingLlmClient`에 provider·단계별 모델 함수 추가(AC9, `LlmUsageLog.provider`, 매퍼 XML) → `LlmConfig`(gms·ollama·gemini 단독·mock, stage-models 키 검증, 메시지 `mock, gms, ollama, gemini`) + `LlmConfigTest`
3. `OllamaLlmClient` `format`=스키마(AC6), `LiveLlmClients`·`LlmLiveTest`에 gms 추가

## 막힌 점 / 결정 필요

- spec 9절: GMS 기본 모델·base URL(실연결로 확정), 키별 사용 한도. 사용자에게 GMS 안내 문서 요청함
- 사람: 노션 팀원 초대, F-05b liveTest, 제출 zip 이름, dh-ssafy `.env`에 GMS 키

## 주의

- F-27은 A 영역 파일만 바꿀 예정. 단 consent 문구(AC12)는 `ErrorCode`/요청 DTO 위치 확인 필요
- 커밋 범위는 F-xx|common|harness만 · Git Bash에서 `/pr-review` 같은 인자는 MSYS 경로 변환됨 (`MSYS_NO_PATHCONV=1`)
- node 스크립트로 Java 편집 시 백슬래시가 깨짐 → Edit 도구 사용 · 이 PC에 python 없음
