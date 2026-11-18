# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 17:30 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-13-seed-concepts

## 현재 기능

WBS 2.5 시드 개념 카드 (DS-05) 85개 → **PR #10 (develop) 리뷰 중**. https://github.com/whans1996/final/pull/10

## 진행

- ✅ `/pr-review`: verifier all green (BE 157 + FE 76). reviewer 반드시 수정 1건(CSRF 카드 시큐리티 6 SPA 예제 403) `a4cfad7`
- ✅ 권장 반영 `ae58203`(이벤트 카드 insert 반환값 모순, MDN "(링크만)" 메모 제거, `SeedConceptsTest.시드_카드가_80개_이상이고_파일의_행이_모두_들어갔다`, 임시 카드 source=AI), `a10d4be`(09·진행현황·08 §3.5)
- ✅ CI 초록 (Backend 검증 + API 명세 일치, FE는 변경 없어 skip) · Codex ✅ a10d4be (지적 없음)

## 바로 다음 할 일

1. (사람) PR #10: 영역 오너(팀원 B)가 카드 카테고리별 2~3개 표본 검수 후 GitHub에서 머지 (wip 없음)
2. 머지 후 `/progress`: 간트 2.5 실적, 진행현황 갱신
3. 그다음: F-05 분석 요청 `/spec`, F-09·F-10 `/spec` (category 필터 값 = 6종)

## 막힌 점 / 결정 필요

- F-13 spec 결정: 관리자가 HIDDEN으로 내린 시드 카드를 R__ 재실행이 PUBLISHED로 되살림 (`status = seed.status`). 보존하려면 `IF(concepts.status='HIDDEN' ...)`
- F-10 spec: 카드 body(Markdown)는 sanitizer 거쳐 렌더링, `example_code`는 텍스트로만 (XSS 예시 문자열 포함)
- (사람) 집 PC: `git pull` → `node scripts/setup.mjs` → `ollama pull qwen2.5-coder:7b` → `./gradlew liveTest` → bootRun으로 시드 확인 / 제출 zip 이름용 지역·반·이름

## 주의

- R__ 파일을 고치면 관리자가 수정한 시드 카드가 덮어써진다. 카드 이름 변경 금지. 본문 백슬래시 금지, 작은따옴표는 두 번. 공개 후 제거는 HIDDEN
- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금). V1·V2 수정 금지
