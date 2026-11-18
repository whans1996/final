# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 14:40 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-13-seed-concepts

## 현재 기능

WBS 2.5 시드 개념 카드 (DS-05). 카드 목록 46개는 사람이 계획 승인으로 승인함 (카테고리 소문자 6종 java·spring·web·security·mybatis·vue)

## 진행

- ✅ `R__seed_concepts.sql` 46개 작성 (row alias `AS seed ON DUPLICATE KEY UPDATE`), `application.yml`에 `flyway.placeholder-replacement: false` (본문의 `${}` 때문)
- ✅ `SeedConceptsTest` 4개 (개수 30~50, PUBLISHED·DS-01 도메인, 카테고리·난이도, 빈 칸 없음). 기존 테스트의 개념 이름은 `test-` 접두사로 바꿈
- ✅ datasets.json DS-05, `09-데이터셋.md` DS-05, WBS 2.5 🟡
- ✅ `verify.mjs be` green (BE 157개 통과, Flyway R__ 적용 확인). 커밋 완료, push 전

## 바로 다음 할 일

1. (완료) 커밋. push 전 reviewer 점검
2. `reviewer`로 출처 규칙(Oracle·MDN 글 각색 금지, 링크만)과 eval 누수 점검 → `/pr-review` → PR(develop). PR 본문에 "사람 표본 검수 필요(본문 AI 초안)" 명시
3. 그다음: F-05 분석 요청 `/spec`, F-09·F-10 `/spec` (category 필터 값 = 위 6종)

## 막힌 점 / 결정 필요

- (사람) 시드 카드 본문 표본 검수 (PUBLISHED로 바로 공개됨)
- (사람) 집 PC `liveTest` 확인, 제출 zip 이름용 지역·반·이름

## 주의

- R__ 파일을 고치면 관리자가 수정한 시드 카드가 덮어써진다. 카드 이름 변경 금지(새 카드가 생김). 본문에 백슬래시 금지
- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금)
- V1·V2 수정 금지. 실제 Gemini 호출은 사람 요청 시만 (절대 규칙 6)
