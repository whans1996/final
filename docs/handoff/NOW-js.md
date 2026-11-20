# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 16:35 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-18-comments

---

## 현재 기능

F-18 댓글 — 구현 완료(AC1~13), `/pr-review` 진행 중 (PR 아직 없음)

## 진행

- ✅ BE AC1~10, FE AC11~13 green, 산출물 UC41·SC-15 반영, ui-verifier 통과
- ✅ /pr-review 점검: verifier 통과, contract-checker 불일치 0, reviewer 반드시 수정 0
- 🔄 권장 3건 반영(미커밋): spec 4절 수정 시각 문구·AC13 문구 + 결정로그 1줄, `CommentWriteApiTest#AC4_없거나_지워진_글에_...`에 빈 내용 PUT→404 한 줄, `CommentItem.onDelete` 404 `POST_003`이면 `changed` emit(+`CommentItem.spec.js` 403 테스트 추가). verify all 재실행 중
- PR 설명 초안 `.harness/pr-F-18.md` (스크린샷 칸은 `SCREENSHOTS_PLACEHOLDER`)

## 바로 다음 할 일

1. verify all 통과 확인 → `fix(F-18): PR 전 리뷰 권장 반영 (404 순서 테스트, 삭제 404 목록 갱신, spec 4절)` 커밋 → push
2. `node scripts/pr-screens.mjs SC-15` 출력으로 PR 설명 스크린샷 칸 교체 → 사용자 확인 후 `gh pr create --base develop --title "feat(F-18): 커뮤니티 댓글 (목록·작성·수정·삭제)" --body-file .harness/pr-F-18.md`
3. `gh pr checks` 확인, Codex 루프(`node scripts/codex-review.mjs <PR> --request --wait --status` 백그라운드)

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 파일 변경(`SecurityConfig.PUBLIC_GET_PATHS`, `ErrorCode`) → PR 본문에 @whans1996 멘션 (초안에 넣음)
- `codex-review.mjs`가 Codex 실행 실패를 RUNNING으로 기다린다 (#48)
