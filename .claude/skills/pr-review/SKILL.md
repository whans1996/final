---
name: pr-review
description: develop에 합치기 전 현재 feature 브랜치를 verifier·reviewer·contract-checker로 점검하고, 지적 사항을 반영한 뒤 GitHub PR 설명을 만들고 (사용자 확인 후) gh로 PR을 연다. "PR 올리자", "머지 전 리뷰", "/pr-review" 요청 시 사용.
---

# /pr-review — PR 전 리뷰와 PR 생성

개발 저장소는 GitHub(`origin`)이다. GitLab(lab.ssafy.com)은 최종 제출 전용이며 이 스킬에서 다루지 않는다.

## 절차

1. **범위 파악**: `git fetch` 후 `git diff --stat origin/develop...HEAD` (원격이 없으면 `develop...HEAD`). 브랜치 이름에서 F-xx를 뽑아 spec을 찾는다.
2. **병렬 점검** — 세 에이전트를 한 번에 실행한다:
   - `verifier`: 전체(`all`) 검증
   - `reviewer`: diff + spec 기준 리뷰
   - `contract-checker`: 이 기능의 API 계약 일치 여부 (API 변경이 없으면 생략)
3. **분류**: 결과를 `반드시 수정`(spec 미충족, 보안, 테스트 실패, 계약 불일치, openapi.json 미갱신) / `권장` / `무시 가능`으로 나눠 사용자에게 보여 준다.
4. **수정**: `반드시 수정`은 바로 고치고 커밋 (`fix(F-xx): ...`). `권장`은 사용자에게 할지 묻는다.
5. **PR 설명 생성**: `.github/pull_request_template.md` 형식으로 채워 `.harness/pr-F-xx.md`에 저장하고 요약을 보여 준다.
   - 수용 기준 표: AC별 테스트 이름과 결과
   - FE 변경이 있으면 스크린샷을 **직접 첨부한다** (사람에게 넘기지 않는다):
     1. ui-verifier가 남긴 `.harness/screens/*.png` 중 이 기능 화면을 `docs/산출물/screens/SC-xx-이름-{desktop,mobile}.png`로 복사하고, `06-화면설계서.md`의 해당 SC 절에 `<img>`로 넣어 커밋한다 (산출물 준비를 겸한다)
     2. push한 뒤 `node scripts/pr-screens.mjs SC-xx …`가 출력한 Markdown(커밋 sha 링크)을 PR 설명의 `## 스크린샷`에 붙인다. 이미 PR이 있으면 `gh pr edit --body-file`로 갱신한다
   - 리뷰어(영역 오너)가 특히 볼 곳, 산출물 영향
6. **PR 생성 (사용자 확인 후)**
   - `git push -u origin <브랜치>`
   - 사용자가 확인하면: `gh pr create --base develop --title "<타입(F-xx): 요약>" --body-file .harness/pr-F-xx.md`
     (리뷰어를 지정하려면 사용자에게 GitHub 아이디를 묻고 `--reviewer`를 붙인다)
   - 이미 PR이 있으면 `gh pr edit --body-file ...`로 설명만 갱신한다.
7. **CI 확인**: `gh pr checks --watch` 대신 `gh pr checks`로 현재 상태를 한 번 확인하고 결과를 보고한다. 실패하면 `gh run view --log-failed`의 핵심 부분만 요약한다.
   - PR 이벤트로 CI가 안 돌면(실행 목록에 `pull_request`가 없음) Actions 탭 → CI → `···` Disable/Enable workflow 후 PR close/reopen을 사람에게 요청한다 (11-18 이 방법으로 해결). 임시로는 `gh workflow run CI --ref <브랜치>`.
8. **Codex 리뷰 루프 (매 PR 필수, head 커밋이 `COMPLETE`가 될 때까지)**: PR을 열면 `chatgpt-codex-connector` 봇이 리뷰한다. 자동 리뷰는 PR 생성 때만 돌기 때문에, 수정을 push한 뒤에는 Claude가 직접 재리뷰를 요청한다. 사용자 확인은 받지 않는다 (11-18 사용자 지시).
   1. push할 때마다 `node scripts/codex-review.mjs <번호> --request --wait`를 **Bash `run_in_background`**로 실행하고 완료 알림을 기다린다. 직접 폴링하지 않는다. 스크립트는 Codex 요약 댓글의 `codex-review-state` 상태를 읽어 판정한다. 같은 head 커밋에 대한 "Didn't find any major issues" 영수증 댓글도 COMPLETE로 인정한다. 상태가 이전 커밋 기준이면 head sha 마커를 붙인 `@codex review`를 커밋마다 한 번만 단다.
   2. `COMPLETE`(head 커밋, 지적 없음)이면 루프를 끝낸다. NOW.md에 `Codex ✅ <sha>`를 적는다.
   3. `FINDINGS`이면 출력된 지적(`[P1/P2] path:line 제목`과 링크)을 읽고 하나씩 판단한다.
      - 타당하면 실패하는 테스트부터 써서 `fix(F-xx): ... (Codex 리뷰)`로 커밋하고 push한 뒤 1로 돌아간다.
      - 타당하지 않거나 spec 범위·설계 판단이 필요하면 **루프를 멈추고** 이유를 사용자에게 보고한다. 같은 지적이 되풀이되지 않게 하기 위해서다.
   4. 재리뷰가 **3회**를 넘거나 `TIMEOUT`이면 멈추고 현재 상태를 보고한다.
9. **머지는 하지 않는다.** `gh pr merge`는 하네스가 차단한다. CI가 초록이면 영역 오너가 GitHub에서 머지한다 (wip 커밋이 있으면 Squash and merge).
10. spec 상태는 DONE으로 바꾸지 않는다 (머지 후 `/progress`에서 확인).
11. PR 생성과 CI·Codex 확인 결과를 NOW.md에 남기고 사용자에게 `/clear`를 권한다. Codex 지적 반영이나 머지 후 정리는 새 세션에서 `/pickup`으로 시작한다 (CLAUDE.md "세션 운영").
