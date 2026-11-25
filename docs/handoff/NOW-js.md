# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-25 14:39 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/common-now-after-pr147

---

## 현재 기능

**없음 (기능 사이)**. SC-01 PR #147 develop 머지 완료(11-25 05:35Z). 원격·로컬 `feature/common-screens-{landing,challenge}` 삭제됨. 이 브랜치(`feature/common-now-after-pr147`)는 NOW·작업로그만 담는다 → 다음 작업 브랜치를 여기서 따거나 다음 docs PR에 함께 올린다

## 진행

- ✅ SC-01: `06-화면설계서.md` SC-01 절 + `screens/SC-01-landing-{desktop,mobile}.png` (develop 반영). WBS 1.8·진행현황 "남음 SC-01"은 11/29 `/progress`가 고침

## 바로 다음 할 일

1. 11/27 Figma 공동 세션 준비 (F-29: 방향·브랜드 색·토큰, 기반 Tailwind v4 + Reka UI). F-29 spec 없음 → 세션 결과로 `/spec F-29` (Opus/high 새 세션)
2. 작업 순서와 무관한 일만 먼저: 공통 follow-up 이슈(#143 Swagger 태그 description 중복 등)는 영역 확인 후
3. DS-03 평가 저장소 5개(Spring BE·Vue FE·풀스택·소규모·엉성한 AI 생성 코드 + **200개 초과·TS/React 포함**) + DS-04 라벨(꼭/나와도 됨/틀림) 12/03 마감. 블라인드(김동환 열람 금지), 저장 위치 미정(블라인드라 공용 저장소 불가 → 사람이 정해야 시작). V-2 세부(D·E 기준표 1~5점, v1/v2 블라인드 쌍 비교, 선택 규칙, 결과 형식)는 이때 `09-데이터셋.md`·평가 문서에 적는다
4. V-1 MVP 감사(11/29~12/02, B 영역) → V-4 FE 늦은 응답 composable(12/03~10)

## 막힌 점 / 결정 필요

- SC-01(랜딩 구현 때): 샘플 분석 데이터 출처, 로그인 회원 `/` → 대시보드, 비회원 랜딩 추천 대신 인기순 공개 카드 (F-16 spec 9절에서 미룸)

## 주의

- B 일정(WBS 재계획): F-29 11/27~12/20, U-1 12/07~14, F-28 FE 12/10~14, V-4 FE 12/03~10, DS-03·04 12/03. 최종 ERD 이미지는 W5
- 하네스 가드 의견(`xargs git reset --hard`, `python -c`/`node -e` .env 읽기 미차단)은 작업로그 11-25 14:23 메모로 옮김
