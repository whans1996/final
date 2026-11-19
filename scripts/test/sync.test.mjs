// 자동 동기화: 할 일 고르기(순수 함수)와 임시 저장소에서의 실제 병합·충돌·보류
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { after, test } from 'node:test';
import { autoSync, mergeNotes, syncPlan } from '../lib/sync.mjs';

// git hook(커밋 테스트 게이트) 안에서 돌면 GIT_DIR·GIT_INDEX_FILE 등이 실제 저장소를 가리킨다. 그대로 두면 아래 임시
// 저장소용 git 명령이 실제 저장소의 index·설정을 건드리므로 모두 지운다 (autoSync도 process.env를 물려받는다).
// 이 삭제는 import 평가 뒤에 일어나므로 common.mjs·sync.mjs는 import 시점에 git을 실행하지 않아야 한다.
for (const key of Object.keys(process.env)) if (key.startsWith('GIT_')) delete process.env[key];
const tmp = mkdtempSync(join(tmpdir(), 'vibenote-sync-'));
after(() => rmSync(tmp, { recursive: true, force: true }));
// 개발 PC의 전역·시스템 git 설정(commit.gpgsign, merge.ff 등)이 결과를 바꾸지 않게 빈 설정을 쓴다
const EMPTY_GLOBAL = join(tmp, 'empty-gitconfig');
writeFileSync(EMPTY_GLOBAL, '');
process.env.GIT_CONFIG_GLOBAL = EMPTY_GLOBAL;
process.env.GIT_CONFIG_NOSYSTEM = '1';

test('받을 커밋이 없으면 할 일이 없다', () => {
  const plan = syncPlan({ branch: 'feature/F-09-x', upstream: { ref: 'origin/feature/F-09-x', ahead: 1, behind: 0 }, developBehind: 0 });
  assert.deepEqual(plan, { actions: [], blocked: null });
});

test('feature 브랜치는 내 원격을 먼저, develop을 다음에 합친다', () => {
  const plan = syncPlan({ branch: 'feature/F-09-x', upstream: { ref: 'origin/feature/F-09-x', ahead: 0, behind: 2 }, developBehind: 3 });
  assert.deepEqual(
    plan.actions.map((a) => [a.ref, a.ffOnly]),
    [
      ['origin/feature/F-09-x', false],
      ['origin/develop', false],
    ],
  );
});

test('upstream이 origin/develop이면 한 번만 합친다', () => {
  const plan = syncPlan({ branch: 'feature/x', upstream: { ref: 'origin/develop', ahead: 0, behind: 2 }, developBehind: 2 });
  assert.deepEqual(
    plan.actions.map((a) => a.ref),
    ['origin/develop'],
  );
});

test('develop은 feature/·fix/ 브랜치에만 합친다', () => {
  const at = (branch) => syncPlan({ branch, upstream: null, developBehind: 2 }).actions.map((a) => a.ref);
  assert.deepEqual(at('feature/F-09-x'), ['origin/develop']);
  assert.deepEqual(at('fix/F-09-x'), ['origin/develop']);
  assert.deepEqual(at('experiment'), []);
  assert.deepEqual(at('main'), []);
});

test('보호 브랜치는 fast-forward만 하고 develop을 합치지 않는다', () => {
  const plan = syncPlan({ branch: 'main', upstream: { ref: 'origin/main', ahead: 0, behind: 1 }, developBehind: 5 });
  assert.deepEqual(
    plan.actions.map((a) => [a.ref, a.ffOnly]),
    [['origin/main', true]],
  );
});

test('커밋 안 된 변경이 있거나 병합 중이면 미룬다', () => {
  const base = { branch: 'feature/x', upstream: null, developBehind: 1 };
  assert.match(syncPlan({ ...base, dirty: [' M a.txt'] }).blocked, /커밋 안 된 변경 1개/);
  assert.match(syncPlan({ ...base, merging: true }).blocked, /병합이 진행 중/);
  assert.deepEqual(syncPlan({ ...base, dirty: [' M a.txt'], developBehind: 0 }), { actions: [], blocked: null });
  assert.deepEqual(syncPlan({ branch: null, developBehind: 3 }).actions, []);
});

