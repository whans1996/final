# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 14:25 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-12-concept-wish

---

## 현재 기능

F-12 카드 찜 — spec `docs/specs/F-12-concept-wish.md` **APPROVED (김준섭 11-20)**, 구현 전 (#57 흡수). F-17은 PR #61 머지 완료

## 진행

- ✅ 결정 3건(11-20, 결정로그): 찜 목록 공개 `GET /api/users/{id}/wishes`, 토글은 SC-10 상세만, WISH 활동은 카드당 처음 한 번만
- ✅ spec 초안: BE AC1~AC11(찜·해제 멱등 200, wish_count 잠금 재계산, addWish, 상세 `wished`, 찜 목록 PUBLISHED만), FE AC12~AC15(`WishButton` 낙관적 토글, 프로필 `UserWishes`)

## 바로 다음 할 일

1. `/clear` → `/implement F-12` (Opus/high), 브랜치 `feature/F-12-concept-wish`(push됨). 마이그레이션 없음
2. 첫 테스트 `backend/src/test/java/com/vibenote/concept/wish/ConceptWishApiTest#AC1_찜하면_찜_수가_늘고_내일_복습_계획에_들어간다` (red) → `ConceptWishController`·`ConceptWishService`·`ConceptWishMapper`

## 막힌 점 / 결정 필요

- 없음 (구현 전 확인: `PUBLIC_GET_PATHS`에 `/api/users/{id:\\d+}/wishes` 추가가 `/users/me` 경로를 열지 않는지 AC11로 확인)

## 주의

- 공통 파일 변경 예정: `ConceptController`·`ConceptService`·`SecurityConfig`·`feed/ActivityService`·`src/api/concepts.js` → PR에서 @whans1996 멘션
- 동환 F-16이 `router/index.js`·헤더 nav를 바꾼다 → 머지 순서에 따라 `App.spec.js`·`navItems.spec.js` 충돌 가능. F-16은 `concept_wishes`를 읽는다
- `codex-review.mjs`가 Codex 실행 실패를 RUNNING으로 기다린다 (#48)
