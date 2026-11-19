---
name: pickup
description: 새 세션이나 PC 교대 후 내 작업을 이어받는다. git pull, 내 NOW 파일·작업로그·현재 spec 확인, 상대 작업 요약, 테스트 상태 점검 후 다음 할 일을 제안한다. "이어받기", "이어서 하자", "어디까지 했어", "/pickup" 요청 시 또는 새 세션을 시작할 때 사용.
---

# /pickup — 이어받기

두 사람이 영역별로 **병렬** 개발한다 (`docs/기획서.md` 4절). 이 스킬은 **내 작업**(내 NOW 파일 `docs/handoff/NOW-<dh|js>.md`)을 내 다른 PC에서 넘긴(또는 토큰 소진으로 끊긴) 상태에서 **빠르게, 정확하게** 복원한다.
SessionStart hook이 내 NOW 파일, 상대 작업 요약, git 상태를 이미 주입했으므로 그 내용을 다시 출력하지 않는다. 사용자는 hook의 화면 배너(systemMessage: 사람·PC·브랜치·환경·주의·내/상대 기능 한 줄씩)만 보고 주입된 본문은 보지 못한다. 내 NOW 파일 경로는 `node scripts/stamp.mjs --file`.

## 절차

0. **PC·환경 확인** (SessionStart hook의 "PC·환경" 섹션과 ⚠️ 주의)
   - PC가 미등록이면 사용자에게 `node scripts/setup.mjs --pc <id>`를 요청하고 기다린다 (내 NOW 파일을 정할 수 없다).
   - ❌ 항목이 있으면 작업 전에 사용자와 해결한다. ⚠️는 보고에 한 줄로 적는다 (예: Docker 꺼짐 → BE 테스트 불가).
   - "직전 작업은 다른 PC" 경고가 있으면 아래 pull을 반드시 하고, 3단계 검증을 건너뛰지 않는다. 그 PC의 주의 사항(pcs.json `notes`)도 보고에 반영한다.
1. **최신화**
   - 미커밋 변경이 있으면 먼저 사용자에게 알린다 (내 PC에서 하던 작업일 수 있음). 임의로 stash/삭제하지 않는다.
   - `origin`(GitHub)이 있으면 `git pull --ff-only`. 충돌·실패 시 멈추고 상황을 보고한다.
   - `gh pr status`로 내 브랜치의 PR, CI 실패를 확인한다 (gh가 없거나 원격이 없으면 건너뜀). 상대가 나를 @멘션한 PR(공통 코드 변경)이 있으면 보고에 포함한다.
   - 내 NOW 파일의 PR이 있으면 `node scripts/codex-review.mjs <번호>`(대기 없음)로 Codex 상태를 확인한다. `STALE`이나 `FINDINGS`이면 `/pr-review` 8단계 루프로 이어 간다. `RUNNING`이면 `--wait --status`를 백그라운드로 다시 걸어 결과를 받는다. `ERROR Codex 계정 미연결`이면 `docs/환경설정.md` 8절을 사용자에게 안내한다. `CLOSED`(이미 머지·종료)면 루프를 돌지 않는다.
   - 현재 브랜치가 main/develop이면 **내 NOW 파일의 브랜치**로 `git switch` 한다 (없으면 사용자에게 확인). **상대 NOW 파일의 브랜치로는 switch하지 않는다.**
   - **내 NOW 파일의 PR이 머지되었으면** (`gh pr view <번호> --json state,body`가 `MERGED`) `git switch develop && git pull` 후 다음 기능을 시작한다. PR 본문의 `Closes #n` 이슈가 아직 열려 있으면 `close-issues` 워크플로가 실패한 것이니 보고에 적는다. spec DONE은 `/pr-review` 10단계에서 PR 안에 이미 반영했다. WBS·간트·진행현황은 주간 `/progress`가 맞춘다 (PR에서 고치지 않는다, 11-19 결정).
   - SessionStart가 "자동 동기화"로 develop·내 원격 브랜치를 이미 합쳤으면 결과만 짧게 알린다 (⏸️ 미룸·⚠️ 충돌이면 커밋 후 `git merge origin/develop` 또는 충돌 해결을 제안한다). 세션을 main·develop에서 시작해 내 feature 브랜치로 `git switch`했으면 자동 동기화는 다음 확인(10분)까지 돌지 않으므로 바로 `git merge origin/develop`을 실행한다.
   - SessionStart가 "develop에 이 브랜치에 없는 커밋"을 알렸고 새 마이그레이션·openapi.json 변경이 있으면, 마이그레이션·API를 추가하기 전에 `git merge origin/develop`을 제안한다.
