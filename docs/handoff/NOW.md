# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 00:10 · 작업자: whans1996@naver.com (Claude) · 브랜치: feature/F-00-scaffold (PR #2 → develop, CI 초록)

## 현재 기능

F-00 프로젝트 뼈대 (spec 없음, 기획서 "다음 액션 2"의 인프라 작업). 구현·리뷰 반영 완료 → PR #2 머지 대기.

## 진행

- ✅ 백엔드 Boot 3.5.16 뼈대 + `V1__init.sql`(ERD P0 22개) + `V2__analysis_soft_delete_and_constraints.sql`, 테스트 38개 green
- ✅ 프론트엔드 Vue 3 뼈대 (`api/http.js`, `stores/auth.js`, `authGuard`), 테스트 14개 green, ui-check 통과
- ✅ /pr-review: verifier 전체 통과, reviewer "반드시 수정" 2건 반영 (4xx 예외 → 공통 코드, 분석 soft delete V2)
- ✅ PR #2 생성 (https://github.com/whans1996/final/pull/2), CI 수동 실행 초록 (run 44112186601: Backend·Frontend 검증 성공)

## 바로 다음 할 일

1. PR #2를 GitHub에서 머지 (사람, wip 없음 → 일반 머지). 이 커밋 이후 CI를 다시 돌리려면 `gh workflow run CI --ref feature/F-00-scaffold` (docs만 바뀌어 재실행 필수는 아님)
2. 각 PC: 로컬 DB 생성(`docs/환경설정.md` 3절) + `.env` 작성 → `cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'` → http://localhost:8080/swagger-ui/index.html 확인 (아직 아무도 안 해 봄)
3. `/spec F-01`(회원가입), `/spec F-02`(로그인·JWT: Refresh 전달 방식, `/api/auth/**` 공개 범위 명시 나열) → 승인 후 `/implement`
4. W1 산출물: `02-유스케이스.md` 확정, 시드 카드 `R__seed_concepts.sql`(`09-데이터셋.md` DS-01 규칙)

## 막힌 점 / 결정 필요

- CI가 push·PR 이벤트로 자동 실행되지 않음 (PR #1부터). 수동 실행은 정상. 저장소 Settings → Actions → General 확인 필요 (사람)
- reviewer 권장, spec에서 결정: DB 세션 시간대(`forceConnectionTimeZoneToSession=true`)·DTO `OffsetDateTime` 규칙, `/uploads/**` 범위(F-24), 업로드 한도 vs `ANALYSIS_413`(F-05), `analysis_concepts` 위치 여러 곳(F-10), Access 토큰 role 30분 신뢰(F-02·F-03)
- 팀원 GitHub 아이디(리뷰어 지정), 제출 zip 이름용 지역·반·이름 (사람)

## 주의

- V1·V2 커밋됨 → 수정 금지, 보강은 V3+. **분석·회원은 soft delete** (`analyses.deleted_at`, `users.status`; backend/CLAUDE.md MyBatis 절)
- 새 환경변수: `.env.example` 참고 (DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, LLM_PROVIDER, GEMINI_API_KEY). `JWT_SECRET=` 빈 값 금지(주석 처리)
- 새 의존성: FE `playwright`(PC마다 `npx playwright install chromium`). 첫 BE 테스트는 mysql:8.4 이미지 다운로드로 느림
- 일부러 깨 둔 테스트 없음. 평가(eval) 데이터는 프롬프트·fixture·시드 카드에 쓰지 않는다
