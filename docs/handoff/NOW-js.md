# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-25 14:13 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/common-screens-landing

---

## 현재 기능

**SC-01 랜딩 화면 설계 초안 작성 (WBS 1.8)** — `feature/common-screens-landing`(이전 `feature/common-screens-challenge`의 NOW·작업로그 커밋 포함). PR 전

## 진행

- ✅ 2차 기획(PR #127·#128) 반영 확인: AI 기능 FE(사용량·커버리지·진행 표시·채팅·피드백)는 B, 디자인은 11/27 Figma 세션에서 다시 정함
- ✅ SC-01: `06-화면설계서.md` SC-01 절(현재 자리 표시 + 계획 구성 5절·와이어프레임·결정 필요 2건) + `screens/SC-01-landing-{desktop,mobile}.png`(현재 화면)

## 바로 다음 할 일

1. SC-01 docs PR 올리기: `/pr-review` → PR(develop). 머지 뒤 원격 `feature/common-screens-challenge` 삭제 여부를 사람에게 확인
2. 11/27 Figma 공동 세션 준비 (F-29: 방향·브랜드 색·토큰, 기반 Tailwind v4 + Reka UI)
3. DS-03 평가 저장소 5개(Spring BE·Vue FE·풀스택·소규모·엉성한 AI 생성 코드 + **200개 초과·TS/React 포함** 요청) + DS-04 라벨(꼭/나와도 됨/틀림) 12/03 마감. 블라인드(김동환 열람 금지), 저장 위치 미정. develop에 없는 V-2 세부(D·E 기준표 1~5점, v1/v2 블라인드 쌍 비교, 선택 규칙 A·B 관문 통과 중 C·D·E 우위, 결과 = 요약 표+세부 표+전후 사례)는 이때 `09-데이터셋.md`·평가 문서에 적는다
4. V-1 MVP 감사(11/29~12/02, B 영역) → V-4 FE 늦은 응답 composable(12/03~10)

## 막힌 점 / 결정 필요

- SC-01 결정 필요(랜딩 구현 때): 샘플 분석 데이터 출처(FE 고정 vs 공개 분석 API, 지금 API는 회원만), 로그인 회원 `/` → 대시보드 이동 여부(F-16에서 미룸)

## 주의

- B 일정(WBS 재계획): F-29 11/27~12/20, U-1 12/07~14, F-28 FE 12/10~14, V-4 FE 12/03~10, DS-03·04 12/03
- 하네스 가드 의견(PR #138 이후): `xargs git reset --hard` 통과(기존 가드는 차단), `python -c`/`node -e`로 .env 읽기·`gh api` 머지 미차단 → 필요하면 김동환에게 follow-up 제안
- 최종 ERD 이미지(`diagrams/erd.png`)는 W5에 붙인다
