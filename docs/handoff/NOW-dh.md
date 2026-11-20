# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-20 14:48 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/F-16-recommendation

## 현재 기능

F-16 맞춤 개념 카드 추천 — **PR #62 열림** (head `f8d6dd9`), CI 진행 중, Codex 리뷰 대기 중

## 진행

- ✅ verify all, contract-checker 불일치 0, reviewer 반드시 수정 0·권장 2건 반영 `928eb83`, '무시 가능' 7건 확인(spec 4절 문구 수정)
- ✅ develop(F-17 PR #61) 합침 `f8d6dd9`: App.spec.js 헤더 메뉴 충돌 해결(대시보드→개념 카드→커뮤니티), BE 574 통과
- ⚠️ 로컬 FE: B의 `PostDetailView.spec.js` AC15·AC16 2건이 dh-home에서만 실패(`/community` 동적 import 타이밍 추정, develop과 동일 파일, PR #61 CI는 통과) → PR #62 CI로 판단
- PR 설명 `.harness/pr-F-16.md` (스크린샷 4장, @JoonseobKim 멘션)

## 바로 다음 할 일

1. `gh pr checks 62` 초록 확인. FE 실패면 `gh run view --log-failed`로 PostDetailView인지 보고 B에게 알림
2. Codex 루프: `node scripts/codex-review.mjs 62 --request --wait --status`(background) → FINDINGS면 spec AC·보안만 수정, 나머지 P2는 follow-up 이슈(area:A) 이관 후 `--resolve`
3. COMPLETE·CI 초록이면 `docs(F-16): 완료 표시 (spec DONE)` 커밋: spec 헤더 `DONE (PR #62)`, 11절 reviewer·CI 칸 체크 → 사용자가 GitHub에서 Squash and merge

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부
- 실제 LLM 품질은 사용자 요청 시 prompt-evaluator(RECOMMEND)

## 주의

- B F-12(찜) 진행 중: concept 패키지·`src/api/concepts.js` 공통 변경 예정 → F-16 ConceptMapper 블록과 충돌 가능, 머지 순서에 따라 `git merge origin/develop`
- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시)
