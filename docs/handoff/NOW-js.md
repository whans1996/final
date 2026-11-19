# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 갱신: 2026-11-19 15:35 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-09-concept-list

## 현재 기능

F-09 개념 카드 목록·검색 — `docs/specs/F-09-concept-list.md` 구현 중 (`/implement F-09`). 다음 F-10 `docs/specs/F-10-concept-detail.md` APPROVED (F-09 머지 뒤 `feature/F-10-concept-detail`)

## 진행

- ✅ BE AC1~AC8 커밋·push `33497a0`: `GET /api/concepts` (`ConceptController#list`, `ConceptService#search`, `ConceptSearchRequest`, `ConceptMapper` `findPublished`·`countPublished`, `PUBLIC_GET_PATHS`, openapi.json)
- ✅ FE AC9~AC11 커밋: `api/concepts.js`, `utils/conceptCategories.js`, `components/concept/ConceptFilterBar.vue`, `views/concept/ConceptListView.vue`, `routes/concept.js`(nav order 5 `show:'all'`), `navItems.js`(`all`), 테스트 concepts·ConceptListView·navItems·App.spec(헤더 기대값에 '개념 카드'). verify fe 통과

## 바로 다음 할 일

1. `contract-checker`(spec 3절 ↔ openapi.json ↔ `api/concepts.js`) → BE(local)·FE dev 띄우고 `ui-verifier` `/concepts` 375·1280px
2. `docs/산출물/06-화면설계서.md` SC-09 + `screens/SC-09-*.png`, `.github/CODEOWNERS`에 B 경로(`views/concept/`, `components/concept/ConceptFilterBar.vue`, concept BE 파일) 추가 → `/pr-review`

## 막힌 점 / 결정 필요

- 없음. 헤더 메뉴 "개념 카드" order 5(맨 앞)는 임의로 정함 — 사용자 확인 대기

## 주의

- 공통 코드(`SecurityConfig`, `ConceptMapper`, `navItems.js`, concept 패키지·`routes/concept.js` 등 CODEOWNERS 공통)를 바꾸므로 PR 본문에서 @whans1996 멘션
- 통합 테스트는 Docker Desktop 필요. F-09는 마이그레이션 없음
