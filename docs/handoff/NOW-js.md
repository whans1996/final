# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 갱신: 2026-11-19 15:23 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-09-concept-list

## 현재 기능

F-09 개념 카드 목록·검색 — `docs/specs/F-09-concept-list.md` **APPROVED**, 구현 착수 가능 (F-00b PR #42 develop 머지됨). 다음 F-10 `docs/specs/F-10-concept-detail.md` APPROVED (F-09 머지 뒤 `feature/F-10-concept-detail`)

## 진행

- ✅ F-09·F-10 spec 승인, 화면 설계서 B 화면 초안
- ✅ `git merge origin/develop` (F-00b 포함). 화면 설계서 충돌: develop의 SC-20·21 관리자 공통 레이아웃 + 내 SC-20 카드 관리를 둘 다 두고, SC-20 진입·권한을 공통 레이아웃(사이드 메뉴 `meta.adminNav`, USER는 홈)에 맞춤
- ✅ F-09 spec을 F-00b 실제 결과에 맞춤: DTO 일치, `PageNav`·`EmptyState`·`LEVELS`(utils/authValidation)·route `meta.nav` 사용, `src/api/concepts.js`는 새로 만듦
- 🟡 js-ssafy 착수 체크리스트: 교육장 PC에서 `/pickup` 확인만 남음

## 바로 다음 할 일

1. `/clear` → `/effort high` → `/implement F-09`. 마이그레이션 없음. 첫 테스트 `backend/src/test/java/com/vibenote/concept/ConceptListApiTest#AC1_...` (MockMvc+Testcontainers, 테스트가 고유 이름 카드를 직접 넣음)
2. BE: `ConceptController#list`(새 파일), `ConceptService#search`(keyword `!` 이스케이프), `ConceptMapper` B 블록에 `findPublished`·`countPublished`, `ConceptSearchRequest` 검증, `SecurityConfig.PUBLIC_GET_PATHS` CONCEPT 블록에 `/api/concepts`
3. FE: `api/concepts.js`, `views/concept/ConceptListView.vue`, `components/concept/ConceptFilterBar.vue`, `routes/concept.js`에 `meta.nav { show: 'all' }` + `utils/navItems.js`에 `all` 추가

## 막힌 점 / 결정 필요

- 없음. F-10 리뷰 관련 미정(1인 1리뷰, 길이, 페이지 크기)은 F-11 spec으로 넘김

## 주의

- 공통 코드(`SecurityConfig`, `navItems.js`)를 바꾸므로 PR 본문에서 @whans1996 멘션
- 통합 테스트는 Docker Desktop 필요. develop 마이그레이션은 V5까지(F-09는 마이그레이션 없음)
