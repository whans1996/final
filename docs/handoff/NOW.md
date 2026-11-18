# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 19:50 · 작업자: whans1996 (Claude, 집 PC) · 브랜치: feature/common-harness-notion

## 현재 기능

없음 — 개발환경 점검 결과 반영(하네스 운영 보완 + 노션 팀 대시보드). 기능 코드 변경 없음

## 진행

- ✅ 노션 대시보드·AI 활용 사례 DB(16건)·회의 DB 생성, 링크는 `docs/notion.md`
- ✅ 정리 PR 폐지(`/pr-review` 10단계에서 PR 안에 완료 표시), 리뷰 지적 처리 기준, 후속 작업 → Issues `follow-up`(#16, #17)
- ✅ `docs/환경설정.md` 6절 팀원 PC 첫 교대 체크리스트, 기획서 결정 로그
- 🟡 이 브랜치 PR(docs·skills만) → CI 초록 → 머지

## 바로 다음 할 일

1. 이 PR 머지 (영역 오너). 머지 후 정리 PR은 이제 만들지 않는다
2. `/clear` 후 다음 기능 `/spec`: A 영역 F-06(개요·파일별 설명, LLM 첫 사용) 또는 F-05b(URL 수집·SSRF), B 영역 F-09·F-10
3. 1.4 유스케이스(마감 11/23): `docs/산출물/02-유스케이스.md`에 분석 요청(F-05a) UC 흐름 추가
4. 팀원 B PC에서 `docs/환경설정.md` 6절 체크리스트로 교대 리허설 (사람)

## 막힌 점 / 결정 필요

- 사람: `.claude/settings.json` allow에 `Bash(gh issue create:*)`·`Bash(gh issue list:*)`·`Bash(gh issue view:*)` 추가 (Claude 자기 권한 확장이라 자동 모드가 막음)
- 사람: 이슈 #16(설정 키 키워드 확장 범위), 노션 대시보드 팀원 초대
- 사람: 집 PC 스마트 앱 컨트롤이 oxlint를 막음 → SAC 끄기 또는 wasm 바인딩. FE 커밋 전 필요

## 주의

- 노션은 사람용 요약이다. 정본은 저장소. Notion MCP가 없는 PC에서는 `/progress`·`/handoff`가 노션 단계를 건너뛴다
- 집 PC DB는 새로 만들어 비어 있음(V1~V3 적용). verify(Gradle)를 bootRun과 동시에 돌리지 않는다. V1·V2·V3 수정 금지
- `.env`는 properties 형식으로 읽힌다: 따옴표·끝 공백이 값에 포함됨
