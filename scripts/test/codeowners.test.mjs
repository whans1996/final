// HX-3: 코드·하네스 경로의 모든 추적 파일은 CODEOWNERS에 주인이 있어야 한다.
// 새 도메인 패키지·FE 폴더를 만들고 CODEOWNERS에 줄을 빠뜨리면 영역 알림(area-notice)과 PR @멘션이 조용히 빠진다.
// docs·루트 설정 파일은 공통 문서라 주인을 두지 않는다 (두면 모든 문서 PR이 상대를 @멘션한다).
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { git } from '../lib/common.mjs';
import { loadOwnerRules, ownersOf } from '../lib/owners.mjs';

const CODE_PATHS = /^(backend\/src|frontend\/src|scripts|\.claude|\.githooks|\.github)\//;

test('코드·하네스 경로의 추적 파일은 모두 CODEOWNERS 주인이 있다', () => {
  const ls = git(['-c', 'core.quotepath=off', 'ls-files', '-z']);
  assert.ok(ls.ok, ls.err);
  const rules = loadOwnerRules();
  assert.ok(rules.length > 0, 'CODEOWNERS 규칙이 없음');
  const unowned = ls.out
    .split('\0')
    .filter((f) => CODE_PATHS.test(f))
    .filter((f) => !ownersOf(f, rules).length);
  assert.deepEqual(unowned, [], `CODEOWNERS에 주인이 없는 파일 → .github/CODEOWNERS에 영역 줄을 추가하세요:\n${unowned.join('\n')}`);
});
