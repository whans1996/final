# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 11:55 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-17-community-board

---

## 현재 기능

F-17 커뮤니티 게시판 — spec `docs/specs/F-17-community-board.md` **APPROVED (김준섭 11-20)**, 구현 전 (F-11은 PR #58 머지 완료)

## 진행

- ✅ spec 초안: BE AC1~12 (목록·검색·상세·조회수·첨부 표시·작성·수정·삭제·권한), FE AC13~18 (SC-14·15·16 + SC-10 글쓰기 버튼). 마이그레이션 없음
- ✅ 결정 4건(분석 첨부 = 내 DONE 분석·표시는 공개/주인만, 조회수 매번 +1, 제목+본문 검색, SC-10 버튼 + `?conceptId=`·`?analysisId=`) → 결정로그 추가, 승인 커밋 push

## 바로 다음 할 일

1. `/clear` → `/implement F-17` (Opus/high): 9절 결정(PUT 403을 400보다 먼저) 고르고 `backend/src/test/.../post/PostListApiTest#AC1_...`부터 red
2. BE AC1~12 → FE AC13~18 순서. `ErrorCode` POST 블록·`SecurityConfig.PUBLIC_GET_PATHS` POST 블록에만 추가

## 막힌 점 / 결정 필요

- spec 9절: `PUT`에서 작성자 확인(403)을 형식 검증(400)보다 먼저 하는 방법 (구현 착수 때)

## 주의

- F-15(동환) 아직 PR 전 → F-12 찜(#57 `addWish` 연동)은 F-15 머지 뒤
- SC-07 "커뮤니티에 질문하기" 버튼은 구현 PR에서 follow-up 이슈(area:analysis, whans1996)로 만든다
- `codex-review.mjs`가 Codex 실행 실패를 RUNNING으로 기다린다 (#48) → 오래 걸리면 PR 댓글 `codex-review-state`를 직접 본다
