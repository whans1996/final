# 바이브노트 (VibeNote)

> AI로 코딩하는 주니어와 개발자 지망생을 위해, AI가 쓴 코드를 **내 수준에 맞는 구조 설명과 개념 카드**로 풀어 주고, **퀴즈와 복습**으로 진짜 내 지식으로 만드는 코드 이해 학습 플랫폼

SSAFY Final Project (Java Track, 자율 주제) · 2026-11-17 ~ 2026-12-24

<!-- 제출 시 채움: 팀 이름, 지역·반, 팀원 이름과 역할, 시연 영상·배포 주소 -->

## 서비스 소개

| 문제 | 바이브노트의 해결 |
|---|---|
| AI가 만든 코드가 동작해도 구조·흐름·설계 의도를 모른다 | 저장소 URL이나 ZIP을 올리면 **수준(입문·주니어·숙련)에 맞춘** 개요, 파일별 설명, API 실행 흐름을 보여 준다 |
| 설명을 들어도 일회성으로 끝난다 | 코드에서 추출한 **개념 카드**가 쌓이고, 내 코드로 만든 **퀴즈·복습 계획·맞춤 추천·학습 챌린지**로 이어진다 |
| 혼자 이해하기 어렵다 | **커뮤니티**(Q&A·학습 후기·자유)에 분석·카드를 첨부해 질문하고, **팔로우·피드**로 함께 공부한다 |

## 기술 스택

| 구분 | 기술 |
|---|---|
| Back-End | Java 21, Spring Boot 4, Spring Security + JWT, MyBatis, MySQL 8, Flyway, Swagger(springdoc-openapi), JavaParser(구조 분석) |
| Front-End | Vue 3, Vite, Pinia, Vue Router, Axios, mermaid(구조 다이어그램) |
| AI | Gemini API (기본), Ollama (폴백), Mock (개발·테스트) |
| 테스트·품질 | JUnit 5, MockMvc, Testcontainers(MySQL), Vitest, Playwright, GitHub Actions |
| 협업·개발 도구 | GitHub(개발), lab.ssafy.com GitLab(제출), Claude Code 하네스 |

## 산출물

| 구분 | 산출물 | 위치 |
|---|---|---|
| 설계 문서 | 요구사항 정의서 | [docs/산출물/01-요구사항정의서.md](docs/산출물/01-요구사항정의서.md) |
| | Use-Case Diagram | [docs/산출물/02-유스케이스.md](docs/산출물/02-유스케이스.md) |
| | ER Diagram | [docs/산출물/03-ERD.md](docs/산출물/03-ERD.md) |
| | WBS | [docs/산출물/04-WBS.md](docs/산출물/04-WBS.md) |
| | Gantt Chart | [docs/산출물/05-간트차트.md](docs/산출물/05-간트차트.md) |
| | 화면 설계서 | [docs/산출물/06-화면설계서.md](docs/산출물/06-화면설계서.md) |
| 구현 결과물 | Back-End (Spring Boot) 소스 | [backend/](backend/) |
| | Front-End (Vue.js) 소스 | [frontend/](frontend/) |
| | DB Schema.sql | [docs/산출물/Schema.sql](docs/산출물/Schema.sql) (Flyway V 파일에서 생성, `node scripts/export-schema.mjs`) |
| | 활용 데이터 셋 | [docs/산출물/09-데이터셋.md](docs/산출물/09-데이터셋.md) |
| | REST API 명세 (Swagger) | [docs/산출물/07-REST-API-명세.md](docs/산출물/07-REST-API-명세.md) (openapi.json에서 생성, `node scripts/export-api-doc.mjs`) |
| 결과 정리 자료 | 최종 보고서 | [docs/산출물/10-최종보고서.md](docs/산출물/10-최종보고서.md) |
| | 발표 자료 (PPT/PDF) | (제출 단계에서 추가) |
| | 외부 API 및 생성형 AI 활용 | [docs/산출물/08-외부API-생성형AI-활용.md](docs/산출물/08-외부API-생성형AI-활용.md) |

기획 전체(기능 명세, 아키텍처, 일정, 역할 분담)는 [docs/기획서.md](docs/기획서.md)에, 결정 기록은 [docs/결정로그.md](docs/결정로그.md)에 있다.

## 실행 방법

### 사전 준비
- JDK 17+(`JAVA_HOME`, 컴파일용 JDK 21은 첫 빌드 때 Gradle이 자동으로 받음), Node 24, MySQL 8, Docker Desktop(WSL2, 테스트용 Testcontainers) — 설치 절차: [docs/환경설정.md](docs/환경설정.md)
- 최초 1회 (PC마다): `node scripts/setup.mjs --pc <PC id>` (PC 등록, git hook 연결, 환경 점검. PC id는 [docs/환경설정.md](docs/환경설정.md) 0절)
- 로컬 DB `vibenote` 생성과 `.env` 작성 (`.env.example` 복사): [docs/환경설정.md](docs/환경설정.md) 3절

### Back-End
```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'   # LLM은 mock 모드
```
- Spring Boot 4.1 · Java 21 · Gradle 9.9 (wrapper). 시작할 때 Flyway가 스키마를 자동 적용한다.
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- 테스트: `./gradlew test` (Docker Desktop 실행 필요, Testcontainers MySQL 8.4)

### Front-End
```bash
cd frontend
npm install
npm run dev   # http://localhost:5173 (/api는 8080으로 프록시)
```
- 테스트·린트: `npm run test:unit -- --run`, `npm run lint:check`

## 개발 방식: Claude Code 하네스

2인이 PC 4대에서 AI 코딩 도구로 **영역별 병렬 개발**(같은 사람의 PC 사이에서는 교대)을 하기 위해, AI가 같은 규칙으로 일하고 스스로 검증하며 실수가 기계적으로 차단되도록 개발 환경을 설계했다.

| 층 | 구성 |
|---|---|
| 컨텍스트 | [CLAUDE.md](CLAUDE.md), [docs/conventions.md](docs/conventions.md), 기능별 spec, 사람별 현재 상태 `docs/handoff/NOW-dh.md`·`NOW-js.md`, 결정 기록 `docs/결정로그.md` |
| 워크플로 | `/pickup` → `/spec` → `/implement`(TDD) → `/pr-review` → `/handoff` |
| 가드레일 | Claude Code hooks(셸 토큰 파서 가드) + git hook (보호 브랜치, 비밀 정보, 마이그레이션, 커밋 규칙, 커밋 빠른 검사·pre-push 전체 검증) |
| 피드백 | 검증 스크립트, 화면 점검, Testcontainers, GitHub Actions CI, Swagger 계약 검사 |
| 서브에이전트 | verifier, reviewer, contract-checker, ui-verifier, prompt-evaluator |

자세한 내용과 사례: [외부 API 및 생성형 AI 활용 3절](docs/산출물/08-외부API-생성형AI-활용.md)
