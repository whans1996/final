# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 21:57 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-13-admin-concepts

---

## 현재 기능

F-13 관리자 카드 관리 — spec **APPROVED** (김준섭 11-20, `docs/specs/F-13-admin-concepts.md`, AC1~AC15). 브랜치 `feature/F-13-admin-concepts` push됨. 구현 전

## 진행

- ✅ F-04 PR #71 develop 머지됨 (3e1ac61)
- ✅ F-13 spec 초안 + 결정로그 11-20 행: DRAFT만 물리 삭제(공개·숨김은 삭제·DRAFT 되돌리기 409 `CONCEPT_003`), 관리자 작성 카드 `source=ADMIN`(V7), 시드 upsert에서 `status` 제외(숨긴 시드 유지), 시드 카드 이름 변경 400, 이름 중복 409 `CONCEPT_002`, #51 흡수(referenceUrl `^https?://`)
- API: `GET /api/admin/concepts`(status·source·keyword·page), `/status-counts`, `GET·PUT·DELETE /{id}`, `POST` (Tag `관리자`)

## 바로 다음 할 일

1. `/clear` 후 새 세션(Opus/high) `/implement F-13`: `node scripts/next-migration.mjs`로 V7 번호 재확인 → `V7__concept_source_admin.sql` + `ConceptSource.ADMIN` → `concept/admin/AdminConceptApiTest` AC1부터 red

## 막힌 점 / 결정 필요

- 없음 (질문 4개 모두 추천안으로 답 받음)

## 주의

- 공통 파일 변경 예정: `ConceptSource`(ADMIN), `ErrorCode` CONCEPT 블록(002·003), `conventions.md` 3절, `ConceptMapper` B 블록 → PR에 @whans1996 멘션
- `R__seed_concepts.sql` 수정 시 체크섬이 바뀌어 다음 기동 때 한 번 재적용된다
