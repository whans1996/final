# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신: 2026-11-20 09:03 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/F-14-code-quiz

## 현재 기능

F-14 코드 이해 퀴즈 (`docs/specs/F-14-code-quiz.md`) — AC1~AC11 green, develop 병합 완료. 남은 것: 화면 재확인·스크린샷 → `/pr-review`

## 진행

- ✅ AC1~AC4·AC11 `bd9a7e9` / AC9·AC10 `df139ec` / AC5~AC8 `e97ace4`, contract-checker 불일치 0
- ✅ 375px 탭 줄바꿈 수정 `0fa4933` (`.tabs` overflow-x, `.tab` nowrap)
- ✅ develop 병합 `fed7933` (F-10 #52, #49 #53), openapi.json 재생성(nullable 2곳 정리 포함)
- ✅ `27dab65` quiz.json도 #49 방식: `conceptName` → `MockLlmClient.linkQuizConceptNames`가 QUIZ 프롬프트 개념 목록 id로 바꿈(없으면 null). spec AC11·결정로그 반영 (사용자 승인)
- 🟡 스크린샷 6장 `docs/산출물/screens/SC-07-quiz-*`·`SC-08-*`은 dh-ssafy 1차본 (탭 수정 전)

## 바로 다음 할 일

1. BE local(`cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'`) + FE dev(`cd frontend && npm run dev`) 띄우고, mock 분석을 새로 1건 돌린다 (fixture가 바뀌었으니 기존 분석 말고 새 분석)
2. ui-verifier로 SC-07 `?tab=quiz`, SC-08 풀이·결과를 1280·375에서 확인 → 스크린샷 6장 다시 찍기, 375px 탭이 가로 스크롤인지 확인
3. `docs/산출물/06-화면설계서.md` SC-07 퀴즈 탭·SC-08 절에 스크린샷 표 추가 (#49 메모는 이제 불필요)
4. `/pr-review` (새 세션)

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부

## 주의

- 공통 파일 변경: `application.yml`(quiz 블록), `docs/conventions.md`(QUIZ 행), `ErrorCode`(QUIZ 2개), `AnalysisService.checkOwned` → PR에 @JoonseobKim 멘션
- spec과 다른 점(PR 본문에): 복습 INSERT는 `NOT EXISTS + ODKU`(CLIENT_FOUND_ROWS). FE는 red 단계 생략. quiz fixture 이름 연결(11-20 결정)
- Gradle 테스트를 bootRun과 동시에 돌리지 않는다
