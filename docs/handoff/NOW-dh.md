# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-26 17:05 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/E-1-eval-runner

## 현재 기능

**E-1 평가 러너 전 단계 + DS-04 채점기 + 수준 구분 지표** (`docs/specs/E-1-eval-runner-grader.md`, DONE) — **PR #220 머지 대기** (`Roadmap: E-1`), 11/29 B에 넘김 → EV-1 기준선. 블라인드: dev(DS-02-01) 합성 라벨로만 테스트

## 진행

- ✅ AC1~AC11 green (prompteval 26개), verify all 통과(BE 1316·FE 732), pre-push 통과
- ✅ /pr-review reviewer 반영: ZIP fixture는 파일 이름을 id로 datasets.json split·오염 조회(`EvalPaths.zipFixture`, DS-03-01 → DS-03), 요약을 공급자 × 단계 × 분할 × 오염·재채점은 최신만(`EvalSummary`), 채점기 Docker 의존 제거(`EvalPaths`), 폴더 채점 실패 보고서만 건너뜀·`fixtureZip`은 `.harness/prompt-eval/repos/`에서 재탐색(`EvalPaths.locateZip`), 판정 켠 grade 연결 테스트
- 후속 이슈: #218 개념 실패 수준의 C 분모(E-2 spec에서 결정), #219 퀴즈 해설 위치 언급 정규식 오탐(EV-1 전 권장), #221 재시도 빈 목록, #223 수준 구성 재실행 중복
- ✅ PR #220: CI 초록, **Codex ✅ ac9109b** (재리뷰 4회, prompt-evaluator eval 실행 전 확인 추가 포함, P2 이관 #219 #221 #223), spec DONE 표시 커밋. **머지 대기 (사람이 GitHub에서)**

## 바로 다음 할 일

1. PR #220을 GitHub에서 머지 (머지 전 develop에 새 커밋이 있으면 `git merge origin/develop` → push → CI 초록 재확인)
2. 리뷰 회차 규칙(결정로그 11-26: 2회차부터 P2는 이관 후 --resolve)을 HP-4에서 `.claude/skills/pr-review/SKILL.md` 8-3·`docs/conventions.md` 7절에 반영, `verify all`에 harness 포함도 HP-4
3. ✅ 김준섭에게 EV-1 실행 전 알림 댓글 (PR #220, 11-26): ZIP·라벨 이름 = id, 금지 사실 Gemini, level=all 보고서만, `eval/` 접두사
4. dh-home에서 `node scripts/setup.mjs` 한 번 (eval fetch 제외 refspec)

## 막힌 점 / 결정 필요
- 사람: dh-home git `user.name` 통일(H-7), 노션 팀원 초대, F-05b liveTest
- 로드맵에 E-2(D·E 기준표·쌍 비교, 12/09 전) 행 추가 필요 · `branches.mjs` 로드맵 ID 거부는 #213

## 주의
- 다음 마이그레이션은 V14 · node heredoc으로 파일 고치면 `$'`·`$&`·`\r`·`\\` 이스케이프가 깨짐 → Edit 도구 · 이 PC에 python 없음
- 금지 사실 판정은 실호출 미확인 (B의 eval 실행이 첫 확인)