2. **상태 읽기**
   - 내 NOW 파일 (pull 후 바뀌었을 수 있으니 다시 읽는다). 상대 NOW 파일은 hook 요약 두 줄로 충분하다.
   - `docs/작업로그.md`의 **내 마지막 항목**(hook이 주입)
   - 내 NOW 파일의 현재 기능 spec(`docs/specs/F-xx-*.md`)에서 수용 기준 표, 결정 필요, 구현 체크리스트
   - 열린 follow-up 이슈 (hook이 "열린 follow-up 이슈" 섹션에 주입. 없으면 이슈 없음이거나 gh 실패). 분류 필요(area 라벨 없음)가 있으면 라벨·담당자 안을 보여 주고 사용자 확인 뒤 `gh issue edit <n> --add-label area:<..> --add-assignee <영역 오너>`로 붙인다. 지금 기능과 관련된 내 이슈는 이번 작업에 흡수할지 묻는다.
   - 마지막 커밋이 `wip(...)` 이면 `git show --stat HEAD`로 무엇이 중간 상태인지 확인한다.
   - **NOW 파일 본문이 마지막 커밋보다 오래되었으면**(갱신 줄에 `자동 handoff, 본문 미갱신`, 갑작스러운 종료) `git diff HEAD~3 --stat` 등으로 실제 진행을 추정하고, NOW 파일을 믿지 말아야 할 부분을 표시한다.
3. **검증 상태**
   - `verifier` 에이전트로 현재 기능 영역의 테스트를 돌린다 (backend/frontend 미생성이면 건너뜀).
   - 실패 테스트가 "의도된 red(TDD 진행 중)"인지 "깨진 것"인지 spec 체크리스트와 대조해 구분한다.
4. **보고** (아래 형식, 11줄 이내)

```
📍 김동환 @dh-ssafy · F-08 개념 추출 · feature/F-08-concepts · 마지막 작업: @dh-home (11/20 22:10, wip)
🖥️ 환경: ✅ (또는 ⚠️ Docker 꺼짐 → BE 테스트 불가)
✅ 완료: AC1, AC2
🟡 진행 중: AC3 — 테스트 작성됨(red), ConceptExtractor.link() 구현 필요
⛔ 막힌 점: 없음 / ⚠️ 주의: develop에 상대 V6 머지됨 → 내 마이그레이션은 V7
👥 상대: F-09 카드 목록 (feature/F-09-card-list, PR #31 열림)
🔀 PR: #30 F-08 — CI ✅, Codex FINDINGS 1건(P2)
📌 이슈: 내 4 · 공통 3 (7일 넘음 2: #16, #17) / 분류 필요 1 → 라벨 제안
➡️ 다음: ConceptExtractor.link()에서 기존 카드 매칭 → AC3 테스트 green
```

5. 사용자가 확인하면 바로 다음 할 일을 시작한다. 큰 방향 전환이 필요해 보이면 시작 전에 묻는다.

## 주의

- NOW 파일을 이 단계에서 수정하지 않는다 (작업을 실제로 진행한 뒤 갱신). **상대 NOW 파일은 어느 단계에서도 수정하지 않는다.**
- 상대 영역 기능을 대신 맡게 되면(사용자가 지시한 경우만) spec의 "결정 필요"를 특히 확인하고, 상대에게 알리도록 사용자에게 권한다.
- 다음 할 일이 `/spec`·`/implement`·리뷰 반영인데 현재 모델·effort가 CLAUDE.md "세션 운영" 표와 다르면 시작 전에 `/model`·`/effort` 변경을 권한다. 컨텍스트가 작은 지금 바꾸는 것이 가장 싸다.
- 머지 후 정리만 하고 끝났으면 NOW 파일 갱신 뒤 `/clear`를 권하고 다음 기능은 새 세션에서 시작한다.
