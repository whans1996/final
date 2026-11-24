# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-24 10:34 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/issue-96-team-json-type-error

---

## 현재 기능

**#96** F-20c "형식 검증보다 먼저"의 예외 명시 (follow-up, `feature/issue-96-team-json-type-error`). **PR #102 열림**, Codex ✅ ea6108a (지적 없음). CI는 #101로 미실행

## 진행

- ✅ 방향 (2) 규칙 명시로 결정(11-24): `docs/conventions.md` 3절 문단 93930ad + reviewer 권장 2건(본문 없음·JSON null·415, `boardType` 예시) ea6108a, 결정로그 1행
- ✅ verify all 통과(BE 988, FE 657), reviewer 반드시 수정 0, contract-checker 생략(API 변경 없음)
- ✅ PR 설명 `.harness/pr-issue-96.md` (Closes #96, @whans1996 멘션), PR #102, Codex ✅ ea6108a

## 바로 다음 할 일

1. #101 복구(저장소 주인 whans1996 계정 Actions 결제·한도) 뒤 PR #102 CI 재실행(`gh run rerun <id>` 또는 빈 docs 커밋 push) → 초록 확인 → GitHub에서 머지 → #96 닫혔는지 확인
2. 다음 이슈: #92 (F-17 게시글 분석 첨부 viewable이 주인 탈퇴 미고려, F-19 공개 열람과 불일치). `feature/issue-92-...`를 develop에서 분기
3. 그다음 산출물: `docs/산출물/03-ERD.md`를 현재 스키마(V8·V9 팀, F-24·25 챌린지)에 맞춰 갱신. SC-01 랜딩·디자인 기준은 F-25 머지 후 2차 기획에서 디자인 방향을 정한 뒤 (사용자 합의 11-24)

## 막힌 점 / 결정 필요

- 🔴 #101 Actions 미실행 → 머지 대기 (사용자 결정 11-24: 로컬 verify로 진행, CI는 복구 뒤 확인)

## 주의

- PR #102 CI 체크 fail은 전부 job 미시작(2초, 결제 한도)이며 테스트 실패가 아니다
