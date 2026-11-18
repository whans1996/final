# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 16:30 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-13-seed-concepts

## 현재 기능

WBS 2.5 시드 개념 카드 (DS-05) **85개 작성 완료** (java 16 · spring 22 · web 15 · security 8 · mybatis 10 · vue 14 / B 32 · J 42 · S 11). 사람 결정(11-18): 최신 문법·실무 위주, Java 21 정식 카드, 30~50 상한 해제 → 기획서 결정 로그에 기록

## 진행

- ✅ `R__seed_concepts.sql` 85개, 고유 URL 82개 모두 200, 백슬래시 0. 시드 테스트 green. 커밋 252b977 + wip 3개(aee4cc3·a020e8d·ebe99db·30ae9d4), 미push
- ✅ `SeedConceptsTest.시드_카드가_30개_이상_120개_이하다`, 기획서(F-13·W1·실행 순서·결정 로그), WBS 2.5, 09·datasets DS-05, backend/CLAUDE.md(시드 upsert·HIDDEN·백슬래시·플레이스홀더) 갱신 (미커밋)
- 🟡 `node scripts/verify.mjs be` 전체 실행 중

## 바로 다음 할 일

1. verify green → wip 커밋들을 `git reset --soft 252b977` 후 하나로: `feat(F-13): 시드 개념 카드 85개로 확장 (최신 문법·실무 위주)` (push 전이라 안전)
2. `/pr-review`: reviewer로 출처 규칙(Oracle·MDN·Swagger는 링크만, 각색 금지)과 eval 누수(DS-02-05~08) 점검 → PR(develop). PR 본문에 "사람 표본 검수: 카드 본문은 AI 초안" 명시
3. 그다음: F-05 분석 요청 `/spec`, F-09·F-10 `/spec` (category 필터 값 = 6종)

## 막힌 점 / 결정 필요

- (사람) 집 PC: `git pull` → `node scripts/setup.mjs` → `ollama pull qwen2.5-coder:7b` → `cd backend && ./gradlew liveTest` → bootRun으로 시드 적용 확인
- (사람) 제출 zip 이름용 지역·반·이름

## 주의

- R__ 파일을 고치면 관리자가 수정한 시드 카드가 덮어써진다. 카드 이름 변경 금지. 본문 백슬래시 금지, 작은따옴표는 두 번. 공개 후 카드 제거는 DELETE 말고 HIDDEN
- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금)
- V1·V2 수정 금지. 실제 Gemini 호출은 사람 요청 시만 (절대 규칙 6)
