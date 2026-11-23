// 자동 동기화: 할 일 고르기(순수 함수)와 임시 저장소에서의 실제 병합·충돌·보류
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { after, test } from 'node:test';
import { autoSync, localRefPlan, mergeNotes, syncPlan } from '../lib/sync.mjs';

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

test('양쪽이 고친 merge=union 파일을 합치면 GitHub PR 충돌을 막도록 push를 안내한다 (#86)', () => {
  const { peer, me } = setup();
  // .gitattributes는 develop에 있다 (실제 저장소와 같다). 한글 경로도 따옴표 없이 비교해야 한다
  peerMergesToDevelop(peer, '.gitattributes', '결정로그.md merge=union\n');
  peerMergesToDevelop(peer, '결정로그.md', 'base\n');
  sh(me, 'fetch', '-q');
  sh(me, 'merge', '-q', 'origin/develop');
  sh(me, 'push', '-q');
  commit(me, '결정로그.md', 'base\nmine\n', 'docs: mine');
  peerMergesToDevelop(peer, '결정로그.md', 'base\ntheirs\n');
  sh(me, 'fetch', '-q');

  const lines = autoSync({ cwd: me });

  assert.match(lines[0], /origin\/develop의 새 커밋 1개를 자동으로 합쳤습니다/);
  assert.ok(
    lines.some((l) => /결정로그\.md/.test(l) && /union/.test(l) && /git push/.test(l)),
    `push 안내가 없다: ${lines.join(' / ')}`,
  );
  assert.ok(!lines.some((l) => l.includes('push하지 않았습니다')), 'union 안내가 기존 줄을 대신한다');
  assert.equal(readFileSync(join(me, '결정로그.md'), 'utf8'), 'base\nmine\ntheirs\n');
});

test('union 파일을 한쪽만 고쳤으면 push 안내를 하지 않는다 (#86)', () => {
  const { peer, me } = setup();
  peerMergesToDevelop(peer, '.gitattributes', 'log.md merge=union\n');
  peerMergesToDevelop(peer, 'log.md', 'base\n');
  sh(me, 'fetch', '-q');
  sh(me, 'merge', '-q', 'origin/develop');
  commit(me, 'mine.txt', 'mine\n', 'feat: mine');
  peerMergesToDevelop(peer, 'log.md', 'base\ntheirs\n');
  sh(me, 'fetch', '-q');

  const lines = autoSync({ cwd: me });

  assert.match(lines[0], /자동으로 합쳤습니다/);
  assert.ok(!lines.some((l) => /union/.test(l)), lines.join(' / '));
  assert.match(lines.at(-1), /push하지 않았습니다/);
});

