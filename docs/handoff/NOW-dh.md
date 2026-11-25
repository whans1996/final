# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-25 21:47 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/F-27-gms-provider

## 현재 기능

**F-27 GMS 공급자** — spec `docs/specs/F-27-gms-provider.md` APPROVED, 브랜치 `feature/F-27-gms-provider` (push됨, BE·FE 전체 통과)

## 진행

- ✅ AC1~AC13 구현·테스트 green, openapi.json 갱신, 산출물(ERD·화면 설계서·08)·LLM 규칙·prompt-evaluator 문서 반영, contract-checker 불일치 0
- ✅ ui-verifier 문제 0건 (SC-05 문구, SC-07 배너 liteMode true·false, 1280·375, 스크린샷 `.harness/screens/f27_*`)
- ✅ spec 9절: base URL·크레딧(`/gmsapi/key-info`) 확인. 남은 것: 기본 모델 ID

## 바로 다음 할 일

1. 사람: `cd backend && ./gradlew liveTest --tests "*LlmLiveTest.gms*"`로 모델 목록·크레딧 확인 → `GMS_MODEL` 기본값 확정(application.yml·.env.example·spec 6절·결정로그)
2. `/pr-review` (PR 본문: 공통 파일 application.yml·build.gradle → @JoonseobKim 멘션, AC8은 회귀 고정 테스트)

## 막힌 점 / 결정 필요

- 기본 모델 ID (후보 gpt-4.1-mini 4cr, gpt-4o-mini 2cr, gpt-5-nano 1cr) — 실호출 결과 보고 사람 결정
- 사람: 노션 팀원 초대, F-05b liveTest, 제출 zip 이름, dh-ssafy `.env`에 GMS 키

## 주의

- 공통 파일 변경: `application.yml`(llm.gms 블록)·`build.gradle`(promptEval 주석) → PR "공통 코드 변경"에 적고 @JoonseobKim 멘션
- AC8 파서 테스트는 처음부터 green(기존 파서가 null 처리) — 회귀 고정용임을 PR에 적기
- node 스크립트로 Java 편집 시 백슬래시가 깨짐 → Edit 도구 사용 · 이 PC에 python 없음
