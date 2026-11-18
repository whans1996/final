# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 16:10 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-00-llm-client

## 현재 기능

WBS 2.4 LlmClient 연결 — spec `docs/specs/F-00-llm-client.md`. **PR #8 열림 (→ develop), Codex ✅ 571d0d8, CI 확인 후 머지 대기**

## 진행

- ✅ AC1~AC9 green. verify all 통과(BE 152, FE 76). liveTest 통과: Gemini 1572ms(52/23), Ollama 7237ms(83/19) → 08 §1.5·§2 기록 15f09f2
- ✅ reviewer: 반드시 수정 1(산출물 미커밋) 반영. 권장 3 반영 99b3bac — Gemini `toString` 키 가림(`LlmPropertiesTest`), "LlmClient는 `@Transactional` 밖에서 호출" 규칙(javadoc·backend/CLAUDE.md), 폴백 조건 문서를 4xx·5xx·타임아웃·연결 실패·형식 오류로 정정
- PR 설명: `.harness/pr-F-00.md`. Codex ✅ 571d0d8 (지적 없음). CI: 제목·영역 통과, BE 검증 실행 중

## 바로 다음 할 일

1. 새 세션 `/pickup` → `gh pr checks 8`로 BE 검증 초록 확인. 실패면 `gh run view --log-failed` 요약 후 수정
2. CI 초록 + Codex COMPLETE → 영역 오너(A)가 GitHub에서 머지 → `/progress`에서 spec DONE
3. 그다음: 2.5 시드 개념 카드(`R__seed_concepts.sql`), W2 F-05 분석 요청(A, LLM 호출과 결과 저장 트랜잭션 분리를 spec에 명시), F-09·F-10 카드 목록·상세(B)

## 막힌 점 / 결정 필요

- (사람) 집 PC에서도 `ollama pull qwen2.5-coder:7b` 후 `cd backend && ./gradlew liveTest` 확인
- (사람) 제출 zip 이름용 지역·반·이름

## 주의

- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금)
- 셸 명령·커밋 메시지에 `.env` 문자열이 있으면 guard-bash가 막는다 → 메시지는 파일(`git commit -F`)로
- V1·V2 수정 금지. 실제 Gemini 호출은 사람 요청 시만 (절대 규칙 6)
