---
name: handoff
description: PC를 옮기거나 세션을 마칠 때 내 NOW 파일 갱신, 작업로그 기록, 커밋(테스트 통과 시 정식, 아니면 wip), push까지 한 번에 한다. "교대", "인수인계", "넘길게", "오늘 여기까지", "집에서 이어서", "토큰 떨어질 것 같아", "/handoff" 요청 시 사용.
---

# /handoff — 인수인계

다음 세션(내 다른 PC, 또는 드물게 상대)이 **이 대화를 전혀 보지 못한 채** 이어받는다고 가정하고 남긴다.
두 사람이 병렬로 일하므로 **내 NOW 파일**(`node scripts/stamp.mjs --file` → `docs/handoff/NOW-<dh|js>.md`)만 고친다. 상대 NOW 파일은 건드리지 않는다.
토큰이 부족해 보이면 1~3단계만 최소로 하고 4단계는 `node scripts/handoff.mjs`로 대신한다.

## 절차

1. **내 NOW 파일 덮어쓰기** (기존 섹션 구조 유지, 30줄 이내)
   - `갱신:` 줄은 `node scripts/stamp.mjs --now` 출력을 그대로 쓴다. 시각·PC를 짐작해 쓰지 않는다 (다음 세션 hook이 이 값으로 PC 전환을 감지한다). PC가 미등록이면 사용자에게 `node scripts/setup.mjs --pc <id>`를 요청한다.
   - `## 현재 기능`: 기능 ID, 이름, spec 경로 (첫 줄은 상대 세션에 요약으로 보이므로 한 줄로 상태까지)
   - `## 진행`: AC별 ✅ / 🟡(상태 설명) / ⬜
   - `## 바로 다음 할 일`: 번호 목록. 파일 경로, 메서드, 테스트 이름까지. 첫 항목은 30분 안에 끝낼 수 있는 크기로.
   - `## 막힌 점 / 결정 필요`: 시도한 것과 결과 포함. 사람 결정이 필요한 것은 따로 표시.
   - `## 주의`: 새 마이그레이션(번호), 바뀐 환경변수, 새 의존성, 일부러 깨 둔 테스트, 상대에게 알려야 할 공통 코드 변경 등
2. **spec 체크리스트 갱신**: 현재 spec의 구현 체크리스트와 "결정 필요"를 실제 상태에 맞춘다.
3. **작업로그 추가**: `docs/작업로그.md` **맨 아래에** (헤더는 `node scripts/stamp.mjs --log` 출력. 파일은 `merge=union`이라 상대 항목과 충돌하지 않는다)

```
## YYYY-MM-DD HH:mm · 작업자 · PC id · 브랜치
- 한 일: (이번 세션에서 한 일, 커밋 단위로 2~5줄)
- 다음 할 일: NOW 파일 참고 (핵심 한 줄)
- 막힌 점:
- 메모: (다음 세션이 알아야 할 맥락. 왜 이렇게 했는지)
- AI 활용: (선택. 보고서에 쓸 만한 사례 한 줄. 예: "guard가 커밋된 V2 수정을 막아 새 V3로 처리", "reviewer가 Zip Slip 누락 발견")
```

   - `AI 활용` 줄은 기록할 만한 사례가 있을 때만 쓴다. 최종 산출물 "외부 API·생성형 AI 활용" 문서의 소재가 된다.
   - `AI 활용` 줄을 썼고 Notion MCP(`notion-create-pages`)를 쓸 수 있으면 `docs/notion.md`의 "AI 활용·트러블슈팅 사례" DB에 행을 하나 추가한다(제목 한 줄, 날짜, 분류, 기능, 브랜치·PR, 내용). 없으면 건너뛴다. 정본은 작업로그다.
   - 막힌 점 중 이번 기능 밖으로 미루는 후속 작업은 `gh issue create --label follow-up --label area:<A|B|common> --assignee @me`로 등록하고 NOW 파일에는 이슈 번호만 적는다. 영역 라벨은 내 영역이면 `node scripts/stamp.mjs --area` 출력(김동환 `area:A`, 김준섭 `area:B`), 하네스·공통 코드면 `area:common`이다. 본문은 `.github/ISSUE_TEMPLATE/follow-up.md` 형식.

4. **커밋과 push**
   - 보호 브랜치(main/develop)면 멈추고 사용자에게 알린다.
   - `node scripts/verify.mjs`(또는 verifier 에이전트)로 변경 영역 검증.
     - 통과: 남은 변경을 의미 단위 커밋 (`feat(F-xx): ...` 등).
     - 실패 또는 작업 중간: `wip(F-xx): <어디까지 했는지 한 줄>` 로 커밋. wip는 테스트 게이트 예외다.
   - `git add`는 경로를 지정한다. `.env`·키 파일이 섞이지 않았는지 `git status`로 확인.
   - `git push -u origin <브랜치>` (`origin` = GitHub. `gitlab` 원격은 제출 전용이라 push하지 않는다). 원격이 없거나 실패하면 그 사실을 보고한다 (커밋은 로컬에 남는다).
   - 이 브랜치에 열린 PR이 있으면 `gh pr view --json url,statusCheckRollup`으로 CI 상태를 확인해 NOW 파일 주의 섹션에 적는다.
5. **보고**: 커밋 해시, push 여부, 다음 PC에서 할 첫 행동(`git switch <브랜치> && git pull` → `/pickup`)을 3줄로.

## 주의

- 일부러 깨 둔(red) 테스트가 있으면 NOW 파일 주의 섹션에 테스트 이름을 적는다.
- 이 스킬은 코드를 새로 작성하지 않는다. 남은 구현은 다음 할 일로 넘긴다.
- 보고 끝에 "다음 세션은 `/clear`(또는 새 터미널) 후 `/pickup`으로 시작"을 안내한다. 같은 대화를 이어 가면 지난 컨텍스트를 턴마다 다시 읽는다.
