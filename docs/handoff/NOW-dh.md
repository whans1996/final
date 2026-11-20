# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-20 16:02 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/issue-67-issue-priority

## 현재 기능

하네스 PR `chore(harness)` — 이슈 #67 (+ #48·#39 Closes). 1·2단계 완료. 기준 = #67 본문 "제안" (실패 테스트부터)

## 진행

- ✅ #63 PR #66 머지 (dh-home FE 정식 커밋 가능). 하네스 점검 결과·사용자 결정(blocker는 "멈추고 묻기", 로컬 develop·main FF + 머지된 브랜치 정리) → #67
- ✅ 1단계: `followUpIssues`→`{ok,issues,reason}`+`issueFailureLine`, session-start는 autoSync 뒤에 gh·docker 시작, env-check `run`에 `timedOut` (테스트 issues·env-check, 하네스 62개 green)
- ✅ 2단계: `blocker` 라벨 생성(GitHub), `summarizeIssues().blockers`(영역 무관·다른 그룹에서 뺌), 배너 🔴, pickup·spec "멈추고 묻기", pr-review·handoff·conventions 7절·이슈 템플릿·결정로그

## 바로 다음 할 일 (Opus/high 새 세션)

1. `codex-review.mjs` `judge()`(185-243) → `scripts/lib/codex.mjs`: `failed` 재요청(같은 head 2회 ERROR), running 시작 시각(`<relative-time datetime=…>`) 15분 초과 = 정체 재요청, docs만 바뀌면 대기 중 요청 커밋도 기준(#39). `--wait`도 failed에서 탈출. fixture 테스트
2. `sync.mjs` `autoSync`: 체크아웃 안 된 로컬 develop·main을 `git fetch . refs/remotes/origin/<b>:refs/heads/<b>`로 FF (순수 함수 + `sync.test.mjs`). fetch에 `--prune` (`session-start.mjs`·`peer-watch.mjs`·`next-migration.mjs`의 `git fetch`)
3. 머지 조건 문구 통일(CLAUDE.md:36·66, pr-review 9단계, PR 템플릿 = CI + Codex), 죽은 포인터(`docs/작업로그.md:5`, `backend/CLAUDE.md:5`)
4. 사람: 저장소 "머지 후 브랜치 자동 삭제" 켜기 (`! gh repo edit --delete-branch-on-merge`, guard가 Claude 실행을 막음). 머지된 원격 브랜치 43개는 목록 확인 후 `git push origin --delete`
5. 그 뒤 #54 → #55·#17 → F-05b `/spec`

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부, 하네스 동결 원칙(막거나 거짓 정보 주는 버그만) 결정로그 추가 여부

## 주의

- 공통(하네스) PR이라 @JoonseobKim 멘션, 원격 브랜치 정리로 B 브랜치도 지워짐을 알린다
- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시)
