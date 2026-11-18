# 노션 팀 대시보드

사람이 보는 요약과 발표·회고 소재를 모으는 곳이다. **정본은 이 저장소**이고 Claude는 저장소만 읽는다. 노션과 저장소가 다르면 저장소가 맞다.

| 페이지 | URL | 누가 갱신 |
|---|---|---|
| 바이브노트 팀 대시보드 (팀 홈·링크·진행 현황) | https://app.notion.com/p/41cce1244b8381649430c6950dead289 | `/progress` (진행 현황 섹션) |
| AI 활용·트러블슈팅 사례 (DB) | https://app.notion.com/p/41cce1244b838115aaf8e14f955137aa | `/handoff` (작업로그 `AI 활용` 줄), 사람이 "발표 후보" 체크 |
| 회의·결정 노트 (DB) | https://app.notion.com/p/41cce1244b83813784edfa457602e951 | 사람 |

- 사례 DB data source: `collection://41cce124-4b83-81d1-9b07-000b51cceeeb` (속성: 제목, 날짜, 분류, 기능, 브랜치·PR, 내용, 발표 후보)
- 회의 DB data source: `collection://41cce124-4b83-8151-bf88-000b0ad4cf45`

## 운영 규칙

1. Notion MCP는 선택이다. 연결되지 않은 PC에서는 스킬이 노션 단계를 건너뛰고, 다음에 연결된 PC에서 `/progress`를 돌리면 따라잡는다.
2. 회의·결정 노트의 결정 중 기획·범위·일정을 바꾸는 것은 `docs/기획서.md` 결정 로그로 옮기고 "기획서 결정 로그 반영"을 체크한다.
3. 할 일·후속 작업은 노션이 아니라 GitHub Issues(`follow-up` 라벨)에 둔다.
4. 팀원 초대와 공유 설정은 사람이 노션에서 직접 한다.
