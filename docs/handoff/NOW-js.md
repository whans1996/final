# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 12:02 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-17-community-board

---

## 현재 기능

F-17 커뮤니티 게시판 — spec `docs/specs/F-17-community-board.md` APPROVED, `/implement` 진행 중 (이번 세션 목표: BE AC1~12 → FE AC13~18)

## 진행

- ✅ 9절 결정: `PUT`은 `@Valid` 없이 `PostService.update`가 404·403 뒤 `Validator`로 직접 검증 (spec 9절에 기록)
- ✅ AC1~3 목록·검색 green: `com.vibenote.post` (`PostController#list`, `PostService#search`, `PostMapper.xml` findPage·count), `PostListApiTest` 6개 + `PostServiceTest`, `PUBLIC_GET_PATHS` POST 블록
- ✅ AC4·5 상세 green: `PostService#getDetail`(조회수 +1, `updated_at` 유지), `PostDetailResponse`·첨부 DTO 2개, `ErrorCode` POST 블록 4개(POST_001·002·403·404 미리 추가), `PostDetailApiTest` 7개
- 테스트 공통 데이터: `backend/src/test/java/com/vibenote/post/PostApiTestBase.java`

## 바로 다음 할 일

1. AC6~8 작성: `PostWriteApiTest#AC6_...`~`#AC8_...` red → `PostRequest`(boardType 문자열 @Pattern, title·content strip), `PostService#create`(getActiveMember → 첨부 둘 다 400 details.conceptId → `PostMapper.existsAttachableAnalysis`/`existsPublishedConcept` → insert → `ActivityService.record(POST)`)
2. AC9~11 수정·삭제(`Validator` 직접 호출) → AC12 인증(`PostAuthApiTest`) → FE. 커밋 전 `OpenApiExportTest`로 openapi.json 재생성해 같이 커밋

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 파일 변경: `SecurityConfig.PUBLIC_GET_PATHS`(POST 블록), 곧 `ErrorCode` POST 블록 → PR 본문에서 @whans1996 멘션
- keyword 이스케이프는 `ConceptService.escapeLike`와 같은 한 줄을 `PostService.escapeLike`로 복제 (공용 유틸로 빼지 않음 → 공통 코드 변경 없음)
- F-15(동환) 아직 PR 전 → F-12 찜(#57 `addWish` 연동)은 F-15 머지 뒤
- SC-07 "커뮤니티에 질문하기" 버튼은 구현 PR에서 follow-up 이슈(area:analysis, whans1996)로 만든다
- `codex-review.mjs`가 Codex 실행 실패를 RUNNING으로 기다린다 (#48)
