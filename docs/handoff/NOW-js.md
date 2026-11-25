# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-25 14:23 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/common-screens-landing

---

## 현재 기능

**SC-01 랜딩 화면 설계 초안 (WBS 1.8)** — `feature/common-screens-landing`. /pr-review 중: verifier all 통과, reviewer 반드시 수정 1(NOW 30줄)·권장 4건 반영(`8ce09b3`). PR 생성 전

## 진행

- ✅ SC-01: `06-화면설계서.md` SC-01 절(현재 자리 표시 + 계획 구성 5절·와이어프레임·상태별 UI·결정 필요 3건) + `screens/SC-01-landing-{desktop,mobile}.png`

## 바로 다음 할 일

1. PR(develop) 생성 → `node scripts/codex-review.mjs <PR> --request --wait --status`(background) → COMPLETE 확인. 머지 뒤 원격 `feature/common-screens-challenge` 삭제 여부를 사람에게 확인
2. 11/27 Figma 공동 세션 준비 (F-29: 방향·브랜드 색·토큰, 기반 Tailwind v4 + Reka UI)
3. DS-03 평가 저장소 5개(Spring BE·Vue FE·풀스택·소규모·엉성한 AI 생성 코드 + **200개 초과·TS/React 포함**) + DS-04 라벨(꼭/나와도 됨/틀림) 12/03 마감. 블라인드(김동환 열람 금지), 저장 위치 미정. V-2 세부(D·E 기준표 1~5점, v1/v2 블라인드 쌍 비교, 선택 규칙, 결과 형식)는 이때 `09-데이터셋.md`·평가 문서에 적는다
4. V-1 MVP 감사(11/29~12/02, B 영역) → V-4 FE 늦은 응답 composable(12/03~10)

## 막힌 점 / 결정 필요

- SC-01(랜딩 구현 때): 샘플 분석 데이터 출처, 로그인 회원 `/` → 대시보드, 비회원 랜딩 추천 대신 인기순 공개 카드 (F-16 spec 9절에서 미룸)

## 주의

- B 일정(WBS 재계획): F-29 11/27~12/20, U-1 12/07~14, F-28 FE 12/10~14, V-4 FE 12/03~10, DS-03·04 12/03. 최종 ERD 이미지는 W5
- 하네스 가드 의견(`xargs git reset --hard`, `python -c`/`node -e` .env 읽기 미차단)은 작업로그 11-25 14:23 메모로 옮김
