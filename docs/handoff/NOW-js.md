# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 갱신: 2026-11-20 10:20 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-11-concept-review

## 현재 기능

F-11 카드 리뷰 (`docs/specs/F-11-concept-review.md` APPROVED). AC1~AC18 green, **`/pr-review` 진행 중 (점검·수정 끝, PR 생성 확인 대기)**

## 진행

- ✅ BE AC1~AC12, FE AC13~AC18, 산출물 01·02·06 + SC-10 스크린샷, develop(F-14) 병합
- ✅ /pr-review 점검: verifier 통과, contract-checker 0건, reviewer 반드시 수정 2건 반영
  - `b122096` 카드 행 잠금을 `getPublished`보다 먼저 (`ConceptReviewConcurrencyTest` red 1 → green 2)
  - `3b06bca` 내 리뷰 삭제 뒤 "더 보기"가 리뷰 건너뜀 → 받은 개수로 다음 쪽, 한 쪽 분량 안 되면 1회 더
- ✅ 권장 3건 반영: `3120c6f` 더 보기 뒤 rating-change({reviewCount})·끝이면 버튼 숨김, `a216818` ConceptReview → `review/domain/`, Tag 상수 → `concept.ConceptApiDocs`
- ✅ PR 설명 `.harness/pr-F-11.md` (스크린샷 자리 `SCREENSHOTS`는 push 후 `node scripts/pr-screens.mjs SC-10`의 reviews-1280·375로 채움)

## 바로 다음 할 일

1. 사용자 확인 후 `git push` → 스크린샷 채우기 → `gh pr create --base develop --title "feat(F-11): 카드 리뷰 작성·조회·수정·삭제" --body-file .harness/pr-F-11.md`
2. `gh pr checks` → Codex 루프 (`node scripts/codex-review.mjs <PR> --request --wait --status`, background)

## 막힌 점 / 결정 필요

- 없음

## 주의

- 로컬 8080 BE는 11-19부터 떠 있던 옛 코드다 (리뷰 API 없음). 화면을 직접 보려면 재시작
- 로컬 DB에 점검용 회원 `f11ui@vibenote.local`과 카드 1의 리뷰 1건이 있다
- 통합 테스트는 트랜잭션 하나라 JDBC insert 뒤 같은 MyBatis 쿼리는 1차 캐시가 답한다 (AC7 테스트 주석)
- F-04 피드는 지워진 리뷰의 REVIEW 활동을 `concept_reviews` JOIN으로 걸러야 함
