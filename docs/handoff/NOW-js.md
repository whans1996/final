# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-24 10:05 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/issue-96-team-json-type-error

---

## 현재 기능

**#96** F-20c 숫자 필드 타입 오류·깨진 JSON이 팀원에게 403보다 400 먼저 (follow-up, `feature/issue-96-team-json-type-error`, develop 8c25c98에서 분기). 방향 (2) 규칙 명시로 결정(11-24), 문서 수정 완료

## 진행

- ✅ #95 PR #100 머지(11-24 09:59), #95 닫힘(수동, close-issues 워크플로 미실행 → #101)
- ✅ develop 8c25c98 verify all 통과 (BE 988, FE 657)
- ✅ conventions 3절에 "형식 검증보다 먼저"의 예외(깨진 JSON·타입 불일치 400, 1MB 초과 413) 명시, 결정로그 1행, `CreateTeamChallengeRequest` 주석 참조 갱신
- 🔴 #101 Actions 결제 한도로 워크플로 전부 미실행 → 사용자 결정(11-24): 로컬 `verify.mjs all`로 검증하며 진행, PR은 복구 뒤 CI 확인

## 바로 다음 할 일

1. `/pr-review` → PR(`Closes #96`, @whans1996 멘션: conventions 공통). Actions 복구(#101) 뒤 CI 확인하고 머지
2. 그다음 #92 (게시글 분석 첨부 viewable이 주인 탈퇴 미고려)

## 막힌 점 / 결정 필요

- 없음

## 주의

- `docs/conventions.md`는 공통 파일 → PR에서 @whans1996 멘션
