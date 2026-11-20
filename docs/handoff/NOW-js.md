# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 14:46 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-12-concept-wish

---

## 현재 기능

F-12 카드 찜 — spec `docs/specs/F-12-concept-wish.md` APPROVED, `/implement` 진행 중 (#57 흡수)

## 진행

- ✅ contract-checker 불일치 0건, ui-verifier 실질 문제 0건(1280·375). 복습 안내가 버튼 상자를 넓혀 평점을 밀던 것 수정(`WishButton` 루트 display: contents), 산출물 06·02·01 + 스크린샷 4장

- ✅ BE AC1~AC11 green (BE 566건 통과): `concept/wish/ConceptWishController·Service·Mapper(+XML)·dto/ConceptWishResponse`, `ConceptDetailResponse.wished`, `ActivityService.recordOnce`·`ActivityMapper.exists`, `PUBLIC_GET_PATHS`에 `/api/users/{id:\\d+}/wishes`, openapi.json 갱신
- 테스트: `ConceptWishApiTest`(AC1~AC11, AC7도 여기), `ConceptWishConcurrencyTest`(AC2 같은 회원·AC6 다른 회원 동시), `ConceptWishServiceTest`(DuplicateKey), `feed/ActivityServiceTest`
- spec과 다른 점: 쓰기 순서를 F-11처럼 `lockConcept` → `getPublished` → `getActiveMember`로 (잠금 전 스냅샷 문제)
- ✅ FE AC12~AC15 green (verify fe 통과): API 함수 3개, `WishButton.vue`, `UserWishes.vue`, `ConceptDetailView`·`UserProfileView` 연결. F-10 AC9 테스트의 "♥ 12" 기대를 "♡ 12"로 (AC12 빈 하트)

## 바로 다음 할 일

1. `/clear` → `/pr-review` (Opus/high): reviewer → PR 본문 `Closes #57`, @whans1996 멘션(concept 패키지·SecurityConfig·feed·src/api/concepts.js 공통), 스크린샷 `node scripts/pr-screens.mjs SC-10 SC-18`
2. PR 후속에 적기: F-04 spec 작성 시 WISH 활동은 `concept_wishes`(아직 찜 중)·`concepts.status='PUBLISHED'` JOIN으로 거르기
3. 임시 서버(BE 8081·FE 5174, 이 세션 백그라운드)는 세션이 끝나면 내려간다. 8080/5173은 11-19 구버전 → 사람이 재시작 필요

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 파일 변경: `concept` 패키지 전체(CODEOWNERS 공통)·`SecurityConfig`·`feed/ActivityService`·`src/api/concepts.js` → PR에서 @whans1996 멘션
- 동환 F-16이 `router/index.js`·헤더 nav를 바꾼다 → `App.spec.js`·`navItems.spec.js` 충돌 가능. F-16은 `concept_wishes`를 읽는다
- `codex-review.mjs`가 Codex 실행 실패를 RUNNING으로 기다린다 (#48)
