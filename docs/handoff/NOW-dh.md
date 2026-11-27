# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 13:23 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/issue-168-a-bugs

## 현재 기능

**③ A 버그 묶음 PR** — 브랜치 `feature/issue-168-a-bugs` (HP-5 PR #239 머지 뒤, P-1a 시작 11/29 전까지). PR 본문에 `Closes #168 #202 #200 #155 #150` 예정

## 진행

- ✅ #168 목표 만들기·공유 해제 경합 500 → `TeamChallengeMapper.isShared`를 `FOR SHARE OF ta` 잠금 읽기로 (7e3e8fa). 재현 테스트 `TeamChallengeConcurrencyTest`(red=FK 1452 확인 → green), verify be 전체 통과. 탈퇴는 markWithdrawn이 users를 먼저 잠가 교착 없음
- ⬜ #202 탈퇴 회원 챌린지 참여가 참여자 수·집계에 남음 (`ChallengeMapper.xml:12`, `ChallengeParticipantMapper.xml:28-30`에 users.status 조건)
- ⬜ #200 `AnalysisDetailView.vue:182` "← 내 분석" 링크를 mine일 때만
- ⬜ #155 "코드 파일 200개 · 10MB" 문구 (AnalysisRequestView 123·139, 산출물 06:157, F-05a spec 표시)
- ⬜ #150 HttpLinker axios·인스턴스 defaults.method 쓰기 반영
- 미룸: #201→T-1, #203→F-29 뒤, #156→EV-1 뒤(프롬프트), #121→F-21b/F-26b

## 이슈 정리 순서 (11-26 사용자 승인)

③ A 버그(이 PR) → ⑤ 가드 우회 #188~#191·#193 한 PR. #163→F-27b, #204→K-1. 11/29부터 P-1a

## 바로 다음 할 일

1. #202: `ChallengeApiTest`류에 "탈퇴 회원이 섞인 챌린지의 참여자 수" 실패 테스트 → 집계 쿼리에 `users.status = 'ACTIVE'` 조건
2. 이어서 #200 → #155 → #150, 끝나면 `/pr-review`

## 막힌 점 / 결정 필요
- 사람: 노션 팀원 초대, F-05b liveTest · 로드맵에 E-2 행 추가 필요
## 주의
- 이 PC Docker에 HP-5 실측 때 남은 익명 볼륨 6개 — `docker volume prune` 여부는 사람이 판단
- 다음 마이그레이션 V15 (이 PR은 마이그레이션 없음)
