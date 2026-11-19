# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 갱신: 2026-11-19 15:29 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-09-concept-list

## 현재 기능

F-09 개념 카드 목록·검색 — `docs/specs/F-09-concept-list.md` 구현 중 (`/implement F-09`). 다음 F-10 `docs/specs/F-10-concept-detail.md` APPROVED (F-09 머지 뒤 `feature/F-10-concept-detail`)

## 진행

- ✅ BE AC1~AC8 green: `GET /api/concepts` — `ConceptController#list`, `ConceptService#search`(+`escapeLike`), `ConceptSort`, `dto/ConceptSearchRequest`, `ConceptMapper` B 블록 `findPublished`·`countPublished`, `SecurityConfig.PUBLIC_GET_PATHS`에 `/api/concepts`, openapi.json 갱신. 테스트 `ConceptListApiTest`(22)·`ConceptServiceTest`
- 🟡 FE AC9~AC11 착수

## 바로 다음 할 일

1. FE red: `frontend/src/api/__tests__/concepts.spec.js`(빈 값 제외, `GET /concepts`), `views/__tests__/ConceptListView.spec.js`(AC9~11, `vi.mock('@/api/concepts')`), `utils/__tests__/navItems.spec.js`에 `show: 'all'` 케이스(+기존 AC8 기대값에 '개념 카드' 추가)
2. FE green: `api/concepts.js#fetchConcepts`, `utils/conceptCategories.js` `CONCEPT_CATEGORIES`, `views/concept/ConceptListView.vue`, `components/concept/ConceptFilterBar.vue`, `router/routes/concept.js` `/concepts` + `meta.nav { label: '개념 카드', order: 5, show: 'all' }`, `navItems.js` `visible()`에 `all`
3. contract-checker → ui-verifier(375·1280) → 화면설계서 SC-09 → CODEOWNERS B 경로 → `/pr-review`

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 코드(`SecurityConfig`, `ConceptMapper`, `navItems.js`, concept 패키지 전체가 CODEOWNERS 공통)를 바꾸므로 PR 본문에서 @whans1996 멘션
- 통합 테스트는 Docker Desktop 필요. F-09는 마이그레이션 없음
