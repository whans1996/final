---
name: mr-review
description: develop에 합치기 전 현재 feature 브랜치를 reviewer·contract-checker·verifier로 점검하고, 지적 사항을 반영한 뒤 GitLab MR 설명을 만든다. "MR 올리자", "머지 전 리뷰", "/mr-review" 요청 시 사용.
---

# /mr-review — MR 전 리뷰

## 절차

1. **범위 파악**: `git fetch` 후 `git diff --stat origin/develop...HEAD` (원격이 없으면 `develop...HEAD`). 브랜치 이름에서 F-xx를 뽑아 spec을 찾는다.
2. **병렬 점검** — 세 에이전트를 한 번에 실행한다:
   - `verifier`: 전체(`all`) 검증
   - `reviewer`: diff + spec 기준 리뷰
   - `contract-checker`: 이 기능의 API 계약 일치 여부 (API 변경이 없으면 생략)
3. **분류**: 결과를 `반드시 수정`(spec 미충족, 보안, 테스트 실패, 계약 불일치) / `권장` / `무시 가능`으로 나눠 사용자에게 보여 준다.
4. **수정**: `반드시 수정`은 바로 고치고 커밋 (`fix(F-xx): ...`). `권장`은 사용자에게 할지 묻는다.
5. **MR 설명 생성**: `.gitlab/merge_request_templates/Default.md` 형식으로 채워 출력하고, `.harness/mr-F-xx.md`에도 저장한다.
   - 수용 기준 표: AC별 테스트 이름과 결과
   - FE 변경이 있으면 `.harness/screens/`의 스크린샷 경로
   - 리뷰어(영역 오너)가 특히 볼 곳
6. spec 상태는 DONE으로 바꾸지 않는다 (MR이 머지된 뒤 사람이 바꾸거나 다음 `/progress`에서 확인).
7. push하고, 사용자에게 GitLab에서 MR을 만들라고 안내한다 (대상 브랜치: develop, 리뷰어: 영역 오너).

## 주의

- wip 커밋이 섞여 있으면 MR 머지 시 squash를 권장한다고 MR 설명에 적는다.
