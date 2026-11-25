# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-25 23:02 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/F-27-gms-provider

## 현재 기능

**F-27 GMS 공급자** — spec `docs/specs/F-27-gms-provider.md` DONE 표시, **PR #161 머지 대기** (CI 초록·Codex ✅ 6f90df2, 이후 docs만)

## 진행

- ✅ AC1~AC13 구현·테스트 green, 산출물(ERD·화면 설계서·08) 반영, GMS 기본 모델 `gpt-4.1-mini` 확정
- ✅ /pr-review: verifier 전체 통과, contract-checker 불일치 0, reviewer 반드시 수정 0 · 권장 5건 모두 반영(0ada431: GMS_MODEL·stage-models 빈 값 기동 실패 + LlmConfigTest 2개, 폴백 주석, Ollama 0.5+, prompt-evaluator Git Bash 예시, 결정로그 대안 칸)
- ✅ 스크린샷 `docs/산출물/screens/SC-05-request-gms-*`·`SC-07-lite-mode-*` 커밋(6f90df2), PR 본문에 첨부
- ✅ **Codex ✅ 6f90df2** (지적 없음), CI 전체 초록, spec `DONE (PR #161)` 완료 표시

## 바로 다음 할 일

1. 사람이 GitHub에서 PR #161 머지 (wip 없음 → 일반 머지 가능). 머지 전 상대 PR이 develop에 먼저 들어갔으면 `git merge origin/develop` → push → CI 재확인
2. 머지 후 `/clear` → `/pickup`으로 다음 기능 선택 (F-27b 단계별 모델 배정 또는 V-2 기준선, 내 영역 이슈 #121·#150·#155·#156)

## 막힌 점 / 결정 필요

- **GMS 키 2026-12-09 18:00 만료** (발표 12-24 전) → 재발급·연장 사람 확인 필요
- 사람: 노션 팀원 초대, F-05b liveTest, 제출 zip 이름, dh-ssafy `.env`에 GMS 키

## 주의

- 머지 전 상대 PR이 develop에 먼저 들어가면 `git merge origin/develop` → push로 CI 재확인 (V13·openapi.json)
- ui-verifier가 로컬 DB에 테스트 계정 `f27shot@example.com`을 만들어 둠 (로컬 전용)
- node 스크립트로 Java 편집 시 백슬래시가 깨짐 → Edit 도구 사용 · 이 PC에 python 없음
