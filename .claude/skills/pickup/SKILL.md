---
name: pickup
description: 릴레이 교대 후 작업을 이어받는다. git pull, NOW.md·작업로그·현재 spec 확인, 테스트 상태 점검 후 다음 할 일을 제안한다. "이어받기", "이어서 하자", "어디까지 했어", "/pickup" 요청 시 또는 새 세션을 시작할 때 사용.
---

# /pickup — 이어받기

상대가 작업하다 넘긴(또는 토큰 소진으로 끊긴) 상태를 **빠르게, 정확하게** 복원한다.
SessionStart hook이 NOW.md와 git 상태를 이미 주입했으므로 그 내용을 다시 출력하지 않는다.

## 절차

0. **PC·환경 확인** (SessionStart hook의 "PC·환경" 섹션과 ⚠️ 주의)
   - PC가 미등록이면 사용자에게 `node scripts/setup.mjs --pc <id>`를 요청하고 기다린다 (인계 기록에 PC를 남길 수 없다).
   - ❌ 항목이 있으면 작업 전에 사용자와 해결한다. ⚠️는 보고에 한 줄로 적는다 (예: Docker 꺼짐 → BE 테스트 불가).
   - "직전 작업은 다른 PC" 경고가 있으면 아래 pull을 반드시 하고, 3단계 검증을 건너뛰지 않는다. 그 PC의 주의 사항(pcs.json `notes`)도 보고에 반영한다.
1. **최신화**
   - 미커밋 변경이 있으면 먼저 사용자에게 알린다 (내 PC에서 하던 작업일 수 있음). 임의로 stash/삭제하지 않는다.
   - `origin`(GitHub)이 있으면 `git pull --ff-only`. 충돌·실패 시 멈추고 상황을 보고한다.
   - `gh pr status`로 내 브랜치의 PR, 리뷰 요청, CI 실패를 확인한다 (gh가 없거나 원격이 없으면 건너뜀). 상대가 리뷰를 요청한 PR이 있으면 보고에 포함한다. NOW.md의 PR이 열려 있으면 `node scripts/codex-review.mjs <번호>`(대기 없음)로 Codex 상태도 확인한다. `STALE`이나 `FINDINGS`이면 `/pr-review` 8단계 루프로 이어 간다.
   - 현재 브랜치가 main/develop이면 NOW.md의 브랜치로 `git switch` 한다 (없으면 사용자에게 확인).
   - **NOW.md의 PR이 머지되었으면** (`gh pr view <번호> --json state`가 `MERGED`) `git switch develop && git pull` 후 다음 기능을 시작한다. spec DONE·WBS 실적·간트는 `/pr-review` 10단계에서 PR 안에 이미 반영했으므로 **정리 PR을 따로 만들지 않는다** (11-18 결정). 실제 머지 날짜가 PR에 적은 예정일과 다르거나 빠진 항목이 있으면 다음 기능 브랜치의 첫 커밋(`docs: ...`)에서 바로잡는다.
2. **상태 읽기**
   - `docs/handoff/NOW.md` (pull 후 바뀌었을 수 있으니 다시 읽는다)
   - `docs/작업로그.md`의 **마지막 항목만**
   - NOW.md의 현재 기능 spec(`docs/specs/F-xx-*.md`)에서 수용 기준 표, 결정 필요, 구현 체크리스트
   - 마지막 커밋이 `wip(...)` 이면 `git show --stat HEAD`로 무엇이 중간 상태인지 확인한다.
   - **NOW.md가 마지막 커밋보다 오래되었으면**(자동 handoff, 갑작스러운 종료) `git diff HEAD~3 --stat` 등으로 실제 진행을 추정하고, NOW.md를 믿지 말아야 할 부분을 표시한다.
3. **검증 상태**
   - `verifier` 에이전트로 현재 기능 영역의 테스트를 돌린다 (backend/frontend 미생성이면 건너뜀).
   - 실패 테스트가 "의도된 red(TDD 진행 중)"인지 "깨진 것"인지 spec 체크리스트와 대조해 구분한다.
4. **보고** (아래 형식, 10줄 이내)

```
📍 F-05 분석 요청 · feature/F-05-analysis-request · 마지막 작업: B @js-home (11/18 14:20, wip) → 지금 dh-ssafy
🖥️ 환경: ✅ (또는 ⚠️ Docker 꺼짐 → BE 테스트 불가)
✅ 완료: AC1, AC2
🟡 진행 중: AC3 — 테스트 작성됨(red), AnalysisService.validateZip() 구현 필요
⛔ 막힌 점: 없음 / ⚠️ 주의: V3 마이그레이션 새로 추가됨 → 로컬 DB 자동 적용
🔀 PR: #12 F-04 팔로우 — CI ✅, Codex FINDINGS 1건(P2), 내 리뷰 요청됨
➡️ 다음: validateZip()에서 200파일 제한 구현 → AC3 테스트 green
```

5. 사용자가 확인하면 바로 다음 할 일을 시작한다. 큰 방향 전환이 필요해 보이면 시작 전에 묻는다.

## 주의

- NOW.md를 이 단계에서 수정하지 않는다 (작업을 실제로 진행한 뒤 갱신).
- 상대 영역(영역 오너가 다른 기능)을 이어받을 때는 spec의 "결정 필요"를 특히 확인한다.
- 다음 할 일이 `/spec`·`/implement`·리뷰 반영인데 현재 모델·effort가 CLAUDE.md "세션 운영" 표와 다르면 시작 전에 `/model`·`/effort` 변경을 권한다. 컨텍스트가 작은 지금 바꾸는 것이 가장 싸다.
- 머지 후 정리만 하고 끝났으면 NOW.md 갱신 뒤 `/clear`를 권하고 다음 기능은 새 세션에서 시작한다.
