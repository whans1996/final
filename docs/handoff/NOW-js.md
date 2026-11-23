# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-23 16:33 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-20a-team

---

## 현재 기능

**F-20a 팀** (P2) `docs/specs/F-20a-team.md` **DONE (PR #83)**. CI 초록 + Codex ✅ → **사람이 GitHub에서 머지 대기**

## 진행

- ✅ BE AC1~AC11 (0fa7c27), FE AC12~AC15 (bea4a16), 산출물 (a7b3d4b)
- ✅ /pr-review: verify all 통과, contract-checker 불일치 0, reviewer 반드시 수정 0 / 권장 3건 반영 (b41b717)
- ✅ PR #83 CI 초록 (BE·FE·마이그레이션 번호·제목), Codex ✅ 08d83a5 (P2 1건 → #84, `--resolve`)
- ✅ 완료 표시 커밋 (spec DONE·체크리스트)

## 바로 다음 할 일

1. 사람이 PR #83을 GitHub에서 Merge (wip 없음 → 일반 머지 가능). 그전에 상대 PR #82(F-05b)가 먼저 머지되면 `git merge origin/develop` → push → CI 초록 재확인 (ErrorCode 블록·openapi.json 충돌 가능, openapi는 BE 테스트로 재생성)
2. 머지 후 `/clear` → `/pickup` → F-20b `/spec` (A 영역 퀴즈·분석 권한 변경 필요 → 상대와 협의). 기능 사이 시간에 #84(팀 상세 늦은 응답 덮어쓰기, area:B) 처리 가능

## 막힌 점 / 결정 필요

- 없음

## 주의

- 로컬 8080(옛 빌드 BE)·5173은 이전 세션이 끄지 못함. 로컬 DB에 점검용 계정 `ui-team-owner@`·`ui-team-member@example.test`(비번 vibe1234)·`ui-admin@`, 초안 카드 id 171 남아 있음
