# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 16:06 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-18-comments

---

## 현재 기능

F-18 댓글 — spec **APPROVED**, `/implement` 진행 중. 이번 세션 목표: BE AC1~10 → FE AC11~13

## 진행

- ✅ spec 승인·결정(오래된순 200 상한, 1~500자, 활동 없음, 모바일 글자 버튼)
- ✅ BE AC1~10 green (BE 617 통과): `post/Comment*` + `ErrorCode.POST_003·004` + `PUBLIC_GET_PATHS` + openapi.json, conventions 3절
- ✅ FE AC11~13 green (verify fe 통과): `api/posts.js` 함수 4개, `components/community/CommentForm·CommentItem·PostComments.vue`, `PostDetailView`에 댓글 절

## 바로 다음 할 일

1. contract-checker (spec 3절 ↔ openapi.json ↔ `src/api/posts.js`)
2. BE(local)·FE dev 띄우고 ui-verifier: `/community/{id}` 댓글 있음·없음·내 댓글 수정 중, 데스크톱·375px
3. 산출물: `docs/산출물/06-화면설계서.md` SC-15 "F-18에서 정할 것"·모바일 ⋯ 문구 → 글자 버튼, `02-유스케이스.md` UC41 "기획 기준" 정리 → `/pr-review`
4. PR 본문에 적기: 수정은 내용이 같으면 UPDATE 생략, 다르면 `updated_at`을 앱 Clock으로 (spec 4절 "ON UPDATE" 문구와 다름)

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 파일 변경 예정(`SecurityConfig.PUBLIC_GET_PATHS`, `ErrorCode`) → PR 본문에서 @whans1996 멘션
- `codex-review.mjs`가 Codex 실행 실패를 RUNNING으로 기다린다 (#48)
