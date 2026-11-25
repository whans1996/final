# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-25 21:25 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/F-27-gms-provider

## 현재 기능

**F-27 GMS 공급자** — spec `docs/specs/F-27-gms-provider.md` APPROVED, 브랜치 `feature/F-27-gms-provider` (push됨, BE 전체 통과)

## 진행

- ✅ AC7 스키마 6개·`LlmSchemas` / ✅ AC3·AC4·AC5 `GmsLlmClient` / ✅ AC1·AC2 `LlmConfig`(gms→ollama, gemini 단독) / ✅ AC6 Ollama `format`=스키마 / ✅ AC9 `llm_usage_logs.provider`
- ✅ `application.yml`(llm.gms)·`.env.example`, `LlmLiveTest#gms_구조화_출력`(@Tag live, key-info 전후 출력) 추가 — 아직 실행 안 함
- ✅ spec 9절: GMS 문서(사용자 제공)로 base URL·크레딧 방식(`/gmsapi/key-info`) 확인. 남은 것: 기본 모델 ID 확정

## 바로 다음 할 일

1. AC8: `ConceptResponseParserTest`(conceptId·new·fileNo·lineHint null), `FlowResponseParserTest`(symbol null), `QuizResponseParserTest`(conceptId null)에 `AC8_…_null이면_생략과_같다` 1개씩
2. AC10: `AnalysisControllerTest#AC10_…` red → `LlmUsageLogMapper.existsSuccessfulByProvider(analysisId,"ollama")` + 상세 응답 DTO `liteMode`(Swagger) → openapi.json 갱신
3. AC12 consent 문구(`CreateAnalysisRequest`·에러 details) → AC13 `PromptEvalRunner` `LLM_PROVIDER` 선택 → FE AC11 배너·AC12 문구 → 문서(backend/CLAUDE.md LLM 절, 환경설정, prompt-evaluator, ERD, 화면 설계서, 산출물 08)

## 막힌 점 / 결정 필요

- 기본 모델 ID: 사람이 `cd backend && ./gradlew liveTest --tests "*LlmLiveTest.gms*"` 실행 → models·크레딧 소모 확인 후 결정(후보 gpt-4.1-mini 4cr, gpt-4o-mini 2cr, gpt-5-nano 1cr)
- 사람: 노션 팀원 초대, F-05b liveTest, 제출 zip 이름, dh-ssafy `.env`에 GMS 키

## 주의

- 공통 파일 변경: `application.yml`(llm.gms 블록) → PR "공통 코드 변경"에 적고 @JoonseobKim 멘션
- spec 6절 concept 스키마 예시(conceptName)는 Mock 표기라 실제는 프롬프트대로 conceptId·new → PR 전 spec 6절 정리
- node 스크립트로 Java 편집 시 백슬래시가 깨짐 → Edit 도구 사용 · 이 PC에 python 없음
