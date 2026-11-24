# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-24 13:25 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/common-erd-v10

---

## 현재 기능

**산출물 ERD 갱신** (`docs/산출물/03-ERD.md`, `feature/common-erd-v10`, develop a2c3215에서 분기). 기능 아님, 문서 PR

## 진행

- ✅ PR #102(#96)·#103(#92) 머지 확인, #92·#96·#101 닫힘
- ✅ 03-ERD.md를 Flyway V1~V10 기준으로 다시 씀
  - 도메인별 mermaid erDiagram 3개(회원·소셜·커뮤니티 / 분석·카드·학습 / 챌린지·팀, 26테이블) — mermaid 11.4.1 파서로 3개 모두 통과
  - 테이블 설명 보강(users 탈퇴 처리, activities 다형 참조, posts #92, comments, challenges·participants F-24·25 규칙, llm_usage_logs), 마이그레이션 이력 표, 정규화 메모(개인 챌린지 저장 vs 팀 목표 계산, 탈퇴와 FK)
  - 머리말: DDL 정본은 migration, 기획서 3절 ERD는 기획 단계 기록(V7 이후 미반영)으로 표현을 바꿈

## 바로 다음 할 일

1. PR 열기 (docs 전용, CI·Codex 확인) → 사람이 GitHub에서 머지
2. 그다음 후보: WBS 1.8·4.3·5.1·5.2·5.4 산출물 초안(최종본은 기능 확정 뒤), B 영역 이슈 없음. SC-01 랜딩·디자인 기준은 F-25 머지됐으니 2차 기획 시작 가능 (사용자 합의 11-24)

## 막힌 점 / 결정 필요

- 없음

## 주의

- 최종 ERD 이미지(`diagrams/erd.png`, Workbench Reverse Engineer)는 W5에 붙인다
