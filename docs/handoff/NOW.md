# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 19:40 · 작업자: whans1996 (Claude, 집 PC) · 브랜치: feature/F-05a-analysis-zip

## 현재 기능

F-05a 분석 요청(ZIP) — `docs/specs/F-05a-analysis-request-zip.md` · **PR #14 (develop)** 열림, 머지 대기

## 진행

- ✅ AC1~16 green. /pr-review: verify all 통과, contract-checker 불일치 0, reviewer 반드시 2·권장 7 반영(deb7f8f·819dc96)
- ✅ Codex P2 6건 반영(마스킹 형식 변형, 1a5e7f2~1bad898)
- ✅ Codex 추가 재리뷰(1bad898) P2 1건 반영: `ZipArchiveReader.readAtMost()`가 다 읽은 엔트리의 CRC32를 비교해 다르면 ANALYSIS_400 (`AC9_헤더는_멀쩡하고_내용만_바뀐_ZIP은_CRC_불일치로_ANALYSIS_400`). 사용자 결정으로 **Codex 재요청 없이** 마무리

## 바로 다음 할 일

1. `gh pr checks 14` 초록 확인 (CRC 수정 커밋 기준)
2. PR #14 설명에 "Codex 추가 지적(CRC) 반영, 재리뷰 생략" 한 줄 추가 → 팀원 A가 GitHub에서 머지
3. 머지 후 `/progress`에서 F-05a spec DONE 처리, 다음 기능 spec 선택

## 막힌 점 / 결정 필요

- spec 10절 미결: 설정 키 키워드 확장(`private-key`·`access-key` 등 `key`로 끝나는 키)
- 후속: 다른 도메인 created_at/updated_at도 DB 세션 시간대 의존(UTC MySQL이면 9시간 차이). 분석은 서비스 Clock으로 고침

## 주의

- 집 PC: 스마트 앱 컨트롤이 oxlint 네이티브 바인딩을 막아 FE lint만 실패(테스트·빌드는 통과). FE 커밋 전 해결 필요 (wasm 바인딩 `--no-save` 설치 또는 SAC 끄기, 사람 결정)
- 집 PC DB는 새로 만들어 비어 있음(V1~V3 적용). 화면 점검용 회원은 필요할 때 가입
- `.env`는 properties 형식으로 읽힌다: 따옴표·끝 공백이 값에 포함됨
- verify(Gradle)를 bootRun과 동시에 돌리지 않는다. V1·V2·V3 수정 금지
