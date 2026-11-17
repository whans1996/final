# REST API 명세 — 바이브노트

> 작성 상태: ⬜ 골격 · 최종본 W5(WBS 5.3)
> **정본은 Swagger**다. 실행 중이면 http://localhost:8080/swagger-ui/index.html, 파일은 [`docs/api/openapi.json`](../api/) (테스트 `OpenApiExportTest`가 생성, CI가 코드와 일치 여부 검사).
> 이 문서는 제출용으로 openapi.json에서 자동 생성한다 (제출 단계 스크립트 `scripts/export-api-doc.mjs`, WBS 5.3에서 작성). 손으로 고치지 않는다.

## 공통 규칙

- 응답·에러 형식, 에러 코드 체계, 페이지네이션, 인증 헤더: [`docs/conventions.md`](../conventions.md)
- 인증: `Authorization: Bearer <accessToken>` (Swagger의 Authorize 버튼에 Access 토큰 입력)

## 엔드포인트 목록

(openapi.json에서 생성: Tag별 메서드 · URL · 요약 · 권한 · 요청 · 응답 · 에러 코드)

설계 단계 목록은 `docs/기획서.md` 3절 "주요 API"를 참고한다.
