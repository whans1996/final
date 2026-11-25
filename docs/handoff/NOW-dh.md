# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-25 21:04 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/F-27-gms-provider

## 현재 기능

**F-27 GMS 공급자** — spec `docs/specs/F-27-gms-provider.md` **APPROVED** (whans1996 11-25), 브랜치 `feature/F-27-gms-provider`. 구현 전

## 진행

- ✅ PR #160(#158·#159) 11/25 머지, 이슈 자동 닫힘 확인 → develop 최신 `48bb806`
- ✅ GMS 키 수령 → dh-home `.env`에 `GMS_API_KEY` 추가 (dh-ssafy는 아직 없음)
- ✅ F-27 spec 초안 AC1~AC13 + 결정로그 1행(gemini 단독·liteMode 배너만·Ollama에도 스키마·구현 중 최소 실호출 허용)

## 바로 다음 할 일

1. `/clear` 후 `/implement F-27` (Opus·high): V13 `llm_usage_logs.provider` → `LlmSchemasTest`(AC7) red → `llm-schemas/*.json` 6개 → `GmsLlmClientTest`(AC3~5) → `LlmConfig`(AC1·2·4)
2. 구현 중 GMS 최소 실호출(`LlmLiveTest`, `@Tag("live")`)로 기본 모델·base URL 확정 → spec 9절 정리

## 막힌 점 / 결정 필요

- spec 9절: GMS 기본 모델·base URL(실연결로 확정), 키별 사용 한도 유무
- 사람: 노션 팀원 초대, F-05b liveTest, 제출 zip 이름, dh-ssafy `.env`에 GMS 키

## 주의

- 다음 마이그레이션은 V13 (이 spec이 사용) · F-27은 A 영역 파일만 바꿀 예정(공통 없음)
- 커밋 범위는 F-xx|common|harness만 · Git Bash에서 `/pr-review` 같은 인자는 MSYS 경로 변환됨 (`MSYS_NO_PATHCONV=1`)
- node 스크립트로 Java 편집 시 백슬래시가 깨짐 → Edit 도구 사용 · 이 PC에 python 없음
