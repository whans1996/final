# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 15:20 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-00-llm-client

## 현재 기능

WBS 2.4 LlmClient 연결 — spec `docs/specs/F-00-llm-client.md` APPROVED. **AC1~AC9 전부 green, 커밋 완료 (PR 전)**

## 진행

- ✅ AC2·3 3b08d24, AC4·5 24beaab, AC6·7 82991c0, AC1·8 d0c0c87, AC9 48ac60e. BE 전체 테스트 통과
- ✅ 산출물: 08 §1.1 호출 흐름·설정, §3.5 사례 / WBS 2.4 실적 / 기획서 결정 로그(기본 모델, ollama도 Fallback으로 감쌈) / conventions LLM_503 / backend/CLAUDE.md `@Tag("live")` 규칙
- 결정: Gemini 기본 `gemini-3.5-flash-lite`(2.5-flash deprecated), Ollama `qwen2.5-coder:7b`. spec AC1 ollama 조립 = `Fallback(Logging(Ollama))`
- ⚠️ AC9 red 확인 중 `.env` 키로 실제 Gemini 1회 호출됨 (정상 응답: 1576ms, in 52/out 26). 이후 `test`에서 live 제외

## 바로 다음 할 일

1. 새 세션에서 `/pr-review` → PR(feature/F-00-llm-client → develop). API 변경 없음(openapi.json 그대로)
2. 그다음: 2.5 시드 개념 카드(`R__seed_concepts.sql`), W2 F-05 분석 요청(A), F-09·F-10 카드 목록·상세(B)

## 막힌 점 / 결정 필요

- (사람) 이 PC Ollama: 서버는 떠 있으나 `/api/generate`가 404 → 모델 없음. `ollama pull qwen2.5-coder:7b` 후 `cd backend && ./gradlew liveTest` 1회 → 결과를 08 §1.5·§2에 기록 (집 PC도 확인)
- (사람) 제출 zip 이름용 지역·반·이름

## 주의

- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금)
- 셸 명령·커밋 메시지에 `.env` 문자열이 있으면 guard-bash가 막는다 → 메시지는 파일(`git commit -F`)로
- V1·V2 수정 금지. 실제 Gemini 호출은 사람 요청 시만 (절대 규칙 6)
