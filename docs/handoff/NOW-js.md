# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 12:21 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-17-community-board

---

## 현재 기능

F-17 커뮤니티 게시판 — spec `docs/specs/F-17-community-board.md` APPROVED, **AC1~18 구현·테스트 green**, `/pr-review` 전

## 진행

- ✅ BE AC1~12: `com.vibenote.post` (Controller·Service·Mapper+XML·DTO 7개), `PUT`은 404→403 뒤 `Validator` 직접 검증(spec 9절). 테스트 `backend/src/test/java/com/vibenote/post/` 5개 클래스
- ✅ FE AC13~18: `api/posts.js`, `routes/community.js`(nav 커뮤니티 order 6), `components/community/` 4개+`boardTypes.js`, `views/community/` 3개, SC-10 "이 카드로 글쓰기"
- ✅ contract-checker 불일치 0 (참고 2: openapi `PostDetailResponse.analysis·concept`에 `$ref`+`type:null`, edit 진입 시 조회수 +1) · ui-verifier 12개 라우트 문제 0
- ✅ 산출물: 화면설계서 SC-10·14·15·16(+스크린샷 6장), conventions 3절 POST 코드

## 바로 다음 할 일

1. `/clear` → `/pr-review` (Opus/high): verifier → reviewer → PR. PR 본문에 contract-checker 참고 2건, @whans1996 멘션(공통 파일), follow-up 이슈 생성(SC-07 "커뮤니티에 질문하기" 버튼 → `/community/write?analysisId=`, area:analysis, 담당 whans1996)

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 파일 변경: `SecurityConfig.PUBLIC_GET_PATHS`·`ErrorCode` POST 블록, `App.spec.js`·`navItems.spec.js` 헤더 기대값(F-15 복습 메뉴와 충돌 가능 → 먼저 머지되는 쪽 뒤에 develop 합치며 둘 다 넣기)
- 로컬 DB(vibenote)에 UI 점검용 회원 f17ui@vibenote.test·글 1~3을 만들었다. 8080 BE는 어제 코드로 떠 있어 8081/5174로 따로 띄워 점검함
- F-15(동환) 아직 PR 전 → F-12 찜(#57 `addWish` 연동)은 F-15 머지 뒤
- `codex-review.mjs`가 Codex 실행 실패를 RUNNING으로 기다린다 (#48)
