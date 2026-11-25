# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-25 14:06 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/common-screens-challenge

---

## 현재 기능

**2차 방향 정리 완료 — 다음은 SC-01 랜딩(WBS 1.8) → DS-03·04(V-2)** (2차 기획은 PR #127·#128로 develop에 반영됨. 코드 변경 없음)

## 진행

- ✅ 검증·모델·디자인 초안 → 김동환이 PR #127에 반영: 검증 수용(기준선 GMS v1·Ollama v1, 커버리지 지표 추가), 공급자 GMS, 시연 혼합(#112), 디자인 초안은 쓰지 않고 11/27 Figma 세션에서 다시 정함
- ✅ 역할 재분배: AI 기능 FE(사용량·커버리지·진행 표시·채팅·피드백)는 B (기획서 6절)
- ✅ H-1 하네스 개편 확인(PR #138 머지): 작업로그는 `stamp.mjs --append-log`, push 때 pre-push 전체 검증

## 바로 다음 할 일

1. `docs/산출물/06-화면설계서.md` SC-01 랜딩: 지금 `frontend/src/views/HomeView.vue` 기준으로 구성·상태·모바일 배치 작성 + `docs/산출물/screens/SC-01-landing-{desktop,mobile}.png` (WBS 1.8, 11/28). 새 브랜치 `feature/common-screens-landing`(origin/develop) 권장
2. 11/27 Figma 공동 세션 준비 (F-29: 방향·브랜드 색·토큰, 기반 Tailwind v4 + Reka UI)
3. DS-03 평가 저장소 5개(Spring BE·Vue FE·풀스택·소규모·엉성한 AI 생성 코드 + **200개 초과·TS/React 포함** 요청) + DS-04 라벨(꼭/나와도 됨/틀림) 12/03 마감. 블라인드(김동환 열람 금지), 저장 위치 미정. develop에 없는 V-2 세부(D·E 기준표 1~5점, v1/v2 블라인드 쌍 비교, 선택 규칙 A·B 관문 통과 중 C·D·E 우위, 결과 = 요약 표+세부 표+전후 사례)는 이때 `09-데이터셋.md`·평가 문서에 적는다
4. V-1 MVP 감사(11/29~12/02, B 영역) → V-4 FE 늦은 응답 composable(12/03~10)

## 막힌 점 / 결정 필요

- 없음. 이 브랜치(이름 불일치)는 NOW·작업로그 커밋만 있어 다음 docs PR에 같이 넣거나 그대로 PR로 올린다

## 주의

- B 일정(WBS 재계획): F-29 11/27~12/20, U-1 12/07~14, F-28 FE 12/10~14, V-4 FE 12/03~10, DS-03·04 12/03
- 하네스 가드 의견(PR #138 이후): `xargs git reset --hard` 통과(기존 가드는 차단), `python -c`/`node -e`로 .env 읽기·`gh api` 머지 미차단 → 필요하면 김동환에게 follow-up 제안
- 최종 ERD 이미지(`diagrams/erd.png`)는 W5에 붙인다
