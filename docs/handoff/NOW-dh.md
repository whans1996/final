# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-25 10:36 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/common-replan-1125

## 현재 기능

**WBS·간트 2차 재계획** (docs PR): 2차 기획(PR #127 머지)을 WBS 4절(V-1~V-5·F-29로 재조정)과 새 6절(H-1·F-26~F-33 등 6.1~6.15), 간트, 진행현황, 기획서 4절 주석·6절 동결 문구, 결정로그 1행에 반영했다. M4 = 12/14 T0·T1 기능 동결, T2는 W5에 티어 순서대로 하다가 12/20에 멈춘다(사용자 결정).

## 진행

- ✅ 04-WBS·05-간트·진행현황·기획서·결정로그 편집
- 🟡 커밋·push → PR `docs: WBS·간트 2차 재계획 (W2)` → CI 초록이면 내가 머지 (docs 예외)

## 바로 다음 할 일

1. 재계획 PR 머지 → `/clear` → 새 세션(Opus/high)
2. **H-1 하네스 개편 (WBS 6.1, 11/26~28)**: `git switch -c feature/common-harness-revamp origin/develop`
   - `.githooks/commit-msg.mjs`: 빠른 검사(포맷·린트·관련 단위 테스트)만 하고 pre-push hook을 추가한다. Gradle은 잠금 파일로 직렬화한다.
   - `.claude/hooks/guard-bash.mjs`: 셸 토큰 파서로 바꾼다. `scripts/stamp.mjs`: NOW·작업로그 자동 초안. `scripts/doctor.mjs`를 새로 만든다. 허용 목록은 fewer-permission-prompts 스킬로 정한다. #29도 함께 처리한다.
3. V-1 MVP 감사 (WBS 4.4, 11/29~12/02, A·B): #73·#94·#91 즉시 처리
4. 11/27(목) Figma 공동 세션 → 12/01 F-27 GMS `/spec` (GMS 키가 필요하다)

## 막힌 점 / 결정 필요

- 사람: GMS 키 수령(12/01 전), 노션 팀원 초대, F-05b liveTest, 제출 zip 이름(지역·반)

## 주의

- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20) · 하네스는 H-1 동안만 동결을 해제하고, 끝나면 다시 동결한다
- Codex: 3회차부터 P2는 follow-up 이관 (11-25 결정) · 진행현황·WBS·간트는 `/progress` docs PR에서만
- 이 PC의 Git Bash heredoc에서 python `'''`가 깨진다 → 편집은 Edit 도구나 Write로 만든 스크립트 파일로
