# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-26 15:25 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/V-1b-f17-fixes

---

## 현재 기능

**V-1b F-17 묶음** (`feature/V-1b-f17-fixes`, 로드맵 V-1b ~11/28) — 2차 점검 이슈 #210 → #207 → #209 → #206 순서. #206은 **(a) 비공개 분석 첨부 금지**로 결정(11-26, 사용자). 정한 뒤 @whans1996에게 알림(F-19 공개 규칙). #208(F-18 댓글 페이지)은 다음 PR, #211은 V-1b에서 뺌(A 영역 화면, B 여유 작업과 함께)

## 진행

- ✅ #210 삭제 확인에 "댓글 n개도 함께 삭제됩니다": `PostComments`가 목록 받을 때 `count` emit → `PostDetailView.liveCommentCount ?? post.commentCount`. 테스트 `PostDetailView.spec.js` AC16 #210 3건, `PostComments.spec.js` AC12 #210
- ✅ #207 `PostWriteView.prefillAttachment`가 `analysis.mine !== true`면 빼고 "첨부할 수 없는 항목이라 빼고 열었습니다"(spec AC18 문구). 테스트 `PostWriteView.spec.js` AC18 #207
- PR #214(#170) 머지·#170 닫힘 (11-26)

## 바로 다음 할 일

1. #209 수정 화면 조회수 2 증가: `post/PostService.java:67-74`·`PostMapper.xml:39-41`·`PostWriteView.vue:68,154` (수정용 조회는 조회수 안 올림)
2. #206 (a): `PostMapper.xml:87-91` `checkAttachments`에 공개 조건, `PostService.java:149-162`, `AttachmentPicker.vue:21,61-62` 비활성·안내 → PR(`Roadmap: V-1b (일부)`, Closes #206 #207 #209 #210, @whans1996)
3. V-2 DS-03·04 고정 **11/28** (결정로그 11-26 R-1): eval `sets.json` `frozenAt` 기록·push → 메타데이터 docs PR(`Roadmap: V-2`). 라벨 9/9 reviewed
4. 11/27 Figma 공동 세션 → F-29 `/spec` (11/29~)

## 막힌 점 / 결정 필요

- SC-01(랜딩 구현 때): 샘플 분석 데이터 출처, 로그인 회원 `/` → 대시보드, 비회원 랜딩 인기순 공개 카드 (F-16 spec 9절)

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용(개념 tier·개수, 결함, forbidden, 채점 기준)을 적지 않는다.** 진행률·eval 커밋 SHA만
- **Figma는 공동 세션 전까지 건드리지 않는다** (김동환 요청)
- B 일정: V-1b·V-2 ~11/28, EV-1 11/29~12/01, F-29 11/29~12/02, V-4 FE 12/03~08, U-1 12/09~14. 기획서 579·WBS 4.1의 DS 12/03은 /progress 때 11/28로