test('병합으로 들어온 마이그레이션과 openapi.json을 알린다', () => {
  assert.deepEqual(mergeNotes(['backend/src/main/resources/db/migration/V6__quiz.sql', 'docs/api/openapi.json', 'a.js']), [
    '새 마이그레이션 V6__quiz.sql',
    'openapi.json 변경',
  ]);
  assert.deepEqual(mergeNotes(['a.js']), []);
});

// ── 임시 저장소 ──

// 개발 PC의 git hook(테스트 게이트 등)이 임시 저장소에서 돌지 않게 빈 폴더를 hooksPath로 쓴다 (Windows 호환)
const NO_HOOKS = join(tmp, 'no-hooks');
mkdirSync(NO_HOOKS);

const ENV = { ...process.env, GIT_AUTHOR_NAME: 't', GIT_AUTHOR_EMAIL: 't@t', GIT_COMMITTER_NAME: 't', GIT_COMMITTER_EMAIL: 't@t' };
function sh(cwd, ...args) {
  const r = spawnSync('git', ['-c', `core.hooksPath=${NO_HOOKS}`, '-c', 'init.defaultBranch=main', ...args], { cwd, encoding: 'utf8', env: ENV });
  assert.equal(r.status, 0, `git ${args.join(' ')}: ${r.stderr}`);
  return r.stdout.trim();
}
function commit(cwd, file, content, msg) {
  writeFileSync(join(cwd, file), content);
  sh(cwd, 'add', file);
  sh(cwd, 'commit', '-q', '-m', msg);
}

let n = 0;
/** origin(bare) + 상대 clone(peer) + 내 clone(me, feature 브랜치) */
function setup() {
  const dir = join(tmp, String(++n));
  const origin = join(dir, 'origin.git');
  sh(tmp, 'init', '-q', '--bare', origin);
  const peer = join(dir, 'peer');
  sh(tmp, 'clone', '-q', origin, peer);
  commit(peer, 'shared.txt', 'base\n', 'init');
  sh(peer, 'push', '-q', 'origin', 'HEAD:main');
  sh(peer, 'push', '-q', 'origin', 'HEAD:develop');
  const me = join(dir, 'me');
  sh(tmp, 'clone', '-q', origin, me);
  sh(me, 'switch', '-q', '-c', 'feature/F-09-x', 'origin/develop');
  sh(me, 'push', '-q', '-u', 'origin', 'feature/F-09-x');
  sh(me, 'config', 'user.name', 't');
  sh(me, 'config', 'user.email', 't@t');
  sh(me, 'config', 'core.hooksPath', NO_HOOKS);
  return { peer, me };
}
function peerMergesToDevelop(peer, file, content) {
  sh(peer, 'fetch', '-q');
  sh(peer, 'switch', '-q', '-C', 'develop', 'origin/develop');
  commit(peer, file, content, `feat: ${file}`);
  sh(peer, 'push', '-q', 'origin', 'develop');
}

test('상대가 develop에 머지한 커밋을 내 feature 브랜치에 합친다', () => {
  const { peer, me } = setup();
  commit(me, 'mine.txt', 'mine\n', 'feat: mine');
  peerMergesToDevelop(peer, 'theirs.txt', 'theirs\n');
  sh(me, 'fetch', '-q');

  const lines = autoSync({ cwd: me });

  assert.match(lines[0], /origin\/develop의 새 커밋 1개를 자동으로 합쳤습니다/);
  assert.match(lines.at(-1), /push하지 않았습니다/);
  assert.equal(sh(me, 'rev-list', '--count', 'HEAD..origin/develop'), '0');
  assert.match(sh(me, 'log', '-1', '--format=%s'), /^Merge /);
});

