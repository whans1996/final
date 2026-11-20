# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 13:54 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-17-community-board

---

## 현재 기능

F-17 커뮤니티 게시판 — **PR #61** 열림, **Codex ✅ 77ce03d** (재리뷰 2회, P2 1건 수정: 미리 채우기 중 폼 로딩), CI 확인 중

## 진행

- ✅ develop(F-15) 합치기 5a7684d: 헤더 순서 개념 카드·커뮤니티·분석 요청·내 분석·복습, 카드 상세에 복습 추가 버튼 + 글쓰기 버튼, conventions REVIEW_PLAN·POST, openapi 재생성
- ✅ verifier·contract-checker 0건·reviewer 반드시 수정 0건 / 권장 반영 6223e8e: 수정 화면에서 viewable=false 첨부는 빼고 열기(`PostWriteView.attachmentFromPost`, AC18 테스트 추가), `backend/CLAUDE.md` 패키지 `post` · follow-up #60(SC-07 질문하기 버튼, area:A)

## 바로 다음 할 일

1. `gh pr checks 61` 초록 확인 (하네스 테스트 실패는 NOW 머리말 깨짐 → 이 커밋에서 고침) → spec 헤더 `DONE (PR #61)`·체크리스트 마지막 칸 → `docs(F-17): 완료 표시 (spec DONE)` 커밋·push → 사람이 GitHub에서 머지

## 막힌 점 / 결정 필요

- 없음

## 주의

- 로컬 DB에 UI 점검용 f17ui@vibenote.test·글 1~3 / F-12 찜 → 복습 계획 연동(#57)은 F-15 머지됐으니 다음 기능 후보
- `codex-review.mjs`가 Codex 실행 실패를 RUNNING으로 기다린다 (#48)
