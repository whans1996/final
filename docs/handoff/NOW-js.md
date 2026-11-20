# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신: 2026-11-20 09:44 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-11-concept-review

## 현재 기능

F-11 카드 리뷰 (`docs/specs/F-11-concept-review.md` APPROVED). **AC1~AC18 모두 green**, 남은 것: `/pr-review`

## 진행

- ✅ BE AC1~AC12 (`concept/review/`, `ConceptReviewApiTest` 18개 + `ConceptReviewServiceTest`), ErrorCode `CONCEPT_001`·`CONCEPT_409`, `PUBLIC_GET_PATHS`, openapi.json
- ✅ FE AC13~AC18 (`api/concepts.js` 5개, `StarRating`·`ReviewForm`·`ConceptReviews`, 상세 헤더 "· 리뷰 N")
- ✅ contract-checker 불일치 0, ui-verifier 1280·375 문제 0 (스크린샷 `SC-10-reviews-*.png`), 산출물 01·02·06 반영
- ✅ develop(F-14 PR #56) 병합, `docs/conventions.md` 충돌 해결 (CONCEPT·QUIZ 행 모두 유지)

## 바로 다음 할 일

1. `/clear` 후 새 세션(Opus/high)에서 `/pr-review` — verifier·reviewer·contract-checker → PR(develop 대상, 본문에 @whans1996 멘션: 공통 파일 `SecurityConfig`·`ErrorCode`·concept 패키지·`components/concept`·`src/api/concepts.js`) → Codex 루프
2. PR 본문 "spec과 다른 점": 리뷰 목록 400(PageQuery) 문서화, 개념 카드 Tag 설명 상수 공유, `rating-change` 첫 이벤트는 `{reviewCount}`만 담음

## 막힌 점 / 결정 필요

- 없음

## 주의

- 로컬 8080 BE는 11-19부터 떠 있던 옛 코드다 (리뷰 API 없음). 점검은 8081/5174 임시 서버로 했고 종료함. 화면을 직접 보려면 8080 BE를 재시작
- 로컬 DB에 점검용 회원 `f11ui@vibenote.local`(리뷰점검)과 카드 1의 리뷰 1건이 있다
- 통합 테스트는 트랜잭션 하나라 같은 MyBatis 쿼리를 JDBC insert 뒤 다시 부르면 1차 캐시가 답한다 (AC7 테스트 주석)
- F-04 피드는 지워진 리뷰의 REVIEW 활동을 `concept_reviews` JOIN으로 걸러야 함
