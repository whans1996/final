// Codex 재리뷰 판정 중 GitHub 호출 없는 부분
import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  isDocPath,
  loopAction,
  onlyDocsChanged,
  requestCount,
  setupNotice,
  skipRecheck,
  unresolvedFindingAt,
  waitingInTranscript,
} from '../lib/codex.mjs';

test('openapi.json은 문서가 아니다', () => {
  assert.ok(isDocPath('docs/handoff/NOW-dh.md'));
  assert.ok(!isDocPath('docs/api/openapi.json'));
  assert.ok(!isDocPath('backend/src/A.java'));
});

test('develop 합치기로 들어온 코드 파일은 재리뷰 대상이 아니다', () => {
  const files = ['docs/진행현황.md', 'scripts/lib/common.mjs', '.claude/hooks/session-start.mjs'];
  assert.ok(!onlyDocsChanged(files));
  assert.ok(onlyDocsChanged(files, (f) => !f.startsWith('docs/')));
  // PR 자신의 코드 변경이 하나라도 있으면 재리뷰
  assert.ok(!onlyDocsChanged([...files, 'backend/src/A.java'], (f) => f !== 'backend/src/A.java' && !f.startsWith('docs/')));
  assert.ok(!onlyDocsChanged([]));
});

test('이관 마커보다 이른 지적은 해결된 것으로 본다', () => {
  const findings = ['2026-11-19T01:00:00Z', '2026-11-19T02:00:00Z'];
  assert.equal(unresolvedFindingAt(findings, []), '2026-11-19T02:00:00Z');
  assert.equal(unresolvedFindingAt(findings, ['2026-11-19T03:00:00Z']), '');
  // 마커 뒤에 새 지적이 오면 다시 미해결
  assert.equal(unresolvedFindingAt([...findings, '2026-11-19T04:00:00Z'], ['2026-11-19T03:00:00Z']), '2026-11-19T04:00:00Z');
  assert.equal(unresolvedFindingAt([], ['2026-11-19T03:00:00Z']), '');
});

const bot = (at, body, updated_at) => ({ user: { login: 'chatgpt-codex-connector[bot]' }, created_at: at, updated_at, body });
const human = (login, at, body) => ({ user: { login }, created_at: at, body });
const SHA = 'a'.repeat(40);
const ACCOUNT = 'To use Codex here, [create a Codex account and connect to github](https://chatgpt.com/codex/cloud/settings/connectors).';
const ENV = 'To use Codex here, [create an environment for this repo](https://chatgpt.com/codex/settings/environments).';

test('계정 미연결 안내는 요청한 사람과 함께 잡는다 (PR #30)', () => {
  const comments = [human('JoonseobKim', '2026-11-19T02:29:48Z', '@codex review'), bot('2026-11-19T02:29:52Z', ACCOUNT)];
  assert.deepEqual(setupNotice(comments), {
    kind: 'account',
    url: 'https://chatgpt.com/codex/cloud/settings/connectors',
    at: '2026-11-19T02:29:52Z',
    who: 'JoonseobKim',
  });
});

test('환경 안내도 잡고, 그 뒤 리뷰가 달리면 안내는 지난 일이다', () => {
  const env = [human('whans1996', '2026-11-19T01:00:00Z', '@codex review'), bot('2026-11-19T01:00:05Z', ENV)];
  assert.equal(setupNotice(env).kind, 'env');
  assert.equal(setupNotice([...env, bot('2026-11-19T01:10:00Z', '<!-- codex-review-receipt:v1 --> ok')]), null);
  // 기존 요약 댓글이 안내 뒤에 갱신돼도 지난 일로 본다
  assert.equal(setupNotice([bot('2026-11-19T00:00:00Z', 'summary', '2026-11-19T01:05:00Z'), ...env]), null);
  assert.equal(setupNotice([human('a', '2026-11-19T01:00:00Z', 'hi')]), null);
});

test('재리뷰 횟수는 사람 댓글의 서로 다른 head 마커 수다', () => {
  const mark = (sha) => `@codex review

<!-- codex-review-request: ${sha} -->`;
  const comments = [
    human('whans1996', '2026-11-19T01:00:00Z', mark(SHA)),
    human('whans1996', '2026-11-19T01:10:00Z', mark(SHA)),
    human('whans1996', '2026-11-19T01:20:00Z', mark('b'.repeat(40))),
    human('whans1996', '2026-11-19T01:30:00Z', '@codex review'),
    bot('2026-11-19T01:31:00Z', mark('c'.repeat(40))),
  ];
  assert.equal(requestCount(comments), 2);
  assert.equal(requestCount([]), 0);
});

