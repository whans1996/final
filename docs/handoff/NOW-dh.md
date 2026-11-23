# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-23 13:25 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/issue-55-json-body-limit

## 현재 기능

공통 이슈 **#55** JSON 요청 본문 전역 크기 제한. 브랜치 `feature/issue-55-json-body-limit`. 구현·테스트 green, 커밋 완료 → `/pr-review`

## 진행

- ✅ (PR #70 #54·#32 11-20 머지, 이슈 닫힘 확인. 이 PC는 F-14 wip에 멈춰 있어 develop으로 맞춤)
- ✅ 방식: Jackson (사용자 결정). Boot 4.1 속성 `spring.jackson.factory.constraints.read.max-document-length: 1048576`(application.yml) — 커스터마이저 코드 불필요
- ✅ `GlobalExceptionHandler.handleNotReadable`: 원인 체인에 `StreamConstraintsException`이면 413 `COMMON_413`, 아니면 400. ErrorCode 추가 없음. COMMON_413 메시지는 "요청 크기가 허용 범위를 넘었습니다."로 일반화 (사용자 결정, 결정로그)
- ✅ 테스트: `GlobalExceptionHandlerTest.본문이_Jackson_읽기_한도를_넘으면_413`, `common/RequestBodyLimitTest`(로그인 1MB+1KB → 413, 1MB−1KB → 413 아님). `verify.mjs be` 통과
- ✅ `docs/conventions.md` 3절 COMMON_413 설명 갱신

## 바로 다음 할 일

1. `/pr-review` (새 세션 권장) → PR 본문 `Closes #55`, 공통 코드 변경(application.yml·GlobalExceptionHandler·ErrorCode 메시지·conventions)으로 @JoonseobKim 멘션. 알릴 점: 한도는 Spring 매퍼 전체라 Gemini/Ollama RestClient 응답에도 1MB가 걸림(출력 토큰 한도상 무관)
2. 그 뒤 #17 (created_at 시간대) → F-05b `/spec` (먼저 `node scripts/branches.mjs F-05b`)

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부, 하네스 동결 원칙 결정로그 추가 여부, 로컬 옛 브랜치 4개(F-07, F-14, common-notify-per-person, issue-54) 정리 여부

## 주의

- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시)
- 하네스 가드는 Bash 명령 문자열의 `core.hooksPath`를 막는다 → 테스트 파일 편집은 Edit 도구로
