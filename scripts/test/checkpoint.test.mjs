// Stop hook checkpoint-nudge의 체크포인트 누락 판정 (HP-4 AC5, H-11)
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { findMissingCheckpoint, parseCommitLog, sessionTouchedFrom, shouldCheck } from '../lib/checkpoint.mjs';

const ROOT = '/repo';
const NOW = 1_000_000;
const ME = 'me@example.com';
const toolUse = (name, input) => JSON.stringify({ message: { content: [{ type: 'tool_use', name, input }] } });
// 커밋한 파일의 작업 트리 mtime (커밋해도 바뀌지 않는다)
const mtimeOf = () => NOW + 5000;
const transcript = (...lines) => lines.join('\n');
const commit = (over) => ({ sha: 'a'.repeat(40), email: ME, time: NOW + 60_000, merge: false, subject: 'feat(F-01): AC1', files: ['backend/src/main/java/A.java'], ...over });

test('HP-4 AC5: 이 세션이 바꾼 작업 파일이 NOW보다 늦게 미커밋이면 누락 (기존 판정)', () => {
  const touched = sessionTouchedFrom([transcript(toolUse('Edit', { file_path: '/repo/backend/src/main/java/A.java' }))], ROOT);
  const dirty = [{ path: 'backend/src/main/java/A.java', mtime: NOW + 5000 }];
  assert.deepEqual(findMissingCheckpoint({ dirty, commits: [], nowMtime: NOW, myEmail: ME, touched, mtimeOf }), { kind: 'uncommitted', path: 'backend/src/main/java/A.java' });
  // NOW보다 1초 안쪽, 하네스·기획 문서, 다른 세션이 바꾼 파일은 아니다
  assert.equal(findMissingCheckpoint({ dirty: [{ path: 'backend/src/main/java/A.java', mtime: NOW + 500 }], commits: [], nowMtime: NOW, myEmail: ME, touched, mtimeOf }), null);
  assert.equal(findMissingCheckpoint({ dirty: [{ path: 'scripts/lib/a.mjs', mtime: NOW + 5000 }], commits: [], nowMtime: NOW, myEmail: ME, touched: null, mtimeOf }), null);
  assert.equal(findMissingCheckpoint({ dirty: [{ path: 'frontend/src/B.vue', mtime: NOW + 5000 }], commits: [], nowMtime: NOW, myEmail: ME, touched, mtimeOf }), null);
});

test('HP-4 AC5: 이 세션이 고친 파일을 커밋까지 하고 NOW를 갱신하지 않았으면 누락', () => {
  const touched = sessionTouchedFrom([transcript(toolUse('Write', { file_path: 'backend/src/main/java/A.java' }))], ROOT);
  const r = findMissingCheckpoint({ dirty: [], commits: [commit()], nowMtime: NOW, myEmail: ME, touched, mtimeOf });
  assert.deepEqual(r, { kind: 'committed', path: 'backend/src/main/java/A.java', sha: 'a'.repeat(40), subject: 'feat(F-01): AC1' });
  // NOW를 커밋 뒤에 갱신했으면 통과
  assert.equal(findMissingCheckpoint({ dirty: [], commits: [commit({ time: NOW - 1000 })], nowMtime: NOW, myEmail: ME, touched, mtimeOf }), null);
  // 코드 수정 → NOW 갱신 → 커밋(평소 흐름): 커밋은 NOW보다 늦어도 파일은 NOW 전에 고쳤으므로 통과
  assert.equal(findMissingCheckpoint({ dirty: [], commits: [commit()], nowMtime: NOW, myEmail: ME, touched, mtimeOf: () => NOW - 3000 }), null);
  // 커밋에서 지운 파일(mtime 없음)은 판정하지 않는다
  assert.equal(findMissingCheckpoint({ dirty: [], commits: [commit()], nowMtime: NOW, myEmail: ME, touched, mtimeOf: () => null }), null);
});

test('HP-4 AC5: 자동 동기화 merge·상대 커밋·다른 세션 파일·작업 밖 파일 커밋은 막지 않는다', () => {
  const touched = sessionTouchedFrom([transcript(toolUse('Edit', { file_path: 'backend/src/main/java/A.java' }))], ROOT);
  const check = (c) => findMissingCheckpoint({ dirty: [], commits: [commit(c)], nowMtime: NOW, myEmail: ME, touched, mtimeOf });
  assert.equal(check({ merge: true }), null);
  assert.equal(check({ email: 'peer@example.com' }), null);
  // 이메일 대소문자는 무시한다
  assert.notEqual(check({ email: 'ME@Example.com' }), null);
  assert.equal(check({ files: ['backend/src/main/java/Other.java'] }), null);
  assert.equal(check({ files: ['scripts/lib/x.mjs', 'docs/handoff/NOW-dh.md'] }), null);
  // 내 이메일을 모르면 커밋 판정은 하지 않는다
  assert.equal(findMissingCheckpoint({ dirty: [], commits: [commit()], nowMtime: NOW, myEmail: '', touched, mtimeOf }), null);
});

test('HP-4 AC5: transcript를 못 읽으면 모든 파일을 이 세션 것으로, 여러 파일을 바꾸는 명령도 전부로 본다', () => {
  assert.equal(sessionTouchedFrom([null], ROOT), null);
  assert.equal(findMissingCheckpoint({ dirty: [], commits: [commit()], nowMtime: NOW, myEmail: ME, touched: null, mtimeOf })?.kind, 'committed');
  const broad = sessionTouchedFrom([transcript(toolUse('Bash', { command: 'cd frontend && npm run lint' }))], ROOT);
  assert.equal(broad('frontend/src/B.vue'), true);
  // 셸 명령에 경로 뒷부분이나 고유한 파일 이름이 있으면 건드린 것
  const sh = sessionTouchedFrom([transcript(toolUse('Bash', { command: "sed -i 's/a/b/' src/views/Home.vue" })), toolUse('Bash', { command: 'cat package.json' })], ROOT);
  assert.equal(sh('frontend/src/views/Home.vue'), true);
  assert.equal(sh('frontend/package.json'), false);
  // 서브에이전트 transcript도 본다
  const sub = sessionTouchedFrom([transcript(), transcript(toolUse('Edit', { file_path: 'docs/specs/F-01-x.md' }))], ROOT);
  assert.equal(sub('docs/specs/F-01-x.md'), true);
});

test('HP-4 AC5: stop_hook_active(이미 한 번 막힘)와 계획 모드는 검사하지 않는다', () => {
  assert.equal(shouldCheck({}), true);
  assert.equal(shouldCheck({ stop_hook_active: true }), false);
  assert.equal(shouldCheck({ permission_mode: 'plan' }), false);
});

test('HP-4 AC5: git log 출력(커밋 머리 + 바뀐 파일) 해석', () => {
  const out = ['\x1eaaa\x1fme@example.com\x1f1700000000\x1fp1\x1ffeat(F-01): AC1', '', 'backend/A.java', 'docs/specs/F-01.md', '\x1ebbb\x1fme@example.com\x1f1700000100\x1fp1 p2\x1fMerge branch develop', ''].join('\n');
  assert.deepEqual(parseCommitLog(out), [
    { sha: 'aaa', email: 'me@example.com', time: 1_700_000_000_000, merge: false, subject: 'feat(F-01): AC1', files: ['backend/A.java', 'docs/specs/F-01.md'] },
    { sha: 'bbb', email: 'me@example.com', time: 1_700_000_100_000, merge: true, subject: 'Merge branch develop', files: [] },
  ]);
  assert.deepEqual(parseCommitLog(''), []);
});
