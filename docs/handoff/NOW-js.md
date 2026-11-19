# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 갱신: 2026-11-19 15:37 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-09-concept-list

## 현재 기능

F-09 개념 카드 목록·검색 — `docs/specs/F-09-concept-list.md` 구현 완료, PR 전 (`/pr-review` 남음). 다음 F-10 `docs/specs/F-10-concept-detail.md` APPROVED (F-09 머지 뒤 `feature/F-10-concept-detail`)

## 진행

- ✅ BE AC1~AC8 `33497a0`, FE AC9~AC11 `aab58f4`, 목록 전체 개수·SC-09 화면설계서·스크린샷·CODEOWNERS(`api/concepts.js`·`utils/conceptCategories.js` 공통) 커밋
- ✅ contract-checker 불일치 0건, ui-verifier 문제 0건 (1280·375, 가로 스크롤 없음)

## 바로 다음 할 일

1. `/clear` 후 `/pr-review` (Opus high). PR 본문 "공통 코드 변경"에 @whans1996 멘션: `SecurityConfig.PUBLIC_GET_PATHS`(`/api/concepts`), `ConceptMapper`(B 블록), concept 패키지 새 파일, `navItems.js`(`show: 'all'`), `routes/concept.js`, `App.spec.js`·`navItems.spec.js` 헤더 기대값, CODEOWNERS
2. 스크린샷 Markdown: `node scripts/pr-screens.mjs SC-09`

## 막힌 점 / 결정 필요

- 헤더 메뉴 "개념 카드" order 5(맨 앞)는 임의로 정함 — 사용자 확인 대기
- (선택) 375px에서 카테고리·난이도 select가 둘 다 "전체"로 보여 구분이 어려움 → 첫 옵션을 "카테고리 전체"·"난이도 전체"로 바꿀지 사용자 확인

## 주의

- 김동환 PR #43(유스케이스)이 B 영역 UC 흐름 확인을 요청할 예정
- 통합 테스트는 Docker Desktop 필요. F-09는 마이그레이션 없음
