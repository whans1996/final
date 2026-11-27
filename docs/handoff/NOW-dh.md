# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 17:31 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/P-1a-project-versions

## 현재 기능

**P-1a 프로젝트·버전 묶음·새 커밋 배지** `docs/specs/P-1a-project-versions.md` (**APPROVED 11-27**) · 브랜치 `feature/P-1a-project-versions` · 로드맵 P-1a (11/29~12/02, 넘겨주기 12/02 A→A·B)

## 진행

- ✅ P-1a spec 초안: AC1~16 (요청 묶기·FETCHING 커밋 조회로 버전·실패 대체·ZIP projectId·프로젝트 목록·상세·이름·latest-commit 배지 5분 캐시·토큰 공개 확인·분석 응답 필드·V15 백필·FE SC-25·26)
- ✅ 사용자 선택: FE 최소 화면 포함, ZIP은 요청 때 projectId 선택 (결정로그 11-27)
- 🟡 spec 승인 대기 (develop 작업 트리에 미커밋: spec·결정로그·NOW)

## 바로 다음 할 일

1. `/clear` 뒤 새 세션 `/implement P-1a` (Opus/high): `node scripts/next-migration.mjs`로 V15 재확인 → `V15__projects.sql` + `ProjectBackfillMigrationTest`(AC13)부터
2. BE 순서: AC1·4·5 `AnalysisService.request` 묶기 → AC2·3·11 `RepositoryArchiveFetcher` 커밋 조회·`zipball/{sha}` + FETCHING 버전 → AC6~10 `analysis.project` API → AC12 → FE AC14~16
3. 병렬 후보: K-1 `/spec` (다른 세션·브랜치, 12/02 넘겨주기, B CD-1이 기다림), SD-1 (B LP-1이 기다림)

## 막힌 점 / 결정 필요
- 사람: 노션 팀원 초대, F-05b liveTest · #251 남은 PC 3대 `--move-env`

## 주의
- 인라인 node -e에 .env 문자열을 넣으면 가드가 막음(스크립트 파일로) · scripts/에 prettier 금지 · 다음 마이그레이션 V15
- 셸 내비 "프로젝트" 추가는 공통 파일 → PR에서 @JoonseobKim 멘션 · 새 화면은 useLatestRequest(V-4) 사용 · #250 A 영역 FE 이관은 별도
