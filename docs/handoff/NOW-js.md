# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-26 22:47 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/issue-225-challenge-progress-paging

---

## 현재 기능

**#225 챌린지 진행 표시 페이지 이어 받기** — `feature/issue-225-challenge-progress-paging`. FE만 (`ChallengeProgressNotice.vue` `fetchAllOngoing`: size 50으로 total까지, 빈 페이지면 멈춤, 상한 20페이지). API 변경 없음

## 진행

- ✅ `ChallengeProgressNotice.spec` `#225` 6건 green (페이지 이어 받기·빈 페이지·id 중복·20페이지 상한·중간 실패·경쟁), `verify fe` 통과 (5ed9191, bca670e)
- ✅ reviewer 반드시 수정 없음, 권장 3건 반영. ✅ PR #232 (Closes #225, A 영역 파일이라 @whans1996 멘션)
- 로컬(js-home): 화면 점검용 테스트 관리자 회원을 로컬 DB에 만듦. `.harness/auth.json`은 무효(다시 만들 것)

## 바로 다음 할 일

1. PR #232 CI ✅·Codex COMPLETE(bf66e17, 지적 없음) → 사람이 GitHub에서 머지 → `git switch develop && git pull`
2. 김동환에게 "평가 세트 고정(3ab0db0)" 알림 확인 (11/28 B→A 넘김)
3. 11/27 Figma 공동 세션 → 새 세션 `/spec F-29` (11/29~, Opus/high)
4. EV-1 기준선 (11/29~12/01, 실제 LLM 호출이라 사람이 요청할 때만, Opus/medium)
5. /progress 때: 기획서 579·WBS 4.1의 DS 12/03 → 11/26 고정으로

## 막힌 점 / 결정 필요

- SC-01(랜딩 구현 때): 샘플 분석 데이터 출처, 로그인 회원 `/` → 대시보드, 비회원 랜딩 인기순 공개 카드 (F-16 spec 9절)

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용(개념 tier·개수, 결함, forbidden, 채점 기준)을 적지 않는다.** 진행률·eval 커밋 SHA만
- **Figma는 공동 세션 전까지 건드리지 않는다** (김동환 요청)
