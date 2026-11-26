# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-26 11:42 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/common-harness-plan-1126

## 현재 기능

**하네스 동결 해제·로드맵·개선 계획 (기획서 7.7절)** — 인터뷰 끝, docs PR 생성 단계. PR #176(합의·운영) 머지됨. GitHub secret scanning·push protection 켬(11:40)

## 진행

- ✅ 결정: 동결 해제 · 로드맵 R-1(`docs/로드맵.md` 정본, 배너·`/pickup`·`/handoff` 표시, 상태는 PR 본문 `Roadmap: <ID>`로 계산, 넘겨주는 시점 D-2 알림) · HX-1~10 · 점검 추가 H-1~7·H-9~12(H-8·H-13~16 보류)
- ✅ CLAUDE.md: 동결 해제 문구, 진행 보고 규칙 · A 트랙 4(하네스) 11/26~29, PR 5개

## 바로 다음 할 일

1. 이 docs PR: 커밋 → push → `gh pr create --base develop` → CI 초록이면 머지. 보류 H-8·H-13~16은 follow-up 이슈(area:common)로 등록
2. 트랙 4 PR ① 보안·가드 (`feature/common-harness-guard-1126`): `.claude/settings.json` PreToolUse matcher에 MCP `start_process|interact_with_process|write_file|edit_block|move_file` 연결(H-1), `guard.mjs` checkout -f·switch --discard-changes·restore·stash drop/clear 차단(H-3)·grep/rg 패턴 오탐(H-4), `node -e`·`python -c` `.env` 읽기 차단 + guard.jsonl 원문 제거(HX-1·#135), `guard-files.mjs` github_pat_·웹훅·GMS 키 패턴 + 커밋 hook 비밀값 검사(H-2), eval 가드(HX-2), CODEOWNERS 주인 없는 파일 테스트(HX-3, 스크래치 `unowned.mjs` 로직). 테스트 먼저
3. 트랙 4 PR ② 로드맵 R-1: `docs/로드맵.md`(점검 문서 2절 표 + 넘겨주는 시점 + 7.5·7.6·7.7 반영) + `scripts/lib/roadmap.mjs` + SessionStart·`/pickup`·`/handoff` 표시 → ③ → ④ → ⑤ (기획서 7.7절)
4. 트랙 1 E-1: `PromptEvalRunner.java:98` stage 확장 + 채점기 (새 세션 Opus/high) · 11/27 Figma 뒤 인터뷰 5단계 UI

## 막힌 점 / 결정 필요
- 사람: dh-home git `user.name`을 `whans1996`로 통일(H-7, 일부 커밋 작성자가 `whans1996@naver.com`), 노션 팀원 초대, F-05b liveTest

## 주의
- 다음 마이그레이션은 V14 · 브랜치 보호 관리자 포함 · node 스크립트로 Java 편집 시 백슬래시 깨짐 → Edit 도구 · 이 PC에 python 없음
