# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 14:20 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-00-wrapup

## 현재 기능

F-00 LlmClient **DONE** (PR #8 머지 495785b). 머지 후 정리 브랜치 `feature/F-00-wrapup` → develop PR (문서만)

## 진행

- ✅ F-00 spec DONE, WBS 2.4 ✅, 간트 2.4 done + 실적 막대, 작업로그
- W1(~11/23) 남은 일: 2.5 시드 개념 카드(⬜), 1.4 유스케이스(🟡), 1.6 기능별 spec

## 바로 다음 할 일

1. wrapup PR CI 초록 → 머지 (문서만이라 Codex 생략)
2. **WBS 2.5 시드 개념 카드**: 새 브랜치 `feature/F-13-seed-concepts`. 먼저 카드 목록(이름·category·difficulty·reference_url) 30~50개 표를 사람에게 승인받고, `backend/src/main/resources/db/migration/R__seed_concepts.sql`에 `status='PUBLISHED'`, `source='SEED'`로 작성. 출처는 `data/datasets.json` DS-01(split `content`)만. Oracle Java·Swagger 문서는 링크만, MDN 글은 각색 금지. eval 저장소(DS-02-05~08)는 참고 금지. 끝나면 DS-05 status, `09-데이터셋.md` 갱신
3. 그다음: F-05 분석 요청 `/spec` (LLM 호출과 결과 저장 트랜잭션 분리 명시), F-09·F-10 카드 목록·상세 `/spec`

## 막힌 점 / 결정 필요

- (사람) 집 PC에서도 `ollama pull qwen2.5-coder:7b` 후 `cd backend && ./gradlew liveTest` 확인
- (사람) 제출 zip 이름용 지역·반·이름

## 주의

- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금)
- 셸 명령·커밋 메시지에 `.env` 문자열이 있으면 guard-bash가 막는다 → 메시지는 파일(`git commit -F`)로
- V1·V2 수정 금지. 실제 Gemini 호출은 사람 요청 시만 (절대 규칙 6)
