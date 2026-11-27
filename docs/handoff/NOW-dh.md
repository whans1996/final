# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 13:29 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/issue-168-a-bugs

## 현재 기능

**③ A 버그 묶음 PR** — 브랜치 `feature/issue-168-a-bugs` (P-1a 시작 11/29 전까지). PR 본문 `Closes #168 #202 #200 #155 #150` 예정

## 진행

- ✅ #168 목표 만들기·공유 해제 경합 500 → `TeamChallengeMapper.isShared` `FOR SHARE OF ta` (7e3e8fa, `TeamChallengeConcurrencyTest` red=FK 1452 → green)
- ✅ #202 탈퇴 회원 챌린지 참여를 참여자 수에서 제외 (1add914, `ChallengeApiTest.AC1_AC2_탈퇴_회원의_참여는…`)
- ✅ #200 남의 공개 분석에서 "← 내 분석" 숨김 (2a409b3, `AnalysisDetailView.spec` #200)
- ✅ #155 "코드 파일 200개 · 10MB" 문구 → ZIP 10MB·아카이브 30MB·중요 파일 30개 (6893360, 산출물 06·F-05a spec 표시)
- 🟡 #150 HttpLinker axios·인스턴스 `defaults.method` 쓰기 반영 (`analysis/structure/http/HttpLinker.java` readTaints·method, `StructureAnalyzerTest`)
- 미룸: #201→T-1, #203→F-29 뒤, #156→EV-1 뒤(프롬프트), #121→F-21b/F-26b. 다음은 ⑤ 가드 우회 #188~#191·#193 한 PR, 11/29부터 P-1a

## 바로 다음 할 일

1. #150: `Taints`에 `axiosMethod`·`instanceMethod`를 `writesBase`처럼 모으고, 호출 config에 method가 없을 때 표시가 있으면 method null. 실패 테스트 먼저
2. `node scripts/verify.mjs all` → `/pr-review` (PR 본문에 미룬 이슈 4개 적기)

## 막힌 점 / 결정 필요
- 사람: 노션 팀원 초대, F-05b liveTest · 로드맵에 E-2 행 추가 필요
## 주의
- 이 PC Docker에 HP-5 실측 때 남은 익명 볼륨 6개 — `docker volume prune` 여부는 사람이 판단
- 다음 마이그레이션 V15 (이 PR은 마이그레이션 없음)
