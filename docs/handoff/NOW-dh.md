# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-24 18:20 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-21a-structure-graph

## 현재 기능

**F-21a 구조 시각화: 파일 의존 그래프** (P1) spec `docs/specs/F-21a-structure-graph.md` — APPROVED. PR #118 (**5.3절 1~7 모두 끝, ready + Codex 재리뷰 요청**). 11-24 사용자 결정: Codex 지적을 하나씩 막지 않고 전수 점검 결과로 **구조를 바꿔 한 번에** + 캐시(#117) 흡수 → spec **5.3절**이 작업 목록, 결정로그 11-24 행

## 진행

- ✅ Codex 1~9차 지적 반영(재리뷰 9회) → 전수 점검 약 40건을 spec 5.3절 1~7로 정리 (11-24 결정)
- ✅ 5.3절 1~7 모두 완료 (각 항목 끝에 spec 구현 메모): 1 `d796d18` 이름 UNKNOWN·`lookupKnown` · 3 `d22972e`·`e4adde6`·`c5a45f7` WorkBudget·색인화·메모리 상한 · 5 `5a3aa56`~`6249215` 흔한 코드의 틀린 간선 7개 · 2·6 `0fab493` 토크나이저(TS script PARSE_ERROR, 줄 끝 4종, `<script` 경계, TS `!`, `?.5`) · 4 `6f950d5` 캐시(#117: DONE만 LRU 64, 같은 분석 하나만 계산, 동시 2개·10초 대기 뒤 `ANALYSIS_007` 503 — 사용자 결정, 예산 다 쓴 결과는 캐시 안 함, 삭제 이벤트로 비움) · 7 follow-up #121
- ✅ verify all 통과, push, PR 설명 갱신(Closes #117, 공통: ErrorCode·application.yml·conventions → @JoonseobKim), `gh pr ready 118`, Codex 재리뷰 요청
- ✅ Codex 10차(`051769d`) 2건 반영: P1 `ea80c31` context-path를 API마다 복사하지 않음(보기로 이어 붙임), P2 `c03dd31` 템플릿 식 안 선언이 있으면 그 식 이름 모두 UNKNOWN. 구조 테스트 164개 통과 → push, 재리뷰 11회차 요청

## 바로 다음 할 일

1. Codex 결과 확인: `node scripts/codex-review.mjs 118` (background `--wait --status` 걸어 둠). FINDINGS면 `/pr-review` 8단계 루프 — 같은 PR에서 고치는 것은 AC·보안 기준을 깨는 지적만, 나머지는 follow-up 이슈(conventions 7절)
2. Codex COMPLETE + CI 초록 → `docs(F-21a): 완료 표시 (spec DONE)` → 사람이 GitHub에서 머지 → `git switch develop && git pull`
3. 머지 뒤: 내 영역 이슈 #104·#105(F-25 집계) 또는 다음 기능

## 막힌 점 / 결정 필요

- 사람: 노션 팀원 초대

## 주의

- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시) · 하네스 동결(11-23): 버그·작업 막는 문제만 고친다
- 이 PC의 Git Bash heredoc에서 python `'''` 문자열은 백슬래시가 풀리거나 파싱이 깨진다 → Java 편집은 Edit 도구나 Write로 만든 스크립트 파일로
- 간선은 (from, to)마다 첫 줄만 남는다 → 테스트에서 "이 줄은 간선이 아니다"를 볼 때는 다른 API로 구분한다
- 내 영역 이슈 #104·#105(F-25 집계)는 F-21 뒤 기능 사이에 처리
