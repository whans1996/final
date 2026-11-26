# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-26 14:57 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/E-1-eval-runner

## 현재 기능

**E-1 평가 러너 전 단계 + DS-04 채점기 + 수준 구분 지표** (`docs/specs/E-1-eval-runner-grader.md`, APPROVED 11-26, 구현 전) — 11/29 B에 넘김 → EV-1 기준선. 블라인드: dev(DS-02-01) 합성 라벨로만 테스트, eval 열지 않음

## 진행

- ✅ FU-1 PR #212 머지(11-26). #170 합의 댓글은 이미 게시돼 있었음
- ✅ spec 초안 AC1~AC11: 러너 flow·concept(Testcontainers 시드 카탈로그)·quiz·all, 보고서 index·units, 채점 A·B(기계)·C·커버리지·근거 유효율, D 수준 판정·E 정답 유일(판정 Gemini), 요약은 점수만
- ✅ 사용자 결정: 라벨 형식은 김준섭에게 구조만 받기, 판정 모델 Gemini, 범위는 자동 지표만(기준표 1~5점·쌍 비교는 E-2)

## 바로 다음 할 일

1. 새 세션에서 `/implement E-1` (Opus/high). 시작점: `FlowGenerator`에서 DB 없는 메서드 분리 → `PromptEvalRunner` 핵심을 `prompteval/EvalPipeline`으로 옮기고 `EvalPipelineTest#AC1_…`부터
2. dh-home에서 `node scripts/setup.mjs` 한 번 (eval fetch 제외 refspec)

## 막힌 점 / 결정 필요
- 김준섭 답 대기 (11-26 Mattermost로 요청함): `LABEL_SCHEMA.md` 필드 구조만 (spec 9절 1) · V-2 고정 11/28 확인. eval 기록 브랜치 `eval/` 접두사 알리기는 남음
- 사람: dh-home git `user.name` 통일(H-7), 노션 팀원 초대, F-05b liveTest
- 로드맵에 E-2(D·E 기준표·쌍 비교, 12/09 전) 행 추가 필요 · `branches.mjs` 로드맵 ID 거부는 #213

## 주의
- 다음 마이그레이션은 V14 · node heredoc으로 파일 고치면 `$'`·`$&`·`\r` 이스케이프가 깨짐 → Edit 도구 · 이 PC에 python 없음
- 로드맵 상태는 `gh pr list --state all`(검색 색인 안 씀)로 계산한다. 커밋 범위에 로드맵 ID 사용 가능(`feat(K-3a): …`)
