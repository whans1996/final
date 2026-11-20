# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 15:53 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-18-comments

---

## 현재 기능

F-18 댓글 — spec **APPROVED** (김준섭 11-20, `docs/specs/F-18-comments.md`). 브랜치 `feature/F-18-comments`, 구현 전

## 진행

- ✅ F-18 spec 승인 커밋·push (BE AC1~10, FE AC11~13)
- ✅ 결정(11-20, 결정로그): 오래된순 한 번에·200 상한, 1~500자, 피드 활동 없음, 모바일도 글자 버튼

## 바로 다음 할 일

1. `/clear` → `/implement F-18` (Opus/high): 마이그레이션 없음(V1 `comments` 그대로, spec 5절)
2. BE `CommentListApiTest`(AC1~3)·`CommentWriteApiTest`·`CommentAuthApiTest` red부터. 패키지 `com.vibenote.post`에 `CommentController`·`CommentService`·`CommentMapper`

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 파일 변경 예정(`SecurityConfig.PUBLIC_GET_PATHS`, `ErrorCode`) → PR 본문에서 @whans1996 멘션
- `codex-review.mjs`가 Codex 실행 실패를 RUNNING으로 기다린다 (#48)
