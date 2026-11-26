// commit-msg hook 판정 (#136 보호 브랜치 검사를 자동 메시지 예외보다 먼저)
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { commitVerdict } from '../lib/commit-msg.mjs';

const v = (subject, o = {}) => commitVerdict({ subject, branch: 'feature/x', hasCommits: true, merging: false, ...o });

test('보호 브랜치: merge 진행 중과 첫 커밋만 예외, 자동 메시지 제목도 막는다', () => {
  assert.ok(v('Merge branch x', { branch: 'develop' }).fail);
  assert.ok(v('Revert "fix(F-01): a"', { branch: 'main' }).fail);
  assert.ok(v('fix(F-01): a', { branch: 'develop' }).fail);
  assert.deepEqual(v("Merge remote-tracking branch 'origin/develop'", { branch: 'develop', merging: true }), { fail: null, verify: false });
  assert.equal(v('chore: init', { branch: 'main', hasCommits: false }).fail, null);
});

test('형식: 자동 메시지는 형식 검사·빠른 검사 생략, wip는 빠른 검사만 생략', () => {
  assert.deepEqual(v('Merge branch develop'), { fail: null, verify: false });
  assert.deepEqual(v('fixup! feat(F-01): a'), { fail: null, verify: false });
  assert.deepEqual(v('wip(F-01): 중간'), { fail: null, verify: false });
  assert.deepEqual(v('feat(F-01): a'), { fail: null, verify: true });
  assert.deepEqual(v('docs: README'), { fail: null, verify: true });
  assert.ok(v('잘못된 메시지').fail.some((l) => l.includes('형식')));
  assert.ok(v('feat: 범위 없음').fail);
});
