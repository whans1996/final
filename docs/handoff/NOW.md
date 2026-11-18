# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 21:40 · 작업자: whans1996 (Claude, 집 PC) · 브랜치: feature/F-06-overview-explanation

## 현재 기능

F-06 개요·파일별 설명 — `docs/specs/F-06-overview-explanation.md` **APPROVED, AC1~AC15 전부 구현·커밋. PR 전 (`/pr-review` 차례)**

## 진행

- ✅ BE: 8b09a86·58a6ccd·02630fb (BE 273개 green)
- ✅ FE: bd1e642 AC13~AC15 (FE 132개 green). 이어서 `FilePath.vue`(경로를 `/` 뒤에서만 줄바꿈) 커밋
- ✅ contract-checker FE·BE 불일치 0, ui-verifier 데스크톱·375px 통과(375px 페이지 scrollWidth=clientWidth)
- ✅ 4abb8bd 프롬프트 평가 하네스 `./gradlew promptEval` + 1차 평가(DS-02-01, 결과 `.harness/prompt-eval/20261118-214618-DS-02-01-file.json`)
- ✅ 산출물: `06-화면설계서.md` SC-07 개요·파일 탭 + 스크린샷 4장, `08` §1.3 프롬프트 설계, `04-WBS`·`05-간트` F-06 진행 중
- local DB에 ui 점검용 회원·분석 1·2(DONE)가 있다. 로그인 상태는 `.harness/auth.json`(git 제외)

## 바로 다음 할 일

1. 사람 결정 대기: (a) 프롬프트 개선 반영 여부 — 08 §1.3 "1차 프롬프트 평가" 참고. 제안: 템플릿 1행의 "Spring Boot·MyBatis·Vue" 프라이밍 완화, `techStack`은 import·빌드 파일 근거만, 숙련은 코드 근거 있는 주의점 1개 이상, 없는 레이어 암시 금지, `layers` 예시 2항목 (b) F-05a 레이어 규칙에 `@RestController`/`@Controller` 어노테이션 판정 추가 여부(F-05a 수정이라 spec 결정 필요, 아니면 follow-up 이슈)
2. 그 뒤 `/clear` → `/pr-review`
3. 머지 직전 `docs(F-06): 완료 표시` 커밋 (spec 상태 DONE, WBS ✅, 간트 done)

## 막힌 점 / 결정 필요

- 사람: 이슈 #16(설정 키 키워드 확장 범위), 노션 대시보드 팀원 초대
- FE 커밋 때 Docker Desktop을 켜 둔다 (oxlint를 Docker로 실행, 꺼져 있으면 건너뜀)

## 주의

- 1.4 유스케이스(마감 11/23): `docs/산출물/02-유스케이스.md`에 분석 요청 UC 흐름 추가 필요
- 집 PC DB는 V1~V3 적용 상태(V4는 bootRun 때 적용). verify(Gradle)를 bootRun과 동시에 돌리지 않는다
