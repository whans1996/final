# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 11:28 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-11-concept-review

---

## 현재 기능

F-11 카드 리뷰 (`docs/specs/F-11-concept-review.md` APPROVED). AC1~AC18 green, **PR #58 열림 (develop 대상), CI·Codex 루프 진행 중**

## 진행

- ✅ BE AC1~AC12, FE AC13~AC18, 산출물 01·02·06 + SC-10 스크린샷, develop(F-14) 병합
- ✅ /pr-review: verifier 통과, contract-checker 0건, reviewer 반드시 수정 2건(`b122096` 잠금 순서 + `ConceptReviewConcurrencyTest`, `3b06bca` 삭제 뒤 더 보기) + 권장 3건(`3120c6f`, `a216818`) 반영
- ✅ PR #58 https://github.com/whans1996/final/pull/58 (head `271bc52`, @whans1996 멘션). CI pull_request 실행 시작
- ✅ CI 초록 (`c40b468`, 하네스 검사 실패는 NOW "갱신:" 줄 형식 → 복구)
- Codex: 1차 실행 실패(#48 증상) → 재리뷰1 P2 2건 `d3e4f02` → 재리뷰2 P1 탈퇴 회원 토큰(`6d5169b`, `getActiveMember`) + P2 첫 목록 늦은 응답(`9a48fd1`, 첫 조회 중 쓰기 비활성) → **재리뷰 3회째 대기**
- FE 경쟁 상태 주제 2회째. 또 같은 주제 지적이면 멈추고 사용자에게 (이관/설계 변경)

## 바로 다음 할 일

1. Codex 판정 처리: COMPLETE면 여기 `Codex ✅ <sha>`, FINDINGS면 spec·보안 위반만 PR에서 고치고(테스트 먼저) 나머지 P2는 follow-up 이슈로 이관 → `--resolve`
2. `gh pr checks 58` 초록 확인 → 완료 표시 커밋 `docs(F-11): 완료 표시 (spec DONE)` (spec 헤더 `DONE (PR #58)`·체크리스트 마지막 칸만) → 머지는 GitHub에서 직접

## 막힌 점 / 결정 필요

- 없음

## 주의

- 로컬 8080 BE는 11-19부터 떠 있던 옛 코드다 (리뷰 API 없음). 화면을 직접 보려면 재시작
- 로컬 DB에 점검용 회원 `f11ui@vibenote.local`과 카드 1의 리뷰 1건이 있다
- F-04 피드는 지워진 리뷰의 REVIEW 활동을 `concept_reviews` JOIN으로 걸러야 함
