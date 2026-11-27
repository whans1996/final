# data/ — 외부·내부 데이터 관리

| 경로 | 내용 | git |
|---|---|---|
| `datasets.json` | 데이터 목록(정본): id, 출처, 라이선스, 고정 커밋 SHA, 분할(split) | 커밋 |
| `raw/` | 외부 저장소 아카이브 원본 (`datasets.json`의 SHA로 다시 받는다) | **gitignore** |

상세 계획(14개 항목, 분리, 누수 검토)은 [`docs/산출물/09-데이터셋.md`](../docs/산출물/09-데이터셋.md)에 있다.

## 규칙

1. **새 외부 데이터는 쓰기 전에 등록한다**: 라이선스를 공식 출처(LICENSE 파일, 데이터 카드, 약관)에서 확인하고 `datasets.json`과 계획서에 먼저 추가한다.
2. **split을 지킨다**
   - `eval`: 평가 전용. 프롬프트, few-shot 예시, Mock fixture, 시드 카드 예제 코드에 **절대 쓰지 않는다**. 프롬프트를 조정하는 사람은 eval 저장소의 코드를 열어 보지 않는다.
   - `dev`: 프롬프트·fixture·통합 테스트 개발에 자유롭게 쓴다.
   - `demo`: 시연용. 프롬프트를 이 저장소에 맞춰 다듬어도 된다 (평가 점수로 보고하지 않음).
3. **SHA를 고정한다**: 내려받기는 항상 `commit`에 적힌 SHA로 한다. SHA를 바꾸면 계획서 변경 이력에 남긴다.
4. **라이선스 고지**: 제출 zip에 서드파티 코드를 넣을 때는 원본 LICENSE(Apache-2.0은 NOTICE 포함)와 출처 URL·SHA를 함께 넣는다.
5. 라이선스가 없는 저장소(예: spring-petclinic-vue)는 쓰지 않는다. LICENSE가 없으면 저작권이 그대로 유지된다.

외부 저장소는 프롬프트 평가 러너(`.claude/agents/prompt-evaluator.md`, `-Pfixture=<id>`)가 `datasets.json`의 SHA로 codeload에서 받아 `.harness/prompt-eval/repos/`에 캐시한다. 별도 내려받기 스크립트(`scripts/fetch-datasets.mjs`)는 만들지 않았다.
