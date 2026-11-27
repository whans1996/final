# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 14:35 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/issue-188-guard-bypass

## 현재 기능

**⑤ 가드 우회 묶음** — 브랜치 `feature/issue-188-guard-bypass` (head b0f7f90, push 전). #188 #189 #190 #191 #193 수정 커밋 1개. 다음은 `/pr-review` (PR 본문에 `Closes #188 #189 #190 #191 #193`)

## 진행

- ✅ #242 머지(11/27 14:15), Closes 5건 닫힘 확인
- ✅ 이슈별 실패 테스트 먼저 → 구현 (guard.test.mjs 6개 test, file-guard.test.mjs 1개 test 추가, 하네스 테스트 통과)
- ✅ #188 재귀 검색: `searchSpec`·`searchReads`·`searchReadsSecret`(guard.mjs) + hook `repoSecrets()`(guard-bash.mjs, git ls-files로 비밀 파일 목록), PowerShell `psPipeReadsSecret`, 글롭 `globMatch`·`globHitsSecret`(protect.mjs), file-guard Grep glob·DC includeHidden
- ✅ #189 `globReaches`·`evalHit`·`recursiveRoots`, parseGit `paths`, checkCode 리터럴 / #190 unwrap PS 할당·`]::` 호출 / #191 checkGh alias·worktree·rm / #193 TEXT_ONLY 리다이렉션·checkHttp --method
- ✅ 지난 도구 호출 18,478개 재판정(임시 스크립트, 저장소 밖): 코드 속 `/**`를 eval 글롭으로 본 오탐을 고침. 남은 차이는 worktree --force 2건, 루트 재귀 grep 14건(대부분 하위 폴더에서 실행이라 실제로는 통과)

## 바로 다음 할 일

1. `/pr-review`: reviewer·verifier → push → PR (`Closes #188 #189 #190 #191 #193`, Roadmap 줄은 로드맵 항목이 없으면 생략 여부 확인) → Codex 루프
2. 그다음 11/29부터 P-1a `/spec` (Opus/high)

## 막힌 점 / 결정 필요
- 사람: 노션 팀원 초대, F-05b liveTest · 로드맵에 E-2 행 추가 필요
- 남긴 범위(PR 본문에 적을 것): `find -name .env | xargs cat`류 파이프, 이미 만들어진 gh 별칭, 저장소 밖 폴더의 비밀 파일

## 주의
- 이 PC Docker에 HP-5 실측 때 남은 익명 볼륨 6개(prune은 사람 판단) · 다음 마이그레이션 V15
