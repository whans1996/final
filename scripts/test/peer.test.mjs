// 상대 작업 알림: NOW 선택, 요약, 변화 판정 + 임시 저장소에서 원격 브랜치별 NOW 고르기 (#54, #32)
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { after, test } from 'node:test';
import { nowStamp } from '../lib/common.mjs';
import { developChanges, featureOnRemotes, latestPeerNow, myBranchLines, myBranchNows, nowHeadline, peerDigest, peerWatchBanner, pickLatestNow } from '../lib/peer.mjs';

// git hook(커밋 테스트 게이트) 안에서 돌면 GIT_DIR 등이 실제 저장소를 가리킨다 → 임시 저장소용 git이 실제 저장소를 건드리지 않게 지운다 (sync.test.mjs와 같음)
for (const key of Object.keys(process.env)) if (key.startsWith('GIT_')) delete process.env[key];

const NOW = (stamp, feature = 'F-09 카드 목록 — AC2 진행') =>
  `# NOW\n\n갱신: ${stamp} · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-09-cards\n\n## 현재 기능\n\n${feature}\n\n## 바로 다음 할 일\n\n1. ConceptListTest 통과\n2. FE 목록 화면\n`;

test('원격과 작업 트리 중 갱신 시각이 늦은 NOW를 고른다', () => {
  const tree = { source: '작업 트리', text: NOW('2026-11-19 10:26'), at: 0 };
  const remote = { source: 'origin/feature/F-09-cards', text: NOW('2026-11-20 14:05'), at: 1 };
  assert.equal(pickLatestNow([tree, remote]).source, 'origin/feature/F-09-cards');
  assert.equal(pickLatestNow([{ ...tree, text: NOW('2026-11-21 09:00') }, remote]).source, '작업 트리');
  // 같으면 상대가 쓴 브랜치 (아래 #29 테스트)
  assert.equal(pickLatestNow([tree, { ...remote, text: NOW('2026-11-19 10:26') }]).source, 'origin/feature/F-09-cards');
  assert.equal(pickLatestNow([{ source: 'x', text: null }]), null);
});

test('갱신 시각이 같으면 갱신 줄의 브랜치(상대가 쓴 곳)를 출처로 고른다 (#29)', () => {
  const text = NOW('2026-11-20 14:05'); // 브랜치: feature/F-09-cards
  const cands = [
    { source: '작업 트리', text, at: 0 },
    // develop을 합친 내 브랜치가 커밋 시각은 더 늦다
    { source: 'origin/feature/F-08-concepts', text, at: 3 },
    { source: 'origin/develop', text, at: 2 },
    { source: 'origin/feature/F-09-cards', text, at: 1 },
  ];
  assert.equal(pickLatestNow(cands).source, 'origin/feature/F-09-cards');
  // 그 브랜치가 지워졌으면 develop·main이 아닌 원격 브랜치보다 develop을, 작업 트리보다 원격을
  assert.equal(pickLatestNow(cands.slice(0, 3)).source, 'origin/develop');
  assert.equal(pickLatestNow(cands.slice(0, 2)).source, 'origin/feature/F-08-concepts');
});

test('NOW 요약은 갱신 줄, 현재 기능 첫 줄, 다음 할 일 1번', () => {
  const h = nowHeadline(NOW('2026-11-20 14:05'));
  assert.match(h.stamp, /^갱신: 2026-11-20 14:05/);
  assert.equal(h.feature, 'F-09 카드 목록 — AC2 진행');
  assert.equal(h.next, 'ConceptListTest 통과');
  assert.deepEqual(nowHeadline(''), { stamp: '갱신: ?', feature: '(없음)', next: '(없음)' });
});

const BASE = {
  developSha: 'a1',
  peerNow: { source: '작업 트리', stamp: '갱신: 2026-11-19 10:26', feature: '없음', next: '-' },
  prs: { 30: { title: 'feat(F-09): 카드 목록', state: 'OPEN', head: 'feature/F-09-cards', sha: 's1' } },
};

test('직전 스냅샷이 없거나 변화가 없으면 알리지 않는다', () => {
  assert.deepEqual(peerDigest(null, BASE), []);
  assert.deepEqual(peerDigest(BASE, { ...BASE, develop: null }), []);
});