test('다른 PC가 push한 내 브랜치의 union 파일과 겹쳐도 develop 병합이 아니면 PR 충돌 안내를 하지 않는다 (#86)', () => {
  const { peer, me } = setup();
  peerMergesToDevelop(peer, '.gitattributes', 'log.md merge=union\n');
  peerMergesToDevelop(peer, 'log.md', 'base\n');
  sh(me, 'fetch', '-q');
  sh(me, 'merge', '-q', 'origin/develop');
  sh(me, 'push', '-q');
  const other = join(me, '..', 'other-pc');
  sh(tmp, 'clone', '-q', '-b', 'feature/F-09-x', join(me, '..', 'origin.git'), other);
  commit(other, 'log.md', 'base\nother pc\n', 'docs: other pc');
  sh(other, 'push', '-q', 'origin', 'feature/F-09-x');
  commit(me, 'log.md', 'base\nthis pc\n', 'docs: this pc');
  sh(me, 'fetch', '-q');

  const lines = autoSync({ cwd: me });

  assert.match(lines[0], /원격 origin\/feature\/F-09-x의 새 커밋 1개/);
  assert.ok(!lines.some((l) => /union/.test(l)), lines.join(' / '));
  assert.match(lines.at(-1), /push하지 않았습니다/);
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

test('체크아웃 안 된 로컬 develop·main은 뒤처졌고 로컬 커밋이 없을 때만 fast-forward한다 (#67)', () => {
  const ref = (name, behind, ahead = 0, extra = {}) => ({ name, local: true, remote: true, behind, ahead, ...extra });
  const plan = localRefPlan({ current: 'feature/F-09-x', refs: [ref('develop', 3), ref('main', 1)] });
  assert.deepEqual(plan, { ff: [{ name: 'develop', behind: 3 }, { name: 'main', behind: 1 }], warn: [] });
  // 지금 브랜치·다른 worktree에 체크아웃된 브랜치는 건드리지 않는다 (작업 트리가 따라가지 않으므로)
  assert.deepEqual(localRefPlan({ current: 'develop', checkedOut: ['main'], refs: [ref('develop', 3), ref('main', 1)] }).ff, []);
  // 로컬에만 있는 커밋이 있으면 FF하지 않고 알린다, 없거나 원격이 없으면 건너뛴다
  const odd = localRefPlan({ current: 'x', refs: [ref('develop', 2, 1), ref('main', 0), { name: 'main', local: false, remote: true, behind: 0, ahead: 0 }] });
  assert.deepEqual(odd, { ff: [], warn: [{ name: 'develop', ahead: 1 }] });
});

test('상대가 develop·main에 머지하면 체크아웃 안 된 로컬 develop·main도 따라간다 (커밋 안 된 변경이 있어도)', () => {
  const { peer, me } = setup();
  sh(me, 'branch', 'develop', 'origin/develop');
  peerMergesToDevelop(peer, 'theirs.txt', 'theirs\n');
  sh(peer, 'push', '-q', 'origin', 'develop:main');
  // 작업 중인 파일: feature 병합은 미루지만 로컬 develop·main은 작업 트리와 무관하므로 갱신한다
  writeFileSync(join(me, 'shared.txt'), 'wip\n');
  sh(me, 'fetch', '-q');
  const fetchHead = readFileSync(join(me, '.git', 'FETCH_HEAD'), 'utf8');

  const lines = autoSync({ cwd: me });

  assert.match(lines[0], /⏸️/);
  assert.ok(lines.some((l) => /^- 📥 로컬 develop \+1 · main \+1/.test(l)), lines.join('\n'));
  // 로컬 갱신(git fetch .)이 사람이 받은 FETCH_HEAD를 덮지 않는다 (reviewer)
  assert.equal(readFileSync(join(me, '.git', 'FETCH_HEAD'), 'utf8'), fetchHead);
  assert.equal(sh(me, 'rev-parse', 'develop'), sh(me, 'rev-parse', 'origin/develop'));
  assert.equal(sh(me, 'rev-parse', 'main'), sh(me, 'rev-parse', 'origin/main'));
  assert.equal(sh(me, 'rev-parse', '--abbrev-ref', 'HEAD'), 'feature/F-09-x');
});

test('로컬 develop에 원격에 없는 커밋이 있으면 FF하지 않고 알린다', () => {
  const { peer, me } = setup();
  sh(me, 'branch', 'develop', 'origin/develop');
  sh(me, 'switch', '-q', 'develop');
  commit(me, 'local.txt', 'x\n', 'chore: 실수로 develop에 커밋');
  sh(me, 'switch', '-q', 'feature/F-09-x');
  peerMergesToDevelop(peer, 'theirs.txt', 'theirs\n');
  sh(me, 'fetch', '-q');
  const before = sh(me, 'rev-parse', 'develop');

  const lines = autoSync({ cwd: me });

  assert.ok(lines.some((l) => /^- ⚠️ 로컬 develop에 origin\/develop에 없는 커밋 1개/.test(l)), lines.join('\n'));
  assert.equal(sh(me, 'rev-parse', 'develop'), before);
});
