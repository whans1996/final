# API 명세 (자동 생성)

- `openapi.json`: 백엔드 테스트 `OpenApiExportTest`가 실제 앱의 `/v3/api-docs`를 받아 키를 정렬해 저장한다. **손으로 고치지 않는다.**
- API를 바꾸면 테스트를 돌려 갱신된 `openapi.json`을 같은 커밋에 포함한다. CI가 코드와 다르면 PR을 실패시킨다.
- `contract-checker` 에이전트가 이 파일을 백엔드 API의 정본으로 사용한다.
- 제출용 사람이 읽는 명세는 `docs/산출물/07-REST-API-명세.md`로 생성한다.

> 아직 백엔드 프로젝트가 없어 `openapi.json`이 없다. 프로젝트 뼈대(F-00)에서 생성된다.
