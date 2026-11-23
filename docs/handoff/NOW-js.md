# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-23 16:28 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-20a-team

---

## 현재 기능

**F-20a 팀** (P2) `docs/specs/F-20a-team.md` APPROVED. **PR #83 열림** (head b41b717) → CI·Codex 루프 진행 중

## 진행

- ✅ BE AC1~AC11 (0fa7c27), FE AC12~AC15 (bea4a16), 산출물 (a7b3d4b)
- ✅ /pr-review: verify all 통과, contract-checker 불일치 0, reviewer 반드시 수정 0 / 권장 3건 반영 (b41b717: 초대 코드 붙여넣기 공백 제거·maxlength 제거, 재시도 정확히 4번 검증, join 읽기 시점 전제 Javadoc. COUNT FOR SHARE는 탈퇴 deleteMembershipsOf와 교착 우려로 안 씀)
- ✅ PR #83 생성 (본문 `.harness/pr-F-20a.md`, 공통 파일 11개 → @whans1996 멘션, 스크린샷 6장)
- ⏳ CI 확인, Codex 리뷰 대기 (`node scripts/codex-review.mjs 83 --request --wait --status`)

## 바로 다음 할 일

1. Codex 판정 확인: FINDINGS면 지적별 판단(AC·보안만 이 PR에서, 그 밖 P2는 follow-up 이관 후 `--resolve`), COMPLETE면 NOW에 `Codex ✅ <sha>`
2. CI 초록 + Codex COMPLETE → `docs(F-20a): 완료 표시 (spec DONE)` 커밋(spec 헤더 `DONE (PR #83)`·체크리스트) → 사람이 GitHub에서 머지
3. 머지 후 F-20b `/spec` (A 영역 퀴즈·분석 권한 변경 필요 → 상대와 협의)

## 막힌 점 / 결정 필요

- 없음

## 주의

- 상대 PR(F-05b, ErrorCode·application.yml 변경)이 먼저 머지되면 머지 전 `git merge origin/develop` → push로 CI 재확인 (ErrorCode 블록 충돌 가능)
- 로컬 8080(옛 빌드 BE)·5173은 이전 세션이 끄지 못함. 로컬 DB에 점검용 계정 `ui-team-owner@`·`ui-team-member@example.test`(비번 vibe1234)·`ui-admin@`, 초안 카드 id 171 남아 있음
