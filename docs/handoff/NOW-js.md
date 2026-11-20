# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 11:37 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-11-concept-review

---

## 현재 기능

F-11 카드 리뷰 — **DONE (PR #58)**. CI 초록, Codex ✅ d2a8fab. **머지만 남음 (GitHub에서 직접)**

## 진행

- ✅ BE AC1~AC12, FE AC13~AC18, 산출물 01·02·06 + SC-10 스크린샷
- ✅ /pr-review: reviewer 반드시 2 + 권장 3, Codex 재리뷰 4회(P1 탈퇴 회원 토큰 `6d5169b`, 더 보기 offset `d3e4f02`, 읽기·쓰기 직렬화 `9a48fd1`·`d2a8fab`) 모두 PR에서 수정, 이관 없음
- ✅ spec DONE 표시 (이 커밋)

## 바로 다음 할 일

1. PR #58 머지 (GitHub, wip 커밋 없음 → 일반 머지 가능). 머지 전 상대 PR(F-15)이 먼저 들어갔으면 `git merge origin/develop` → push → CI 초록 재확인
2. 머지 후 `/clear` → `/pickup`으로 다음 B 영역 기능 선택 (F-12 찜은 #57 `ReviewPlanService.addWish` 연동 흡수, F-15 머지 뒤가 좋음)

## 막힌 점 / 결정 필요

- 없음

## 주의

- 로컬 8080 BE는 11-19부터 떠 있던 옛 코드다 (리뷰 API 없음). 화면을 직접 보려면 재시작
- 로컬 DB에 점검용 회원 `f11ui@vibenote.local`과 카드 1의 리뷰 1건이 있다
- F-04 피드는 지워진 리뷰의 REVIEW 활동을 `concept_reviews` JOIN으로 걸러야 함
- `codex-review.mjs`가 Codex 실행 실패(status failed)를 RUNNING으로 기다린다 (#48, PR #58에서 재현) → 오래 걸리면 PR 댓글 `codex-review-state`를 직접 본다
