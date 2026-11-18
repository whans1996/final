# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 17:53 · 작업자: whans1996 (Claude, SSAFY 교육장 PC → 집 PC로 이어감) · 브랜치: feature/F-05a-analysis-zip

## 현재 기능

F-05a 분석 요청(ZIP) — `docs/specs/F-05a-analysis-request-zip.md` · **PR #14 (develop)** 열림, Codex 추가 재리뷰 대기

## 진행

- ✅ AC1~16 green. /pr-review: verify all 통과, contract-checker 불일치 0, reviewer 반드시 2·권장 7 반영(deb7f8f·819dc96)
- ✅ Codex P2 6건 반영: 흐름 매핑·properties 줄 이음(1a5e7f2), 파서 기반 2차 마스킹 `ConfigSecretValues`(460113b), 별칭 문맥별 방문(27b3f27), 이스케이프 값이면 파일 전체 가림·properties 공백 구분자(1bad898)
- 🟡 Codex 재리뷰: 한도 3회 소진 후 사용자 승인으로 **1회 추가 요청**(head 1bad898). CI도 1bad898 실행 중(819dc96까지 초록)

## 바로 다음 할 일

1. `git pull` 후 `node scripts/codex-review.mjs 14` (요청 없이 판정만). COMPLETE면 NOW.md에 `Codex ✅ 1bad898`, `gh pr checks 14` 초록 확인
2. 또 FINDINGS면 **루프를 더 돌리지 말고** 사람에게 반영/후속 여부를 묻는다 (마스킹 형식 변형 지적이 계속 나오는 중)
3. COMPLETE + CI 초록이면 팀원 A가 GitHub에서 머지 → `/progress`에서 spec DONE

## 막힌 점 / 결정 필요

- spec 10절 미결: 설정 키 키워드 확장(`private-key`·`access-key` 등 `key`로 끝나는 키)
- 후속: 다른 도메인 created_at/updated_at도 DB 세션 시간대 의존(UTC MySQL이면 9시간 차이). 분석은 서비스 Clock으로 고침

## 주의

- 로컬 DB(교육장 PC)에 화면 점검용 회원 `ui-check@vibenote.test`/`uicheck1234` — 집 PC에는 없음, 필요하면 가입
- Git Bash heredoc·node -e로 Java 문자열을 쓰면 백슬래시가 줄어든다. Java 소스는 Write/Edit 도구로 쓴다
- verify(Gradle)를 bootRun과 동시에 돌리지 않는다. V1·V2·V3 수정 금지. 새 의존성 없음(snakeyaml은 Boot 전이 의존)
