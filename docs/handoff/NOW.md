# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 14:00 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-00-llm-client

## 현재 기능

WBS 2.4 LlmClient 연결 (Gemini·Ollama·Mock) — spec `docs/specs/F-00-llm-client.md` **APPROVED (whans1996 11-18)**. 하네스 PR #6·#7 머지 완료, F-01~F-03 DONE

## 진행

- ✅ bd25ba9 리뷰 정책: 사람 리뷰 없이 reviewer 에이전트 + Codex 봇. CI 초록 + Codex COMPLETE면 영역 오너가 머지 (pr-review 스킬, PR 템플릿, conventions 7절, 기획서 역할·결정 로그). 팀원은 GitHub에 초대 완료
- ✅ 2.4 spec 초안: AC1~AC9. 사용자 결정: 사용량 기록 포함(`UsageLoggingLlmClient`), Ollama·폴백 함께, Gemini 실패는 재시도 없이 즉시 Ollama, 실제 연결은 `@Tag("live")` + `./gradlew liveTest`(사람 수동). 결정 로그 추가

## 바로 다음 할 일

1. 새 세션(Opus/high)에서 `/implement F-00-llm-client` — spec 승인 커밋 완료
2. 구현 순서(spec 6·7절): `LlmProperties` → `GeminiLlmClientTest#AC2`·`OllamaLlmClientTest#AC3` red (MockRestServiceServer) → 클라이언트 구현 → `FallbackLlmClientTest` AC4·5 → `UsageLoggingLlmClientTest` AC6·7 + `LlmUsageLogMapperTest` → `LlmConfigTest` AC1·8 (`ApplicationContextRunner`) → `build.gradle` liveTest(AC9). 기본 모델명은 구현 시 공식 문서로 확인 (spec 9절)
3. 그다음: 2.5 시드 개념 카드(`R__seed_concepts.sql`), W2 F-05 분석 요청(A), F-09·F-10 카드 목록·상세(B)

## 막힌 점 / 결정 필요

- (사람) 두 PC에서 Ollama 구동 가능 여부 확인, 키를 넣고 `liveTest` 1회 실행 (2.4 PR 전후)
- (사람) 제출 zip 이름용 지역·반·이름

## 주의

- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금). bootRun은 TaskStop 후에도 java.exe가 남을 수 있어 8080 PID를 확인해 종료
- 셸(node -e, 따옴표 없는 heredoc)로 Markdown을 쓸 때 백틱이 명령 치환된다 → Edit 도구 또는 `<<'EOF'`
- V1·V2 수정 금지. 실제 Gemini 호출은 사람 요청 시만 (절대 규칙 6)
