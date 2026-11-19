---
name: pr-review
description: develop에 합치기 전 현재 feature 브랜치를 verifier·reviewer·contract-checker로 점검하고, 지적 사항을 반영한 뒤 GitHub PR 설명을 만들고 (사용자 확인 후) gh로 PR을 연다. "PR 올리자", "머지 전 리뷰", "/pr-review" 요청 시 사용.
---

# /pr-review — PR 전 리뷰와 PR 생성

개발 저장소는 GitHub(`origin`)이다. GitLab(lab.ssafy.com)은 최종 제출 전용이며 이 스킬에서 다루지 않는다.

## 절차

1. **범위 파악**: `git fetch` 후 `git diff --stat origin/develop...HEAD` (원격이 없으면 `develop...HEAD`). 브랜치 이름에서 F-xx를 뽑아 spec을 찾는다.
   - develop에 이 브랜치에 없는 커밋이 있으면(`git rev-list --count HEAD..origin/develop`) 먼저 `git merge origin/develop`으로 합친다 (rebase·force push 금지). openapi.json이 충돌하면 한쪽을 고른 뒤 BE 테스트로 다시 생성하고, 마이그레이션 번호는 `node scripts/check-migrations.mjs`로 확인한다.
2. **병렬 점검** — 세 에이전트를 한 번에 실행한다:
   - `verifier`: 전체(`all`) 검증
   - `reviewer`: diff + spec 기준 리뷰
   - `contract-checker`: 이 기능의 API 계약 일치 여부 (API 변경이 없으면 생략)
3. **분류**: 결과를 `반드시 수정`(spec 미충족, 보안, 테스트 실패, 계약 불일치, openapi.json 미갱신) / `권장` / `무시 가능`으로 나눠 사용자에게 보여 준다.
4. **수정**: `반드시 수정`은 바로 고치고 커밋 (`fix(F-xx): ...`). `권장`은 사용자에게 할지 묻는다.
   - 이번 PR에서 하지 않기로 한 지적과 spec 범위 밖 후속 작업은 GitHub 이슈로 남긴다: `gh issue create --label follow-up --title "<F-xx> 요약" --body "<배경, 출처 PR·지적 링크, 제안>"`. PR 설명 "후속 작업"과 내 NOW 파일에는 이슈 번호만 적는다 (`docs/conventions.md` 7절).
5. **PR 설명 생성**: `.github/pull_request_template.md` 형식으로 채워 `.harness/pr-F-xx.md`에 저장하고 요약을 보여 준다.
   - 수용 기준 표: AC별 테스트 이름과 결과
   - FE 변경이 있으면 스크린샷을 **직접 첨부한다** (사람에게 넘기지 않는다):
     1. ui-verifier가 남긴 `.harness/screens/*.png` 중 이 기능 화면을 `docs/산출물/screens/SC-xx-이름-{desktop,mobile}.png`로 복사하고, `06-화면설계서.md`의 해당 SC 절에 `<img>`로 넣어 커밋한다 (산출물 준비를 겸한다)
     2. push한 뒤 `node scripts/pr-screens.mjs SC-xx …`가 출력한 Markdown(커밋 sha 링크)을 PR 설명의 `## 스크린샷`에 붙인다. 이미 PR이 있으면 `gh pr edit --body-file`로 갱신한다
   - 리뷰어(Codex·reviewer 에이전트)가 특히 볼 곳, 산출물 영향
   - 영역 오너(머지) 칸에 내 GitHub 계정. `node scripts/owners.mjs --diff origin/develop`이 `.github/CODEOWNERS` 기준으로 상대 영역·공통 파일을 뽑아 주면, "공통 코드 변경"에 그 파일과 이유를 적고 상대 계정을 @멘션한다 (A `@whans1996`, B `@JoonseobKim`). CODEOWNERS 때문에 GitHub이 상대에게 리뷰를 자동 요청하지만 승인은 기다리지 않는다
6. **PR 생성 (사용자 확인 후)**
   - `git push -u origin <브랜치>`
   - 사용자가 확인하면: `gh pr create --base develop --title "<타입(F-xx): 요약>" --body-file .harness/pr-F-xx.md`
     (사람 리뷰어는 지정하지 않는다. 리뷰는 reviewer 에이전트(2단계)와 Codex 봇(8단계)이 한다 — 11-18 결정)
   - 이미 PR이 있으면 `gh pr edit --body-file ...`로 설명만 갱신한다.
7. **CI 확인**: `gh pr checks --watch` 대신 `gh pr checks`로 현재 상태를 한 번 확인하고 결과를 보고한다. 실패하면 `gh run view --log-failed`의 핵심 부분만 요약한다.
   - PR 이벤트로 CI가 안 돌면(실행 목록에 `pull_request`가 없음) Actions 탭 → CI → `···` Disable/Enable workflow 후 PR close/reopen을 사람에게 요청한다 (11-18 이 방법으로 해결). 임시로는 `gh workflow run CI --ref <브랜치>`.