test('develop 새 커밋은 마이그레이션·API 변경과 함께 알리고, 내 커밋뿐이면 알리지 않는다', () => {
  const develop = {
    commits: [{ sha: 'b2', author: 'JoonseobKim', subject: 'Merge pull request #30' }],
    files: ['backend/src/main/resources/db/migration/V6__concept_wish.sql', 'docs/api/openapi.json'],
  };
  const [line] = peerDigest(BASE, { ...BASE, develop }, { myNames: ['whans1996'] });
  assert.match(line, /새 커밋 1개 \(새 마이그레이션 V6__concept_wish\.sql, openapi\.json 변경\)/);
  assert.match(line, /git merge origin\/develop/);
  const mine = { ...develop, commits: [{ sha: 'c3', author: 'whans1996', subject: 'Merge pull request #31' }] };
  assert.deepEqual(peerDigest(BASE, { ...BASE, develop: mine }, { myNames: ['whans1996'] }), []);
});

test('상대 NOW 갱신과 PR 열림·머지·push를 알린다', () => {
  const curr = {
    ...BASE,
    peerNow: { source: 'origin/feature/F-09-cards', stamp: '갱신: 2026-11-20 14:05', feature: 'F-09 AC2', next: 'FE 화면' },
    prs: {
      30: { ...BASE.prs[30], state: 'MERGED' },
      32: { title: 'feat(F-10): 카드 상세', state: 'OPEN', head: 'feature/F-10-card-detail', sha: 't1' },
    },
  };
  const lines = peerDigest(BASE, curr, { peerName: '김준섭' });
  assert.equal(lines.length, 3);
  assert.match(lines[0], /김준섭 NOW 갱신 \(origin\/feature\/F-09-cards\): F-09 AC2 \/ 다음: FE 화면/);
  assert.match(lines[1], /PR #30 .* 머지됨 → develop 합치기 대상/);
  assert.match(lines[2], /PR #32 .* 열림/);
  const pushed = { ...BASE, prs: { 30: { ...BASE.prs[30], sha: 's2' } } };
  assert.match(peerDigest(BASE, pushed)[0], /PR #30 .*새 커밋 push/);
  // 직전 PR 목록이 없으면(첫 확인) PR은 기준만 잡는다
  assert.deepEqual(peerDigest({ ...BASE, prs: null }, { ...BASE, prs: curr.prs }), []);
});

// ── 임시 저장소: 원격 브랜치별 NOW (#54, #32) ──

const tmp = mkdtempSync(join(tmpdir(), 'vibenote-peer-'));
after(() => rmSync(tmp, { recursive: true, force: true }));
// 개발 PC의 전역 git 설정·hook이 결과를 바꾸지 않게 한다
const EMPTY_GLOBAL = join(tmp, 'empty-gitconfig');
writeFileSync(EMPTY_GLOBAL, '');
process.env.GIT_CONFIG_GLOBAL = EMPTY_GLOBAL;
process.env.GIT_CONFIG_NOSYSTEM = '1';
const NO_HOOKS = join(tmp, 'no-hooks');
mkdirSync(NO_HOOKS);
let clock = Date.parse('2026-11-19T00:00:00Z') / 1000;
function sh(cwd, ...args) {
  // 명령마다 1분씩 늦게 찍어 커밋 시각 순서를 고정한다
  const date = `${(clock += 60)} +0900`;
  const env = { ...process.env, GIT_AUTHOR_DATE: date, GIT_COMMITTER_DATE: date };
  const r = spawnSync('git', ['-c', `core.hooksPath=${NO_HOOKS}`, '-c', 'init.defaultBranch=main', ...args], { cwd, encoding: 'utf8', env });
  assert.equal(r.status, 0, `git ${args.join(' ')}: ${r.stderr}`);
  return r.stdout.trim();
}
function commit(cwd, file, content, msg, author = 't') {
  mkdirSync(join(cwd, dirname(file)), { recursive: true });
  writeFileSync(join(cwd, file), content);
  sh(cwd, 'add', file);
  sh(cwd, '-c', `user.name=${author}`, '-c', 'user.email=t@t', 'commit', '-q', '-m', msg);
}
const DH = 'docs/handoff/NOW-dh.md';
const JS = 'docs/handoff/NOW-js.md';
const MY = (stamp, pc, feature, next = '다음') =>
  `# NOW\n\n갱신: ${stamp} · 작업자: whans · PC: ${pc} · 브랜치: x\n\n## 현재 기능\n\n${feature}\n\n## 바로 다음 할 일\n\n1. ${next}\n`;

let n = 0;
/** origin(bare) + 작업용 clone(work). develop·main에 두 사람의 NOW가 있다 */
function setup() {
  const dir = join(tmp, String(++n));
  const origin = join(dir, 'origin.git');
  sh(tmp, 'init', '-q', '--bare', origin);
  const work = join(dir, 'work');
  sh(tmp, 'clone', '-q', origin, work);
  commit(work, DH, MY('2026-11-19 10:00', 'dh-home', 'F-13 마무리', 'F-14 /spec'), 'init');
  commit(work, JS, NOW('2026-11-19 09:00', 'F-03 로그인'), 'init js');
  sh(work, 'push', '-q', 'origin', 'HEAD:main', 'HEAD:develop');
  return { origin, work };
}
/** develop에서 브랜치를 만들어 파일을 바꾸고 push */
function branchWith(work, branch, file, content, author = 't') {
  sh(work, 'fetch', '-q');
  sh(work, 'switch', '-q', '-C', branch, 'origin/develop');
  commit(work, file, content, `docs: ${branch}`, author);
  sh(work, 'push', '-q', '-f', 'origin', branch);
}
function mergeToDevelop(work, branch, merger = 't') {
  sh(work, 'fetch', '-q');
  sh(work, 'switch', '-q', '-C', 'develop', 'origin/develop');
  sh(work, '-c', `user.name=${merger}`, '-c', 'user.email=t@t', 'merge', '-q', '--no-ff', '-m', `Merge ${branch}`, `origin/${branch}`);
  sh(work, 'push', '-q', 'origin', 'develop');
}
/** 세션을 여는 내 clone (develop 체크아웃) */
function myClone(origin) {
  const me = join(dirname(origin), 'me');
  sh(tmp, 'clone', '-q', '-b', 'develop', origin, me);
  return me;
}

function squashToDevelop(work, branch) {
  sh(work, 'fetch', '-q');
  sh(work, 'switch', '-q', '-C', 'develop', 'origin/develop');
  sh(work, 'merge', '-q', '--squash', `origin/${branch}`);
  sh(work, '-c', 'user.name=t', '-c', 'user.email=t@t', 'commit', '-q', '-m', `${branch} (#1)`);
  sh(work, 'push', '-q', 'origin', 'develop');
}

test('develop에 머지 안 된 브랜치 중 내 NOW를 바꾼 것만 고른다 (#54: 11-19 F-14 중복 spec)', () => {
  const { origin, work } = setup();
  // squash 머지됐는데 원격 브랜치가 남은 것: --no-merged에 걸리지만 그 NOW는 develop 이력에 있어 고르지 않는다 (reviewer R5)
  branchWith(work, 'feature/issue-40-old', DH, MY('2026-11-18 15:00', 'dh-home', '#40 옛 일'));
  squashToDevelop(work, 'feature/issue-40-old');
  // 상대 브랜치: 상대 NOW만 바꾼다
  branchWith(work, 'feature/F-04-follow', JS, NOW('2026-11-20 16:55', 'F-04 팔로우'), 'JoonseobKim');
  // dh-ssafy: F-14 진행 → push (머지 전)
  branchWith(work, 'feature/F-14-code-quiz', DH, MY('2026-11-19 17:55', 'dh-ssafy', 'F-14 코드 퀴즈 — AC1~AC11 구현', 'PR'));
  // dh-home: #49 진행 → develop에 머지. develop의 NOW는 "다음: F-14 /spec"이라 F-14 진행을 가린다
  branchWith(work, 'feature/issue-49-mock', DH, MY('2026-11-19 20:26', 'dh-home', '#49 mock 개념 이름', 'F-14 /spec'));
  mergeToDevelop(work, 'feature/issue-49-mock');
  // 상대가 develop을 합치면 내 NOW 변경이 상대 브랜치에 들어오지만, develop에서 온 것이라 고르지 않는다
  sh(work, 'switch', '-q', '-C', 'feature/F-04-follow', 'origin/feature/F-04-follow');
  sh(work, '-c', 'user.name=t', '-c', 'user.email=t@t', 'merge', '-q', '--no-ff', '-m', 'Merge develop', 'origin/develop');
  sh(work, 'push', '-q', 'origin', 'feature/F-04-follow');
  const me = myClone(origin);

  const mine = myBranchNows('dh', { cwd: me });
  assert.equal(mine.truncated, false);
  assert.deepEqual(
    mine.branches.map((b) => b.branch),
    ['origin/feature/F-14-code-quiz'],
  );
  assert.match(mine.branches[0].text, /F-14 코드 퀴즈/);
  assert.deepEqual(
    myBranchNows('js', { cwd: me }).branches.map((b) => b.branch),
    ['origin/feature/F-04-follow'],
  );
  // 기준 브랜치가 없으면 빈 목록
  assert.deepEqual(myBranchNows('dh', { cwd: me, base: 'origin/nope' }).branches, []);
  // 시간 상한을 넘기면 남은 브랜치는 보지 않고 생략을 알린다 (세션 시작 hook 제한, reviewer R2)
  assert.deepEqual(myBranchNows('dh', { cwd: me, budgetMs: -1 }), { branches: [], truncated: true });
});

test('/spec 가드: develop 밖에서 같은 기능을 진행 중인 원격 브랜치 (#54 제안 2)', () => {
  const { origin, work } = setup();
  // 브랜치 이름으로
  branchWith(work, 'feature/F-14-code-quiz', DH, MY('2026-11-19 17:55', 'dh-ssafy', 'F-14'));
  // 이름은 달라도 spec 파일로 (다른 이름의 브랜치에서 /spec을 커밋한 경우)
  branchWith(work, 'feature/issue-49-mock', 'docs/specs/F-14-quiz-dup.md', '# F-14');
  // F-1·F-140은 F-14가 아니다. develop에 이미 있는 spec(다른 브랜치가 물려받은 것)도 아니다
  branchWith(work, 'feature/F-1-x', 'docs/specs/F-140-y.md', '# F-140');
  branchWith(work, 'feature/F-05a-zip', 'docs/specs/F-05a-zip.md', '# F-05a');
  // 로드맵 ID도 같은 규칙 (#213): 브랜치 이름·spec 파일 모두. E-10은 E-1이 아니다
  branchWith(work, 'feature/E-1-eval-runner', 'docs/specs/E-1-eval.md', '# E-1');
  branchWith(work, 'feature/E-10-other', 'docs/specs/E-10-other.md', '# E-10');
  // squash 머지됐는데 원격 브랜치가 남은 F-20: spec이 develop과 같으면 "머지됨"으로 표시만 한다 (reviewer R5)
  branchWith(work, 'feature/F-20-done', 'docs/specs/F-20-done.md', '# F-20 DONE');
  squashToDevelop(work, 'feature/F-20-done');
  // develop의 F-20 spec을 물려받기만 한 브랜치는 F-20 진행이 아니다 (reviewer R4)
  branchWith(work, 'feature/issue-70-z', 'z.txt', 'z');
  // develop에 있는 F-20 spec을 고치는 브랜치는 진행 중이다
  branchWith(work, 'feature/issue-71-w', 'docs/specs/F-20-done.md', '# F-20 고침');
  // 그 뒤 develop이 F-20 spec을 고치면(APPROVED → DONE) issue-70-z는 옛 spec을 물려받은 채 뒤처지지만, 고친 적이 없으므로 진행이 아니다.
  // squash 잔여 F-20-done도 그 spec 내용이 develop 이력에 있으므로 여전히 머지됨이다 (Codex 리뷰)
  sh(work, 'switch', '-q', '-C', 'develop', 'origin/develop');
  commit(work, 'docs/specs/F-20-done.md', '# F-20 DONE (PR #1)', 'docs: F-20 완료 표시');
  sh(work, 'push', '-q', 'origin', 'develop');
  const me = myClone(origin);

  assert.deepEqual(featureOnRemotes('F-14', { cwd: me }), [
    { branch: 'origin/feature/issue-49-mock', specs: ['docs/specs/F-14-quiz-dup.md'], here: false, merged: false },
    { branch: 'origin/feature/F-14-code-quiz', specs: [], here: false, merged: false },
  ]);
  assert.deepEqual(featureOnRemotes('F-05a', { cwd: me }), [{ branch: 'origin/feature/F-05a-zip', specs: ['docs/specs/F-05a-zip.md'], here: false, merged: false }]);
  assert.deepEqual(featureOnRemotes('F-05', { cwd: me }), []);
  assert.deepEqual(featureOnRemotes('F-99', { cwd: me }), []);
  assert.deepEqual(featureOnRemotes('E-1', { cwd: me }), [{ branch: 'origin/feature/E-1-eval-runner', specs: ['docs/specs/E-1-eval.md'], here: false, merged: false }]);
  assert.deepEqual(featureOnRemotes('F-20', { cwd: me }), [
    { branch: 'origin/feature/issue-71-w', specs: ['docs/specs/F-20-done.md'], here: false, merged: false },
    { branch: 'origin/feature/F-20-done', specs: [], here: false, merged: true },
  ]);
  // 지금 브랜치(승인 뒤 같은 브랜치에서 /spec 다시 실행)는 표시만 하고 멈출 대상이 아니다 (reviewer R1)
  assert.equal(featureOnRemotes('F-14', { cwd: me, current: 'feature/F-14-code-quiz' })[1].here, true);
  // 기준 브랜치를 못 찾으면 "없음"(✅)이 아니라 판정 불가(null) — 가드가 거짓 통과를 내지 않게 (reviewer)
  assert.equal(featureOnRemotes('F-14', { cwd: me, base: 'origin/nope' }), null);
  // 원격 브랜치 목록 조회가 실패(시간 초과 등)해도 판정 불가 (Codex 리뷰)
  assert.equal(featureOnRemotes('F-14', { cwd: me, listBranches: () => null }), null);
});

test('내 다른 브랜치 목록: 지금 브랜치가 아닌 것만 ⚠️', () => {
  const at1 = Date.parse('2026-11-19T09:10:00Z');
  const at2 = Date.parse('2026-11-20T09:05:00Z');
  const branches = [
    { branch: 'origin/feature/F-14-code-quiz', at: at1, text: MY('2026-11-19 17:55', 'dh-ssafy', 'F-14 코드 퀴즈 — AC1~AC11 구현') },
    { branch: 'origin/feature/issue-54-x', at: at2, text: MY('2026-11-20 18:00', 'dh-home', '#54') },
  ];
  const r = myBranchLines({ branches, truncated: false }, 'feature/issue-54-x');
  assert.deepEqual(r.others, ['origin/feature/F-14-code-quiz']);
  // NOW 갱신 시각과 브랜치 마지막 커밋 시각을 함께 (NOW 뒤에 코드만 더 push했을 수 있다, #54 제안 형식, reviewer R3)
  const c = (at) => nowStamp(new Date(at)).slice(5);
  assert.equal(r.lines[0], `- ⚠️ origin/feature/F-14-code-quiz · NOW 11-19 17:55 dh-ssafy · 커밋 ${c(at1)} · F-14 코드 퀴즈 — AC1~AC11 구현`);
  assert.equal(r.lines[1], `- (지금 브랜치) origin/feature/issue-54-x · NOW 11-20 18:00 dh-home · 커밋 ${c(at2)} · #54`);
  assert.deepEqual(myBranchLines({ branches: [], truncated: false }, 'develop'), { lines: [], others: [], truncated: false });
  // 시간 상한으로 생략했으면 마지막 줄에 알린다 (본 브랜치가 0개여도)
  assert.match(myBranchLines({ branches, truncated: true }, 'develop').lines.at(-1), /일부 생략.*node scripts\/branches\.mjs/);
  assert.deepEqual(myBranchLines({ branches: [], truncated: true }, 'develop').truncated, true);
  assert.equal(myBranchLines({ branches: [], truncated: true }, 'develop').lines.length, 1);
});

test('상대 NOW는 원격 브랜치마다 보고 갱신 시각이 늦은 것을 고른다 (#32-1: 나중에 머지된 옛 NOW)', () => {
  const { origin, work } = setup();
  branchWith(work, 'feature/F-08-old', JS, NOW('2026-11-19 11:00', 'F-08 옛 작업'), 'JoonseobKim');
  branchWith(work, 'feature/F-09-cards', JS, NOW('2026-11-20 14:05', 'F-09 카드 목록'), 'JoonseobKim');
  // 옛 브랜치가 나중에 squash 머지되면 NOW를 바꾼 커밋 중 develop의 squash 커밋이 가장 늦다
  // (--no-ff 머지 커밋은 log의 history simplification이 건너뛰어 재현되지 않는다, reviewer)
  sh(work, 'fetch', '-q');
  sh(work, 'switch', '-q', '-C', 'develop', 'origin/develop');
  sh(work, 'merge', '-q', '--squash', 'origin/feature/F-08-old');
  sh(work, '-c', 'user.name=t', '-c', 'user.email=t@t', 'commit', '-q', '-m', 'F-08 (#40)');
  sh(work, 'push', '-q', 'origin', 'develop');
  const me = myClone(origin);
  // 전제: 수정 전 방식(NOW를 바꾼 가장 늦은 커밋 1개)이면 옛 F-08 NOW를 고른다
  const last = sh(me, 'log', '--remotes=origin', '-1', '--format=%H', '--', JS);
  assert.match(sh(me, 'show', `${last}:${JS}`), /F-08 옛 작업/);

  const picked = latestPeerNow('js', { cwd: me });
  assert.equal(picked.source, 'origin/feature/F-09-cards');
  assert.match(picked.text, /F-09 카드 목록/);
  // 세션 시작 예산이 바닥나면(timeoutMs 0) git을 부르지 않고 작업 트리의 NOW만 본다 (H-5, reviewer)
  const local = latestPeerNow('js', { cwd: me, timeoutMs: 0 });
  assert.ok(!local || local.source === '작업 트리', local?.source);
});

test('develop 변화는 10개로 자르지 않고 작성자를 전부 본다 (#32-2)', () => {
  const { work } = setup();
  const prev = sh(work, 'rev-parse', 'HEAD');
  commit(work, 'backend/src/main/resources/db/migration/V9__x.sql', 'x', 'feat: V9', 'JoonseobKim');
  for (let i = 0; i < 11; i++) commit(work, `mine${i}.txt`, 'x', `chore: ${i}`, 'whans1996');
  const curr = sh(work, 'rev-parse', 'HEAD');

  const dev = developChanges(prev, curr, { cwd: work });
  assert.equal(dev.commits.length, 12);
  const [line] = peerDigest(BASE, { ...BASE, develop: dev }, { myNames: ['whans1996'] });
  assert.match(line, /새 커밋 12개 \(새 마이그레이션 V9__x\.sql\)/);
});

test('내 다른 PC의 git 이름이 달라도 develop 병합 커밋(GitHub 계정)이 내 것이면 알리지 않는다 (H-7)', () => {
  const { work } = setup();
  const start = sh(work, 'rev-parse', 'origin/develop');
  // dh-home은 user.name이 GitHub 계정과 달랐다. 머지 커밋은 GitHub이 계정 이름으로 만든다
  branchWith(work, 'feature/L-1a-x', 'a.txt', 'x', 'whans1996@naver.com');
  mergeToDevelop(work, 'feature/L-1a-x', 'whans1996');
  const mine = sh(work, 'rev-parse', 'origin/develop');
  const dev = developChanges(start, mine, { cwd: work });
  assert.equal(dev.commits.length, 2);
  assert.deepEqual(peerDigest(BASE, { ...BASE, develop: dev }, { myNames: ['whans1996'] }), []);

  // 상대가 머지한 PR은 알린다
  branchWith(work, 'feature/F-20-y', 'b.txt', 'y', 'JoonseobKim');
  mergeToDevelop(work, 'feature/F-20-y', 'JoonseobKim');
  const peerDev = developChanges(mine, sh(work, 'rev-parse', 'origin/develop'), { cwd: work });
  assert.match(peerDigest(BASE, { ...BASE, develop: peerDev }, { myNames: ['whans1996'] })[0], /새 커밋 2개/);
});

test('peer-watch 사람용 알림: 상대 변화와 자동 동기화 결과를 짧게 보이고, 없으면 null (HX-7)', () => {
  assert.equal(peerWatchBanner([], []), null);
  // 자동 동기화의 부가 안내(✅·⚠️·⏸️로 시작하지 않는 줄)는 사람 화면에 넣지 않는다
  assert.equal(peerWatchBanner([], ['- 병합 커밋은 아직 push하지 않았습니다.']), null);
  const long = `- 김준섭 PR #230 ${'가'.repeat(200)}`;
  const text = peerWatchBanner([long, '- 김준섭 NOW 갱신 (origin/x): F-20 / 다음: y'], ['- ✅ origin/develop의 새 커밋 2개를 자동으로 합쳤습니다.']);
  const lines = text.split('\n');
  assert.match(lines[0], /^\[바이브노트\] 📣 상대 작업 변화/);
  assert.match(lines[1], /^📣 김준섭 PR #230 가+…$/);
  assert.ok(lines[1].length <= 113);
  assert.match(lines[2], /^📣 김준섭 NOW 갱신/);
  assert.match(lines[3], /^🔄 ✅ origin\/develop/);
  // 많으면 앞의 몇 줄만 보이고 나머지 수를 알린다
  const many = peerWatchBanner(Array.from({ length: 9 }, (_, i) => `- 변화 ${i}`), []);
  assert.match(many.split('\n').at(-1), /외 4건/);
});
