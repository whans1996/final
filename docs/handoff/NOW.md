# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 11:50 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-03-wrapup

## 현재 기능

F-03 머지 후 정리 PR (문서·하네스만, 코드 변경 없음). F-01~F-03 모두 DONE (PR #3·#4 머지)

## 진행

- ✅ F-03 spec DONE, 작업로그, 화면설계서 SC-02·03·18·19에 스크린샷(`docs/산출물/screens/`)
- ✅ WBS·간트 실적 반영: 계획 날짜는 기준선 유지 + 실적 열·간트 `실적` 섹션 (04-WBS "일정 관리 방식")
- ✅ 하네스: `/pickup` 머지 후 정리, `/progress` WBS·간트 동기화, `/pr-review` 스크린샷 직접 첨부(`scripts/pr-screens.mjs`)
- ✅ 첫 `docs/진행현황.md` (W1 앞섬, 재계획 불필요)

## 바로 다음 할 일

1. 이 정리 PR CI 초록 확인 → 영역 오너가 GitHub에서 머지 (`gh pr merge` 금지)
2. 다음 기능 (W1 남은 일, 둘 다 W2 기능의 전제): **2.5 시드 개념 카드 30~50개** (`backend/src/main/resources/db/migration/R__seed_concepts.sql`, 카드별 `reference_url`, `data/datasets.json` split이 eval인 데이터 금지) 또는 **2.4 LlmClient Gemini·Ollama** (`backend/.../llm/`, 실제 호출 테스트는 사람 요청 시만). `/spec`으로 시작
3. 그다음 W2: F-05 분석 요청(A), F-09·F-10 카드 목록·상세(B)

## 막힌 점 / 결정 필요

- 팀원 GitHub 아이디(리뷰어 지정), 제출 zip 이름용 지역·반·이름 (사람)

## 주의

- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금). bootRun은 TaskStop 후에도 java.exe가 남을 수 있어 8080 PID를 확인해 종료
- 셸(node -e, 따옴표 없는 heredoc)로 Markdown을 쓸 때 백틱이 명령 치환된다 → Edit 도구 또는 `<<'EOF'`
- V1·V2 수정 금지
