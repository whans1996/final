# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-23 14:00 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/issue-17-db-session-tz

## 현재 기능

공통 이슈 **#17** created_at/updated_at이 DB 세션 시간대에 의존. 방식 (a) 연결 단위 고정 (사용자 결정, 결정로그 11-23). PR 설명 `.harness/pr-issue-17.md`

## 진행

- ✅ PR #72(#55) 11-23 머지, #55 닫힘 확인
- ✅ 원인: URL의 `serverTimezone=Asia/Seoul`은 드라이버 힌트일 뿐 세션 time_zone을 바꾸지 않음. 테스트 컨테이너가 `--default-time-zone=+09:00`이라 가려져 있었음
- ✅ `IntegrationTestBase` 컨테이너를 `--default-time-zone=+00:00`(UTC)으로 변경
- ✅ 새 테스트 `common/DbSessionTimeZoneTest` 3개(세션_시간대가_서버_기본값과_무관하게_KST, DB_NOW가_서비스_Clock과_같은_시각, 기본값_created_at이_KST로_들어간다) 작성 → red 확인 (세션 +00:00, 9시간 차이)
- ✅ `application.yml` `spring.datasource.hikari.connection-init-sql: SET time_zone = '+09:00'` 추가 (이름 대신 오프셋: 시간대 테이블 빈 MySQL 대비)
- ✅ 결정로그 #17 한 줄, `backend/CLAUDE.md` MyBatis 절에 "시각" 규칙 한 줄
- ✅ `verify.mjs be` 650 통과(UTC 컨테이너 회귀 없음), 로컬 MySQL bootRun 기동 확인. 상대 이슈 댓글(피드 `activities.created_at` 9시간)도 이 고정으로 해결 → ActivityMapper 수정 불필요
- ✅ 커밋 `fix(common): #17 DB 세션 시간대를 +09:00으로 고정`

## 바로 다음 할 일

1. **PR #74** 열림 (Closes #17, @JoonseobKim 멘션). /pr-review 점검 끝(verify all 통과, reviewer 반드시 수정 0, 권장 2건 반영). Codex ✅ a350002 (지적 없음). CI BE 진행 중 → `gh pr checks 74` 초록이면 GitHub에서 머지, 머지 뒤 #17 닫힘 확인 (Closes #17, @JoonseobKim 멘션: IntegrationTestBase·application.yml 공통)
2. 그 뒤 F-05b `/spec` (먼저 `node scripts/branches.mjs F-05b`)

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부, 하네스 동결 원칙 결정로그 추가 여부, 머지된 로컬 옛 브랜치(26개) 정리 여부

## 주의

- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시)
- 하네스 가드는 Bash 명령 문자열의 `core.hooksPath`를 막는다 → 테스트 파일 편집은 Edit 도구로
