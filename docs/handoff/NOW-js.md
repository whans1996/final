# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-19 17:15 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-10-concept-detail

## 현재 기능

F-10 개념 카드 상세 (`docs/specs/F-10-concept-detail.md` APPROVED). 이번 세션 목표: BE AC1~AC8 → FE AC9~AC14

## 진행

- ✅ PR #47(F-09)·#50(F-08) develop 머지 확인, 브랜치 생성
- ✅ BE AC1~AC8 green: `ConceptDetailApiTest`(12개), `dto/ConceptAppearanceResponseTest`. `GET /api/concepts/{id}` = `ConceptController#detail` → `ConceptService#getDetail`(`ConceptQueryService.getPublished` 재사용) → `ConceptMapper` `findAppearances`·`findCoOccurring`·`findSameCategoryPopular`. openapi.json 갱신
- 🟡 FE AC9~AC14 시작 전

## 바로 다음 할 일

1. AC14: `frontend/src/components/concept/markdown.js` `renderMarkdown` + `__tests__/markdown.spec.js` (red → green)
2. `src/api/concepts.js` `fetchConcept(id)` + `api/__tests__/concepts.spec.js`
3. `MarkdownBody.vue`·`CodeBlock.vue`·`ConceptAppearances.vue`·`views/concept/ConceptDetailView.vue` + 라우트 `/concepts/:id(\\d+)` (`concept-detail`), `ConceptDetailView.spec.js`로 AC9~AC13
4. contract-checker, ui-verifier(375·1280), SC-10 화면설계서·스크린샷 → `/pr-review`

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 파일 변경: `SecurityConfig.PUBLIC_GET_PATHS`(concept 블록에 `/api/concepts/{id:\\d+}`), `ConceptMapper`·XML(B 블록), `ConceptDetailApiTest`(CODEOWNERS 공통) → PR 본문에서 @whans1996 멘션
- 통합 테스트는 Docker Desktop 필요. 마이그레이션 없음