8. **Codex 리뷰 루프 (매 PR 필수, head 커밋이 `COMPLETE`가 될 때까지)**: PR을 열면 `chatgpt-codex-connector` 봇이 리뷰한다. 자동 리뷰는 PR 생성 때만 돌기 때문에, 수정을 push한 뒤에는 Claude가 직접 재리뷰를 요청한다. 사용자 확인은 받지 않는다 (11-18 사용자 지시).
   1. push할 때마다 `node scripts/codex-review.mjs <번호> --request --wait`를 **Bash `run_in_background`**로 실행하고 완료 알림을 기다린다. 직접 폴링하지 않는다. 스크립트는 Codex 요약 댓글의 `codex-review-state` 상태를 읽어 판정한다. 같은 head 커밋에 대한 "Didn't find any major issues" 영수증 댓글도 COMPLETE로 인정한다. 마지막 리뷰가 지적 없이 끝난 커밋 이후 `docs/` 아래(`docs/api/openapi.json` 제외)만 바뀌었으면 재리뷰를 요청하지 않고 COMPLETE로 인정한다 (NOW 파일·작업로그 커밋 때문에 루프가 다시 돌지 않게). develop 합치기로 들어온 상대 파일은 이 PR의 변경으로 보지 않는다. 상태가 이전 커밋 기준이면 head sha 마커를 붙인 `@codex review`를 커밋마다 한 번만 단다.
   2. `COMPLETE`(head 커밋, 지적 없음)이면 루프를 끝낸다. 내 NOW 파일에 `Codex ✅ <sha>`를 적는다.
   3. `FINDINGS`이면 출력된 지적(`[P1/P2] path:line 제목`과 링크)을 읽고 하나씩 판단한다.
      - 타당하면 실패하는 테스트부터 써서 `fix(F-xx): ... (Codex 리뷰)`로 커밋하고 push한 뒤 1로 돌아간다.
      - **같은 PR에서 고치는 것은 spec 수용 기준이나 보안 기준(`docs/conventions.md` 7절)을 깨는 지적만이다.** 그 밖의 P2(형식 변형, 드문 경계 조건, 개선 제안)는 4단계처럼 `follow-up` 이슈로 넘기고 그 지적 스레드에 이슈 번호를 답한다(`gh api repos/{owner}/{repo}/pulls/<번호>/comments/<댓글 id>/replies -f body="#<이슈> 로 이관"`). P1은 이관하지 않는다.
      - **이관 완료**: head 커밋의 남은 지적이 모두 이렇게 이슈로 넘어갔으면 `node scripts/codex-review.mjs <번호> --resolve`를 실행한다. PR에 이관 마커 댓글을 달고 COMPLETE로 끝나며, 이후 `docs/` 아래만 바꾸는 커밋(10단계 완료 표시 등)은 이 커밋을 기준으로 재리뷰 없이 COMPLETE가 된다. 내 NOW 파일과 PR 설명 "후속 작업"에 `Codex ✅ <sha> (P2 n건 → #이슈)`를 적는다. 코드가 바뀌면 1로 돌아간다.
      - 타당하지 않거나 spec 범위·설계 판단이 필요하면 **루프를 멈추고** 이유를 사용자에게 보고한다. 같은 지적이 되풀이되지 않게 하기 위해서다.
      - **같은 주제**(예: 설정 마스킹)로 재리뷰를 2회 했는데 또 지적이 나오면 멈추고 사람이 정한다(이슈로 넘길지, 설계를 바꿀지). PR #14에서 형식 변형을 쫓다 한도를 소진한 데서 나온 규칙이다.
   4. 재리뷰가 **3회**를 넘거나 `TIMEOUT`이면 멈추고 현재 상태를 보고한다. `ERROR Codex 환경 또는 계정 연결 없음`이면 사람에게 출력된 링크에서 Codex 환경을 만들거나 GitHub 계정을 Codex에 연결해 달라고 요청하고, 준비되면 1을 다시 실행한다 (11-18 PR #7 환경, 11-19 PR #30 계정 미연결에서 발생).
9. **머지는 하지 않는다.** `gh pr merge`는 하네스가 차단한다. CI가 초록이면 영역 오너(= 이 PR 작성자)가 GitHub에서 머지한다 (wip 커밋이 있으면 Squash and merge). PR을 연 뒤 상대 PR이 develop에 먼저 머지됐으면, 머지 전에 `git merge origin/develop` → push로 CI를 다시 돌려 초록을 확인한다 (마이그레이션 번호·openapi.json은 머지 뒤에야 충돌이 드러난다).
   - 머지 뒤 PR 본문의 `Closes #n` 이슈가 열려 있으면(develop은 기본 브랜치가 아니라 자동으로 닫히지 않는다) `gh issue close <n> --comment "PR #<번호>로 해결"`을 사용자에게 제안한다.
10. **완료 표시는 PR 안에서 한다 (정리 PR 없음)**: Codex가 COMPLETE(또는 8-3 이관 완료)이고 CI가 초록이 되면, 머지 전 마지막 커밋 `docs(F-xx): 완료 표시 (spec DONE)`에 **내 spec만** 넣는다. `docs/` 아래만 바뀌므로 Codex 재리뷰는 돌지 않는다.
    - spec 헤더 `DONE (PR #n)`, 구현 체크리스트 마지막 칸
    - `docs/산출물/04-WBS.md`·`05-간트차트.md`·`docs/진행현황.md`는 **고치지 않는다.** 두 PR이 같은 표를 고치면 충돌하므로 주간 `/progress` docs PR이 develop 기준으로 한 번에 맞춘다 (11-19 결정).
11. PR 생성과 CI·Codex 확인 결과를 내 NOW 파일에 남기고 사용자에게 `/clear`를 권한다. Codex 지적 반영은 새 세션에서 `/pickup`으로 시작한다 (CLAUDE.md "세션 운영").
