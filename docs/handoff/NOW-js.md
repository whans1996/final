# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-24 11:02 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/issue-92-post-attachment-owner-withdrawn

---

## 현재 기능

**#92** F-17 게시글 분석 첨부 viewable이 주인 탈퇴 미고려 (follow-up, `feature/issue-92-post-attachment-owner-withdrawn`, develop 8c25c98에서 분기). 수정 커밋 97bad5c, `/pr-review` 진행 중

## 진행

- ✅ #96 PR #102: Actions 복구(#101 해소 추정, 11-24 10:50경) 뒤 CI 재실행 → 초록, Codex ✅ ea6108a → **사람이 GitHub에서 머지할 차례**
- ✅ #92 red→green: `PostDetailApiTest#AC5_탈퇴한_회원의_공개_분석은_남과_비로그인에게_숨긴다`
  - `PostMapper.xml` findDetail에 `analysis_owner_active`, `Post.analysisOwnerActive`, `PostAnalysisAttachment.from` 조건 `(공개 AND 주인 ACTIVE) OR 내 분석`
  - Swagger 설명 → `openapi.json` 1줄, spec F-17 AC5·테스트 계획 문구 보완
- ✅ 커밋 hook BE 테스트 통과

## 바로 다음 할 일

1. `/pr-review` 이어서: verifier all + reviewer → PR 설명 `.harness/pr-issue-92.md` (Closes #92) → push·PR → Codex 루프
2. PR #102 머지 확인 → #96 닫혔는지, #101 닫기(Actions 복구 확인 후)
3. 그다음 산출물: `docs/산출물/03-ERD.md`를 현재 스키마(V8·V9 팀, F-24·25 챌린지)에 맞춰 갱신. SC-01 랜딩·디자인 기준은 F-25 머지 후 2차 기획에서 (사용자 합의 11-24)

## 막힌 점 / 결정 필요

- 없음

## 주의

- 로컬 BE(8080)를 이 세션에서 develop 기준으로 다시 띄워 둠 (직접 써 보기용, 나중에 진행하기로 함)
