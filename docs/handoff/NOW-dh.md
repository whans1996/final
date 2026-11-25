# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-25 19:29 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/issue-158-config-collect

## 현재 기능

**#158·#159 F-26a 후속** (`feature/issue-158-config-collect`) — 구현·BE 전체 테스트 통과, 커밋 `4cd1e0e`, **PR 전**. F-26a PR #157은 11/25 머지됨

## 진행

- ✅ #158 `ZipArchiveReader.isSupported`: `.cjs` 확장자 대신 `*.config.cjs` 파일명만 받음 (`ZipArchiveReaderTest#AC5_지원_확장자와_파일명`에 eslint.config.cjs·lib.cjs 추가). spec 5.2절 2·결정로그 한 줄
- ✅ #159 `ImportanceRanker.choose`: 전체 정렬 순서로 FE·BE 몫 안에서 고르고(1차) 남는 자리는 몫 무시하고 채움(2차), config는 실제 고른 것만 세어 `max-config`. 테스트 `AC7_config_상한은_FE_BE_몫_안에서_실제로_고를_때_센다`(Codex 재현, red→green)·`AC7_config_상한은_FE_BE를_합쳐_센다`
- 커밋 본문에 `Closes #158`·`Closes #159`

## 바로 다음 할 일

1. `/pr-review` (reviewer → PR 생성은 사용자 확인 후, base develop) → CI·Codex 루프
2. 머지 뒤 develop 정리 → #155는 F-28 화면 때, #156은 F-26b에서
3. 11/27 Figma 세션 → 11/29~ V-1 MVP 감사

## 막힌 점 / 결정 필요

- 사람: GMS 키 수령(12/01 전), 노션 팀원 초대, F-05b liveTest, 제출 zip 이름

## 주의

- 다음 마이그레이션은 V13 · 이 PR은 A 영역 파일만 바꿈(공통 파일 없음)
- 커밋 범위는 F-xx|common|harness만 · Git Bash에서 `/pr-review` 같은 인자는 MSYS 경로 변환됨 (`MSYS_NO_PATHCONV=1`)
- node 스크립트로 Java 편집 시 백슬래시가 깨짐 → Edit 도구 사용 · 이 PC에 python 없음