test('자동 리뷰가 부른 계정 안내에는 예전 요청자를 붙이지 않는다', () => {
  const comments = [
    human('whans1996', '2026-11-19T01:00:00Z', '@codex review'),
    bot('2026-11-19T01:00:30Z', '<!-- codex-review-receipt:v1 --> ok'),
    bot('2026-11-19T02:00:00Z', ACCOUNT),
  ];
  assert.equal(setupNotice(comments).who, null);
});

test('Stop hook 재확인: COMPLETE는 판정한 PR head가 로컬 HEAD일 때만 확정', () => {
  const head = 'abc1234' + '0'.repeat(33);
  const now = 10 * 60_000;
  const MS = 3 * 60_000;
  assert.ok(skipRecheck({ head, prHead: 'abc1234', verdict: 'COMPLETE', at: 0 }, head, now, MS));
  // push 전 커밋: PR head는 이전 커밋 → 3분이 지나면 다시 본다
  assert.ok(!skipRecheck({ head, prHead: 'def5678', verdict: 'COMPLETE', at: 0 }, head, now, MS));
  assert.ok(skipRecheck({ head, prHead: 'def5678', verdict: 'COMPLETE', at: now - 1000 }, head, now, MS));
  assert.ok(!skipRecheck({ head: 'other', verdict: 'COMPLETE', prHead: 'abc1234', at: now }, head, now, MS));
  assert.ok(!skipRecheck(undefined, head, now, MS));
});

test('Stop hook 대기 판정은 백그라운드 Bash 호출만 본다', () => {
  const use = (command, bg, id = 'toolu_1') =>
    JSON.stringify({ message: { content: [{ type: 'tool_use', id, name: 'Bash', input: { command, run_in_background: bg } }] } });
  const done = (id) => JSON.stringify({ message: { content: `<task-notification><tool-use-id>${id}</tool-use-id><status>failed</status></task-notification>` } });
  const hookText = JSON.stringify({ type: 'tool_use_reason', text: '"tool_use" `node scripts/codex-review.mjs 7 --request --wait --status`' });
  assert.ok(waitingInTranscript(use('node scripts/codex-review.mjs 7 --request --wait --status', true), 7));
  assert.ok(!waitingInTranscript(use('node scripts/codex-review.mjs 7 --request --wait', false), 7));
  assert.ok(!waitingInTranscript(use('node scripts/codex-review.mjs 71 --wait', true), 7));
  assert.ok(!waitingInTranscript(hookText, 7));
  assert.ok(!waitingInTranscript('', 7));
  // 이전 head의 --wait가 끝났으면 기다리는 프로세스가 아니다 (Codex 리뷰). 새로 건 것은 센다
  const old = use('node scripts/codex-review.mjs 7 --request --wait --status', true, 'toolu_old');
  assert.ok(!waitingInTranscript([old, done('toolu_old')].join('\n'), 7));
  assert.ok(waitingInTranscript([old, done('toolu_old'), use('node scripts/codex-review.mjs 7 --wait', true, 'toolu_new')].join('\n'), 7));
});

test('Stop hook 동작: 판정별 차단·안내, 같은 (PR, head, 판정)은 한 번만', () => {
  const a = (line, extra = {}) => loopAction({ line, pr: 7, ...extra });
  assert.deepEqual(
    [a('STALE head abc1234, Codex def5678 기준 (재리뷰 1회)').action, a('STALE head abc1234, Codex def5678 기준 (재리뷰 1회)').kind],
    ['block', 'request'],
  );
  assert.equal(a('RUNNING head abc1234 리뷰 in_progress (재리뷰 1회)').kind, 'wait');
  assert.equal(a('RUNNING head abc1234 리뷰 in_progress (재리뷰 1회)', { waiting: true }).action, 'none');
  assert.equal(a('FINDINGS head abc1234 (재리뷰 4회)').kind, 'fix');
  assert.equal(a('FINDINGS head abc1234 (재리뷰 5회)').kind, 'limit');
  assert.equal(a('ERROR Codex 계정 미연결 (@JoonseobKim, t): u (재리뷰 0회)').kind, 'setup');
  assert.equal(a('ERROR gh pr view: not found').kind, 'error');
  assert.equal(a('COMPLETE head abc1234 지적 없음 (재리뷰 2회)').action, 'none');
  const first = a('FINDINGS head abc1234 (재리뷰 2회)');
  assert.equal(first.key, '7:abc1234:FINDINGS');
  assert.equal(a('FINDINGS head abc1234 (재리뷰 2회)', { handled: [first.key] }).action, 'none');
  // 새 head의 지적은 다시 막는다
  assert.equal(a('FINDINGS head 9999999 (재리뷰 3회)', { handled: [first.key] }).action, 'block');
});
