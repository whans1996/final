# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 09:00 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-00-scaffold (PR #2 → develop)

## 현재 기능

F-00 프로젝트 뼈대 (spec 없음, 인프라 작업). 구현·리뷰·Codex 지적 반영 완료 → PR #2 머지 대기.

## 진행

- ✅ BE Boot 3.5.16 뼈대 + V1·V2 마이그레이션 (테스트 38 green), FE Vue 3 뼈대 (테스트 14 green) — 교육장 PC에서도 재확인
- ✅ Codex 리뷰 지적: `backend/gradlew` 실행 권한 100755 (7ee4db2)
- ✅ 하네스: CI chmod 단계 제거, guard-bash hooksPath 조회 오탐, setup gh `--active`, `.gitattributes` `eol=lf`
- ✅ CI 수동 실행 run 44203662970 초록 (chmod 단계 없이 Backend·Frontend 통과)

## 바로 다음 할 일

1. PR #2를 GitHub에서 머지 (사람, CI 초록 확인 후)
2. 각 PC: 로컬 DB 생성(`docs/환경설정.md` 3절) + `.env` 작성 → `cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'` → http://localhost:8080/swagger-ui/index.html 확인 (아직 아무도 안 해 봄)
3. develop에서 `feature/F-01-signup` → `/spec F-01`(회원가입), `/spec F-02`(로그인·JWT: Refresh 전달 방식, `/api/auth/**` 공개 범위 명시 나열) → 승인 후 `/implement`
4. W1 산출물: `02-유스케이스.md` 확정, 시드 카드 `R__seed_concepts.sql`(`09-데이터셋.md` DS-01 규칙)

## 막힌 점 / 결정 필요

- CI가 push·PR 이벤트로 자동 실행되지 않음 (11/18 push 후에도 pull_request 실행 0건, workflow active·권한 all). Settings → Actions, PR close/reopen으로 트리거되는지 확인 필요 (사람)
- reviewer 권장, spec에서 결정: DB 세션 시간대·DTO `OffsetDateTime`, `/uploads/**` 범위(F-24), 업로드 한도 vs `ANALYSIS_413`(F-05), `analysis_concepts` 위치(F-10), Access 토큰 role 30분 신뢰(F-02·F-03)
- 팀원 GitHub 아이디(리뷰어 지정), 제출 zip 이름용 지역·반·이름 (사람)

## 주의

- 다른 PC는 pull 후 `git ls-files --eol | grep w/crlf`로 CRLF 파일이 남았는지 확인 (있으면 해당 파일 삭제 후 `git checkout -- <파일>`)
- verify(Gradle)를 두 개 동시에 돌리지 않는다 (Windows 파일 잠금으로 compileTestJava 실패). 커밋 hook도 Gradle을 돌린다
- V1·V2 수정 금지, 보강은 V3+. 분석·회원은 soft delete. 새 PC는 `npx playwright install chromium`, Docker Desktop 실행 필요
