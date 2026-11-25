// doctor 하네스 무결성 점검 (H-1)
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { crlfFiles, depsStale, hookModeProblems, settingsHookScripts } from '../lib/doctor.mjs';

test('hook 파일이 없거나 실행 권한이 빠졌으면 알린다', () => {
  const out = ['100755 aaa 0\t.githooks/commit-msg', '100644 bbb 0\t.githooks/pre-push'].join('\n');
  assert.deepEqual(hookModeProblems(out), ['.githooks/pre-push 실행 권한 없음']);
  assert.deepEqual(hookModeProblems('100755 aaa 0\t.githooks/commit-msg'), ['.githooks/pre-push 없음']);
  assert.deepEqual(hookModeProblems(['100755 a 0\t.githooks/commit-msg', '100755 b 0\t.githooks/pre-push'].join('\n')), []);
});

test('settings.json hook이 부르는 스크립트 경로', () => {
  const settings = {
    hooks: {
      PreToolUse: [{ matcher: 'Bash', hooks: [{ type: 'command', command: 'node "$CLAUDE_PROJECT_DIR/.claude/hooks/guard-bash.mjs"' }] }],
      Stop: [{ hooks: [{ command: 'node "$CLAUDE_PROJECT_DIR/.claude/hooks/a.mjs"' }, { command: 'echo hi' }] }],
    },
  };
  assert.deepEqual(settingsHookScripts(settings), ['.claude/hooks/a.mjs', '.claude/hooks/guard-bash.mjs']);
  assert.deepEqual(settingsHookScripts({}), []);
});

test('작업 트리만 CRLF인 파일', () => {
  const out = [
    'i/lf    w/crlf  attr/text eol=lf \tfrontend/src/a.js',
    'i/lf    w/lf    attr/text eol=lf \tb.js',
    'i/crlf  w/crlf  attr/ \tc.bat',
    'i/lf    w/crlf  attr/text eol=crlf    \tbackend/gradlew.bat',
  ].join('\n');
  assert.deepEqual(crlfFiles(out), ['frontend/src/a.js']);
});

test('package-lock이 설치 기록보다 새로우면 npm ci가 필요하다', () => {
  assert.ok(depsStale(5000, 1000));
  assert.ok(!depsStale(1000, 5000));
  assert.ok(depsStale(1000, null));
});
