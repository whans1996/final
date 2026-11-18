# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-13-wrapup

## 현재 기능

WBS 2.5 시드 개념 카드 85개 — **PR #10 develop 머지 완료 (11-18)**. 지금은 머지 후 정리(문서만) 브랜치.

## 진행

- ✅ PR #10 머지 (554dc21). CI 초록, Codex 지적 없음
- ✅ 머지 후 정리: `04-WBS.md` 2.5 ✅·실적, `05-간트차트.md` t25 done + 실적 a25, `진행현황.md` 2.5 문구, 작업로그

## 바로 다음 할 일

1. `feature/F-13-wrapup` 정리 PR → develop (문서만, CI 초록 후 머지)
2. 새 세션(Opus/high)에서 F-05 분석 요청 `/spec` (A 영역). 이어서 F-09·F-10 `/spec` (B, category 필터 값 = 6종)
3. 주간 `/progress`는 11/23(일): 진행현황 W1 문구(2.4 LlmClient 완료 반영 등) 전체 갱신

## 막힌 점 / 결정 필요

- F-13 spec 결정: 관리자가 HIDDEN으로 내린 시드 카드를 R__ 재실행이 PUBLISHED로 되살림 (`status = seed.status`). 보존하려면 `IF(concepts.status='HIDDEN' ...)`
- F-10 spec: 카드 body(Markdown)는 sanitizer 거쳐 렌더링, `example_code`는 텍스트로만 (XSS 예시 문자열 포함)
- (사람) 집 PC: `git pull` → `node scripts/setup.mjs` → `ollama pull qwen2.5-coder:7b` → `./gradlew liveTest` → bootRun으로 시드 확인 / 제출 zip 이름용 지역·반·이름

## 주의

- R__ 파일을 고치면 관리자가 수정한 시드 카드가 덮어써진다. 카드 이름 변경 금지. 본문 백슬래시 금지, 작은따옴표는 두 번. 공개 후 제거는 HIDDEN
- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금). V1·V2 수정 금지
