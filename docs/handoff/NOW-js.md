# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-27 13:14 · 작업자: JoonseobKim · PC: js-ssafy · 브랜치: feature/F-29-design-common

## 현재 기능

**F-29 디자인 공통 PR (토큰·폰트·공통 컴포넌트·앱 셸)** — `feature/F-29-design-common` (11/29~12/02, `Roadmap: F-29`). spec 아직 없음. 11/27 Figma 세션 끝남, 결정 5줄 결정로그(11-27) → `/spec F-29`. 병행: **EV-1 기준선 실행·채점 끝남 (11/27)**, 점수 공유·기록 남음

## 진행

- ✅ 11/27 Figma 세션: 토큰·폰트·Tailwind 이전·공통 컴포넌트 7개·앱 셸·알림(T1)·랜딩 3개·SD-1 범위 → 결정로그 11-27 5줄
- ✅ EV-1 생성: 9개 fixture(DS-02-05~10, DS-03-01~03) × stage=all × level=all, GMS gpt-4.1-mini, 실패 0
- ✅ EV-1 채점: Gemini 판정(gemini-3.5-flash-lite, seed 42) **독립 2회**, 9개 모두 `라벨=true`·`split=eval`. 정본 = 2회차 `.harness/prompt-eval/ev1-20261127-v2/summary.md` (js-ssafy 로컬, 로그 `ev1-v3-grade.log`)
- 판정 흔들림(1회 vs 2회, 그룹 평균): D·E 정답 유일 ±2.3%p 이내, 판정 없는 지표(A·B 경로·C·커버리지·근거)는 같음, **v2 하한 ✅/❌ 두 번 모두 같음**
- 2회차가 1회차와 겹쳐 돌아 마지막에 Gradle `in-progress-results-generic.bin` 없음으로 BUILD FAILED → 결과 파일은 다 쓴 뒤라 점수 영향 없음. promptGrade는 동시에 두 개 돌리지 않는다
- js-ssafy: eval 저장소 `data/raw/eval/`(3ab0db0), ZIP `data/raw/eval-zips/DS-03-0{1,2,3}.zip`, 라벨 `-Plabels=../data/raw/eval/labels`

## 바로 다음 할 일

1. EV-1 기록: `eval/EV-1-baseline` 브랜치(develop에서)에 summary.md 점수표만 + 판정 흔들림 범위 → docs PR (`Roadmap: EV-1`). 12/01 김동환에게 summary.md 점수만 공유 (AI 출력 바꾸는 PR 머지 해제 신호)
2. Figma(11/28까지, B): 토큰 스타일·공통 컴포넌트 7개·앱 셸 3폭 프레임
3. 새 세션(Opus/high) `/spec F-29`: 결정로그 11-27 5줄 = 12/02 기반 PR 경계
4. 김동환에게 요청: 로드맵·기획서에 알림 항목(T1, B 12/09~12/14) 추가 docs PR, SD-1 범위 넓힘 반영
5. /progress 때: 기획서 579·WBS 4.1의 DS 12/03 → 11/26 고정으로

## 막힌 점 / 결정 필요

- GMS 키 만료 12/09 18:00 → EV-2(12/09~12/12) 전에 재발급·연장 확인
- EV-1 기록 위치(새 문서 경로) 미정 → 사람에게 확인

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용(개념 이름·개수, 결함, forbidden, 채점 기준)을 적지 않는다.** 점수·개수·eval 커밋 SHA만
- **Figma는 공동 세션 전까지 건드리지 않는다** (김동환 요청)