test('다른 PC가 push한 내 브랜치 커밋을 받는다 (fast-forward)', () => {
  const { me } = setup();
  const other = join(me, '..', 'other-pc');
  sh(tmp, 'clone', '-q', '-b', 'feature/F-09-x', join(me, '..', 'origin.git'), other);
  commit(other, 'pc2.txt', 'pc2\n', 'feat: from other pc');
  sh(other, 'push', '-q', 'origin', 'feature/F-09-x');
  sh(me, 'fetch', '-q');

  const lines = autoSync({ cwd: me });

  assert.match(lines[0], /원격 origin\/feature\/F-09-x의 새 커밋 1개/);
  assert.ok(!lines.some((l) => l.includes('push하지 않았습니다')), 'fast-forward는 push할 것이 없다');
  assert.equal(sh(me, 'log', '-1', '--format=%s'), 'feat: from other pc');
});

test('충돌하면 되돌리고 충돌 파일을 알린다', () => {
  const { peer, me } = setup();
  commit(me, 'shared.txt', 'mine\n', 'feat: mine');
  peerMergesToDevelop(peer, 'shared.txt', 'theirs\n');
  sh(me, 'fetch', '-q');
  const head = sh(me, 'rev-parse', 'HEAD');

  const lines = autoSync({ cwd: me });

  assert.match(lines[0], /충돌\(shared\.txt\)이 나서 되돌렸습니다/);
  assert.equal(sh(me, 'rev-parse', 'HEAD'), head);
  assert.equal(spawnSync('git', ['rev-parse', '-q', '--verify', 'MERGE_HEAD'], { cwd: me }).status, 1, '병합 상태가 남지 않는다');
  assert.equal(sh(me, 'status', '--porcelain'), '');
});

test('커밋 안 된 변경이 있으면 합치지 않고 미룬다', () => {
  const { peer, me } = setup();
  peerMergesToDevelop(peer, 'theirs.txt', 'theirs\n');
  sh(me, 'fetch', '-q');
  writeFileSync(join(me, 'shared.txt'), 'editing\n');
  const head = sh(me, 'rev-parse', 'HEAD');

  const lines = autoSync({ cwd: me });

  assert.match(lines[0], /커밋 안 된 변경 1개가 있어 자동 병합을 미뤘습니다/);
  assert.equal(sh(me, 'rev-parse', 'HEAD'), head);
});

test('git config vibenote.autoSync false면 아무것도 하지 않는다', () => {
  const { peer, me } = setup();
  peerMergesToDevelop(peer, 'theirs.txt', 'theirs\n');
  sh(me, 'fetch', '-q');
  sh(me, 'config', 'vibenote.autoSync', 'false');

  assert.deepEqual(autoSync({ cwd: me }), []);
  assert.equal(sh(me, 'rev-list', '--count', 'HEAD..origin/develop'), '1');
});

test('다른 PC가 develop을 합쳐 push했으면 내 원격만 받고 develop을 "0개 합침"으로 보고하지 않는다', () => {
  const { peer, me } = setup();
  peerMergesToDevelop(peer, 'theirs.txt', 'theirs\n');
  // 같은 사람의 다른 PC: develop을 합쳐 내 브랜치에 push
  const other = join(me, '..', 'other-pc');
  sh(tmp, 'clone', '-q', '-b', 'feature/F-09-x', join(me, '..', 'origin.git'), other);
  sh(other, 'config', 'core.hooksPath', NO_HOOKS);
  sh(other, 'merge', '-q', '--no-edit', 'origin/develop');
  sh(other, 'push', '-q', 'origin', 'feature/F-09-x');
  sh(me, 'fetch', '-q');

  const lines = autoSync({ cwd: me });

  assert.equal(lines.length, 1, lines.join('\n'));
  assert.match(lines[0], /원격 origin\/feature\/F-09-x의 새 커밋/);
  assert.equal(sh(me, 'rev-list', '--count', 'HEAD..origin/develop'), '0');
});
