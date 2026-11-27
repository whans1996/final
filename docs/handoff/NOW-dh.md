# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 14:13 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/issue-168-a-bugs

## 현재 기능

**③ A 버그 묶음 PR #242** — 브랜치 `feature/issue-168-a-bugs` (head eebfa91). **Codex ✅ eebfa91, CI 초록 → 사람 머지 대기** (Backend는 Maven Central 429로 한 번 실패, 재실행 통과). PR 본문 `.harness/pr-issue-168.md` (`Closes #168 #202 #200 #155 #150`)

## 진행

- ✅ 버그 5건 수정 (#168 7e3e8fa, #202 1add914, #200 2a409b3, #155 6893360, #150 6639ff2)
- ✅ /pr-review: reviewer 권장 6건 반영 (305b725 #200 상세 실패 시 링크 유지, 69bd677 경합 테스트 탈퇴 경로, 94982bb AC8 Object.assign, aada380 관리자 참여자 수, 62390c2 기획서, eebfa91 스크린샷)
- ✅ pre-push 전체 검증 통과, push 완료
- 미룸(PR 본문에 적음): #201→T-1, #203→F-29 뒤, #156→EV-1 뒤, #121→F-21b/F-26b

## 바로 다음 할 일

1. 사람이 GitHub에서 #242 머지 (상대 PR이 먼저 머지되면 `git merge origin/develop` → push → CI 재확인). 머지 뒤 Closes 5건이 닫혔는지 확인
2. 다음: ⑤ 가드 우회 #188~#191·#193 (`feature/issue-188-guard-bypass`), 11/29부터 P-1a `/spec`

## 막힌 점 / 결정 필요
- 사람: 노션 팀원 초대, F-05b liveTest · 로드맵에 E-2 행 추가 필요

## 주의
- 스크린샷용 BE(8080)·FE(5173) 프로세스가 남아 있을 수 있음 (포트 기준 강제 종료는 권한 검사에 막힘, 사람이 종료)
- 이 PC Docker에 HP-5 실측 때 남은 익명 볼륨 6개(prune은 사람 판단) · 다음 마이그레이션 V15
