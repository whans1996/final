# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-26 16:54 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/issue-211-challenge-actions

---

## 현재 기능

**#211 챌린지 행동 링크·완료 화면 진행** — PR #222 열림 (`feature/issue-211-challenge-actions`, FE만, A 영역 파일 → PR에 @whans1996 멘션). V-2는 PR #217 머지(eval 3ab0db0)

## 진행

- ✅ 구현·reviewer 반영(size 50 필수, 권장 4건)·화면 설계서 SC-07·08·11·13·스크린샷 4b114a1, push
- 🟡 PR #222: CI 진행 중, Codex 리뷰 요청함 (head 4b114a1)
- 로컬(js-home): 화면 점검용 테스트 관리자 회원을 로컬 DB에 만듦. `.harness/auth.json`은 무효(다시 만들 것)

## 바로 다음 할 일

1. PR #222 Codex 루프: `node scripts/codex-review.mjs 222` → FINDINGS면 spec·보안만 고치고 P2는 follow-up 이관 → COMPLETE까지. CI 초록 확인 후 GitHub에서 머지
2. 김동환에게 "평가 세트 고정(3ab0db0)" 알림 확인 (11/28 B→A 넘김)
3. 11/27 Figma 공동 세션 → `/spec F-29` (11/29~) · EV-1은 E-1 PR #220 머지 대기
4. /progress 때: 기획서 579·WBS 4.1의 DS 12/03 → 11/26 고정으로

## 막힌 점 / 결정 필요

- SC-01(랜딩 구현 때): 샘플 분석 데이터 출처, 로그인 회원 `/` → 대시보드, 비회원 랜딩 인기순 공개 카드 (F-16 spec 9절)

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용(개념 tier·개수, 결함, forbidden, 채점 기준)을 적지 않는다.** 진행률·eval 커밋 SHA만
- **Figma는 공동 세션 전까지 건드리지 않는다** (김동환 요청)
