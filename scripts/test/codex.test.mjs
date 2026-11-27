// Codex 재리뷰 판정 중 GitHub 호출 없는 부분
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import {
  STUCK_MS,
  isDocPath,
  isRetryLine,
  headLagging,
  judgeReview,
  loopAction,
  loopSkipReason,
  onlyDocsChanged,
  requestCount,
  setupNotice,
  STOP_LABEL,
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

test('codex-loop는 draft·닫힌 PR과 docs만 바꾼 PR을 보지 않는다 (#129)', () => {
  const files = (...paths) => paths.map((path) => ({ path }));
  assert.equal(loopSkipReason({ state: 'OPEN', isDraft: true, files: files('backend/A.java') }), 'DRAFT');
  assert.equal(loopSkipReason({ state: 'MERGED', isDraft: false, files: files('backend/A.java') }), 'CLOSED');
  assert.equal(loopSkipReason({ state: 'OPEN', isDraft: false, files: files('docs/진행현황.md', 'docs/산출물/04-WBS.md') }), 'DOCS');
  // openapi.json은 API 계약이라 문서가 아니다
  assert.equal(loopSkipReason({ state: 'OPEN', isDraft: false, files: files('docs/진행현황.md', 'docs/api/openapi.json') }), null);
  assert.equal(loopSkipReason({ state: 'OPEN', isDraft: false, files: files('docs/a.md', '.claude/hooks/x.mjs') }), null);
  // 파일 목록을 못 받으면 루프를 건너뛰지 않는다
  assert.equal(loopSkipReason({ state: 'OPEN', isDraft: false }), null);
});

test('사람이 PR에 codex-stopped 라벨을 붙이면 codex-loop가 재요청을 지시하지 않는다 (#249)', () => {
  const files = [{ path: 'scripts/lib/guard.mjs' }];
  assert.equal(loopSkipReason({ state: 'OPEN', isDraft: false, files, labels: [{ name: 'follow-up' }, { name: STOP_LABEL }] }), 'STOPPED');
  assert.equal(loopSkipReason({ state: 'OPEN', isDraft: false, files, labels: [{ name: 'follow-up' }] }), null);
  assert.equal(STOP_LABEL, 'codex-stopped');
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

// ── judgeReview: codex-review.mjs의 판정 (#67, #48·#39) ──
const HEAD = '1'.repeat(40);
const PREV = '2'.repeat(40);
const OLD = '3'.repeat(40);
const T0 = Date.parse('2026-11-20T06:00:00Z');
const iso = (min) => new Date(T0 + min * 60_000).toISOString().replace('.000Z', 'Z');
// 실제 요약 댓글 형식 (PR #47·#66): 기계용 상태 + Code Review 행의 상태 시각
const summary = (sha, status, { at = 0, findings = false } = {}) =>
  bot(
    iso(-60),
    [
      '<!-- codex-pull-request-review-summary -->',
      `<!-- codex-review-state:v1 {"code_review":{"commit_sha":"${sha}","has_any_findings":${findings},"status":"${status}"}} -->`,
      `| 📝 **Code Review** | **${status}** <relative-time datetime="${new Date(T0 + at * 60_000).toISOString()}">x</relative-time> | \`${sha.slice(0, 7)}\` | Manual request |`,
    ].join('\n'),
    iso(at),
  );
const req = (sha, min, { retry = false } = {}) =>
  human('whans1996', iso(min), `@codex review\n\n<!-- codex-review-request: ${sha} -->${retry ? `\n<!-- codex-review-retry: ${sha} -->` : ''}`);
const judge = (comments, { now = 0, docsOnly = [], findings = [] } = {}) =>
  judgeReview({
    pr: 7,
    h: { sha: HEAD, state: 'OPEN' },
    comments,
    findingTimes: () => findings,
    docsOnlySince: (b) => docsOnly.includes(b),
    now: T0 + now * 60_000,
  });

test('judgeReview 기존 판정: 완료·지적·이전 커밋·요청 대기·문서만 변경·종료', () => {
  assert.equal(judge([summary(HEAD, 'completed')]).verdict, 'COMPLETE');
  assert.equal(judge([summary(HEAD, 'completed', { findings: true })]).verdict, 'FINDINGS');
  assert.equal(judge([summary(PREV, 'completed', { findings: true })]).verdict, 'STALE');
  assert.equal(judge([summary(PREV, 'completed', { findings: true }), req(HEAD, 0)], { now: 2 }).verdict, 'RUNNING');
  const docs = judge([summary(PREV, 'completed')], { docsOnly: [PREV] });
  assert.equal(docs.verdict, 'COMPLETE');
  assert.match(docs.msg, /^head 1111111 문서만 변경/);
  assert.equal(judgeReview({ pr: 7, h: { sha: HEAD, state: 'MERGED' }, comments: [] }).verdict, 'CLOSED');
});

test('상태 댓글의 지적 표시가 이전 커밋 지적의 누적이어도, 모든 인라인 지적보다 늦은 그 커밋의 영수증이 있으면 지적 없음이다 (PR #97)', () => {
  const receipt = (sha, min) =>
    bot(
      iso(min),
      `<!-- codex-review-receipt:v1 -->\n\nCodex Review: Didn't find any major issues. Keep it up!\n\n**Reviewed commit:** \`${sha.slice(0, 10)}\``,
    );
  const earlier = { findings: [iso(-30)] };
  // head 리뷰: 영수증 + has_any_findings:true 상태 댓글 (Codex가 PR의 미해결 스레드를 누적해 표시)
  const head = judge([summary(HEAD, 'completed', { at: 10, findings: true }), receipt(HEAD, 9)], earlier);
  assert.equal(head.verdict, 'COMPLETE');
  // 영수증 뒤에 인라인 지적이 달렸으면 지적이 있는 것이다
  assert.equal(judge([summary(HEAD, 'completed', { at: 10, findings: true }), receipt(HEAD, 9)], { findings: [iso(9.5)] }).verdict, 'FINDINGS');
  // 영수증이 없으면 상태 댓글 그대로
  assert.equal(judge([summary(HEAD, 'completed', { at: 10, findings: true })], earlier).verdict, 'FINDINGS');
  // 그 뒤 문서만 push해도 그 커밋을 기준으로 COMPLETE (spec DONE 커밋)
  const docs = judge([summary(PREV, 'completed', { at: 10, findings: true }), receipt(PREV, 9)], { ...earlier, docsOnly: [PREV] });
  assert.equal(docs.verdict, 'COMPLETE');
  assert.match(docs.msg, /문서만 변경/);
});

test('Codex 리뷰 실패(failed)는 RUNNING이 아니라 재요청 대상, 재시도 뒤 또 실패하면 ERROR (#48)', () => {
  const failed = judge([summary(HEAD, 'failed', { at: 0 })], { now: 1 });
  assert.equal(failed.verdict, 'STALE');
  assert.equal(failed.retry, true);
  assert.match(failed.msg, /^head 1111111 Codex 리뷰 실패/);
  // 실패 뒤 재요청했으면 시작을 기다린다
  assert.equal(judge([summary(HEAD, 'failed', { at: 0 }), req(HEAD, 1, { retry: true })], { now: 3 }).verdict, 'RUNNING');
  // 재시도한 뒤 다시 실패: 사람에게
  const again = judge([req(HEAD, -10, { retry: true }), summary(HEAD, 'failed', { at: 0 })], { now: 1 });
  assert.equal(again.verdict, 'ERROR');
  assert.match(again.msg, /^head 1111111 Codex 리뷰 실패가 재시도 뒤에도 반복/);
  // 알 수 없는 종료 상태도 실패로 본다
  assert.equal(judge([summary(HEAD, 'cancelled', { at: 0 })], { now: 1 }).retry, true);
});

test(`리뷰가 ${STUCK_MS / 60_000}분 넘게 진행 중이거나 요청 뒤 시작하지 않으면 정체로 보고 재요청한다 (PR #66)`, () => {
  assert.equal(judge([summary(HEAD, 'in_progress', { at: 0 })], { now: 5 }).verdict, 'RUNNING');
  const stuck = judge([summary(HEAD, 'in_progress', { at: 0 })], { now: 21 });
  assert.equal(stuck.verdict, 'STALE');
  assert.equal(stuck.retry, true);
  assert.match(stuck.msg, /21분째 in_progress \(정체\)/);
  // 재요청 뒤 15분 안이면 기다리고, 넘으면 사람에게
  assert.equal(judge([summary(HEAD, 'in_progress', { at: 0 }), req(HEAD, 21, { retry: true })], { now: 25 }).verdict, 'RUNNING');
  assert.equal(judge([summary(HEAD, 'in_progress', { at: 0 }), req(HEAD, 21, { retry: true })], { now: 40 }).verdict, 'ERROR');
  // 요청했는데 Codex가 시작도 안 함
  const notStarted = judge([summary(PREV, 'completed', { findings: true }), req(HEAD, 0)], { now: 16 });
  assert.equal(notStarted.verdict, 'STALE');
  assert.equal(notStarted.retry, true);
  assert.equal(judge([summary(PREV, 'completed', { findings: true }), req(HEAD, 0), req(HEAD, 16, { retry: true })], { now: 33 }).verdict, 'ERROR');
});

test('리뷰 결과를 기다리는 커밋 뒤에 문서만 push하면 재요청하지 않고 그 리뷰를 기다린다 (#39)', () => {
  // OLD는 지적 없이 끝났고 PREV(코드 변경)를 요청한 뒤 HEAD(NOW 파일만)를 push
  const base = [summary(OLD, 'completed', { at: -30 }), req(PREV, -5)];
  const r = judge(base, { now: 0, docsOnly: [PREV] });
  assert.equal(r.verdict, 'RUNNING');
  assert.match(r.msg, /^head 1111111 문서만 변경, 2222222 리뷰 대기/);
  // PREV 이후 코드가 바뀌었으면 원래대로 재요청
  assert.equal(judge(base, { now: 0 }).verdict, 'STALE');
  // PREV 리뷰가 진행 중이어도 같다. 정체하면 head로 재요청 (재시도 아님)
  assert.equal(judge([summary(PREV, 'in_progress', { at: -2 })], { now: 0, docsOnly: [PREV] }).verdict, 'RUNNING');
  const stuck = judge([summary(PREV, 'in_progress', { at: -20 })], { now: 0, docsOnly: [PREV] });
  assert.equal(stuck.verdict, 'STALE');
  assert.ok(!stuck.retry);
  // PREV가 지적 없이 끝나면 기존 문서만 변경 규칙으로 COMPLETE
  assert.equal(judge([summary(PREV, 'completed', { at: 0 })], { now: 1, docsOnly: [PREV] }).verdict, 'COMPLETE');
});

test('지적이 남은 리뷰 커밋 뒤에 문서만 push하면 STALE이 아니라 그 커밋의 FINDINGS다 (PR #78)', () => {
  const findings = [summary(PREV, 'completed', { findings: true })];
  const r = judge(findings, { docsOnly: [PREV] });
  assert.equal(r.verdict, 'FINDINGS');
  assert.equal(r.findingsSha, PREV);
  assert.match(r.msg, /^head 1111111 문서만 변경, 2222222 지적 있음/);
  // 그 뒤 코드가 바뀌었으면 원래대로 재요청
  assert.equal(judge(findings).verdict, 'STALE');
  // head에 재리뷰를 요청했으면 그 리뷰를 기다린다
  assert.equal(judge([...findings, req(HEAD, 0)], { now: 2, docsOnly: [PREV] }).verdict, 'RUNNING');
  // PREV 지적을 이관(--resolve 마커는 지적 커밋 sha)했으면 문서만 변경으로 COMPLETE
  const resolved = human('whans1996', iso(1), `이관\n\n<!-- codex-findings-resolved: ${PREV} -->`);
  assert.equal(judge([...findings, resolved], { now: 2, docsOnly: [PREV] }).verdict, 'COMPLETE');
});

test('실패·정체 재시도 댓글은 재리뷰 횟수에 넣지 않는다 (reviewer: 자동 리뷰 실패 뒤 재시도가 1회로 셈)', () => {
  // PR 생성 때 돈 자동 리뷰(요청 마커 없음)가 실패 → 재시도
  assert.equal(requestCount([summary(HEAD, 'failed', { at: 0 }), req(HEAD, 1, { retry: true })]), 0);
  assert.equal(requestCount([req(PREV, -30), req(HEAD, 0), req(HEAD, 20, { retry: true })]), 2);
  // 판정은 재시도 댓글을 요청으로 계속 본다 (시작 대기)
  assert.equal(judge([summary(HEAD, 'failed', { at: 0 }), req(HEAD, 1, { retry: true })], { now: 3 }).verdict, 'RUNNING');
});

test('같은 초 안의 요청·상태 시각은 문자열이 아니라 시각으로 비교한다 (reviewer)', () => {
  // 재시도 요청(…:00Z) 뒤 0.6초에 다시 실패(…:00.600Z): 문자열 비교면 'Z' > '.'라 요청이 더 늦은 것으로 본다
  const r = judge([req(HEAD, 0, { retry: true }), summary(HEAD, 'failed', { at: 0.01 })], { now: 1 });
  assert.equal(r.verdict, 'ERROR');
});

test('#39: 이전 커밋 리뷰가 실패로 남았어도 더 최근 요청 커밋 뒤 문서만 바뀌었으면 그 리뷰를 기다린다 (reviewer)', () => {
  const r = judge([summary(OLD, 'failed', { at: -30 }), req(PREV, -5)], { now: 0, docsOnly: [PREV] });
  assert.equal(r.verdict, 'RUNNING');
  assert.match(r.msg, /^head 1111111 문서만 변경, 2222222 리뷰 대기/);
  // 진행 중인 리뷰가 더 최근이면 그 리뷰를 본다
  const running = judge([req(OLD, -30), summary(PREV, 'in_progress', { at: -2 })], { now: 0, docsOnly: [OLD, PREV] });
  assert.equal(running.verdict, 'RUNNING');
  assert.match(running.msg, /2222222 리뷰 in_progress/);
});

test('Stop hook: 실패·정체 STALE은 기다리는 프로세스가 있어도 막는다 (대기 프로세스가 재시도할 수 있는지 hook이 모른다, Codex 리뷰 2회)', () => {
  // 같은 head 재시도: /pickup의 --wait(--request 없음)는 재시도하지 않고 끝난다
  const headFailed = judge([summary(HEAD, 'failed', { at: 0 })], { now: 1 });
  assert.ok(headFailed.retry);
  const headLine = `${headFailed.verdict} ${headFailed.msg} (재리뷰 0회)`;
  assert.ok(isRetryLine(headLine));
  assert.equal(loopAction({ line: headLine, pr: 7, waiting: true }).action, 'block');
  // 이전 커밋 리뷰 실패(→ head 재요청): 어떤 대기 프로세스도 재요청하지 않는다
  const prevFailed = judge([summary(PREV, 'failed', { at: -2 })], { now: 0, docsOnly: [PREV] });
  assert.ok(!prevFailed.retry);
  const prevLine = `${prevFailed.verdict} ${prevFailed.msg} (재리뷰 1회)`;
  assert.ok(!isRetryLine(prevLine), prevLine);
  assert.equal(loopAction({ line: prevLine, pr: 7, waiting: true }).action, 'block');
});

test('Stop hook: 실패·정체 재요청은 같은 head의 일반 STALE과 따로 한 번 반응한다', () => {
  const plain = loopAction({ line: 'STALE head abc1234, Codex def5678 기준 (재리뷰 1회)', pr: 7 });
  const stuck = loopAction({ line: 'STALE head abc1234 Codex 리뷰 실패(failed) → 재요청 (재리뷰 1회)', pr: 7, handled: [plain.key] });
  assert.equal(stuck.action, 'block');
  assert.equal(stuck.kind, 'request');
  assert.notEqual(stuck.key, plain.key);
  assert.equal(loopAction({ line: 'ERROR head abc1234 Codex 리뷰 실패가 재시도 뒤에도 반복 (재리뷰 1회)', pr: 7 }).kind, 'error');
});

test('push 직후 PR head가 방금 push한 커밋의 조상이면 반영을 기다린다 (#235)', () => {
  // a ← b ← c (c가 최신)
  const order = ['a', 'b', 'c'];
  const isAncestor = (x, y) => order.indexOf(x) <= order.indexOf(y);
  assert.equal(headLagging('b', 'c', isAncestor), true); // GitHub가 아직 b
  assert.equal(headLagging('c', 'c', isAncestor), false); // 반영됨
  assert.equal(headLagging('c', 'b', isAncestor), false); // 다른 PC가 더 새 커밋을 push함
  assert.equal(headLagging('b', null, isAncestor), false); // 원격 추적 브랜치 없음
  assert.equal(headLagging('x', 'c', () => false), false); // force push 등 갈라짐
});

const cleanReceipt = (sha, min) =>
  bot(iso(min), `<!-- codex-review-receipt:v1 -->\n\nCodex Review: Didn't find any major issues. Keep it up!\n\n**Reviewed commit:** \`${sha.slice(0, 10)}\``);

test('HP-4 AC10: PR 전체가 문서만이면 리뷰가 없거나 실패해도 COMPLETE (문서만 PR, Codex 생략) (#165)', () => {
  const prDocs = (comments, opts = {}) => judgeReview({ pr: 7, h: { sha: HEAD, state: 'OPEN' }, comments, prDocsOnly: () => true, now: T0, ...opts });
  for (const comments of [[], [summary(HEAD, 'failed')], [summary(PREV, 'failed')], [summary(PREV, 'completed', { findings: true })]]) {
    const r = prDocs(comments);
    assert.equal(r.verdict, 'COMPLETE', JSON.stringify(comments.map((c) => c.body.slice(-60))));
    assert.equal(r.msg, 'head 1111111 문서만 PR, Codex 생략 (conventions 7절)');
  }
  // head 리뷰가 끝났으면 그 판정을 그대로 보인다: 지적 없음, 또는 지적(사람이 읽고 판단, conventions 7절)
  assert.match(prDocs([summary(HEAD, 'completed')]).msg, /지적 없음/);
  assert.equal(prDocs([summary(HEAD, 'completed', { findings: true })]).verdict, 'FINDINGS');
  // 문서 밖 변경이 있는 PR은 기존 판정
  assert.equal(judge([summary(HEAD, 'failed')]).verdict, 'STALE');
  assert.equal(judge([]).verdict, 'STALE');
});

test('HP-4 AC10: codex-review.mjs는 develop merge-base 대비 PR 전체로 문서만 PR을 판정한다', () => {
  const src = readFileSync(new URL('../codex-review.mjs', import.meta.url), 'utf8');
  assert.match(src, /prDocsOnly/);
});

test('HP-4 AC11: 상태 댓글이 지적 있던 A에 머물러도, 지적보다 늦은 영수증 B 이후 문서만 바뀌었으면 COMPLETE (#198)', () => {
  const B = '3'.repeat(40);
  // A(PREV) 리뷰: 지적 2건(-30, -20분), 상태 댓글 갱신 0분 → B 리뷰: 영수증 10분 (영수증의 sha는 10자)
  const comments = [summary(PREV, 'completed', { at: 0, findings: true }), cleanReceipt(B, 10)];
  const r = judge(comments, { findings: [iso(-30), iso(-20)], docsOnly: [B.slice(0, 10)] });
  assert.equal(r.verdict, 'COMPLETE');
  assert.equal(r.msg, `head 1111111 문서만 변경 (Codex ✅ ${B.slice(0, 7)} 이후)`);
  // 영수증보다 늦은 미해결 인라인 지적이 있으면 COMPLETE가 아니다
  assert.notEqual(judge(comments, { findings: [iso(-30), iso(12)], docsOnly: [B.slice(0, 10)] }).verdict, 'COMPLETE');
  // B 이후 코드도 바뀌었으면 COMPLETE가 아니다
  assert.notEqual(judge(comments, { findings: [iso(-30)] }).verdict, 'COMPLETE');
  // 상태 댓글보다 이른 영수증은 기준이 아니다 (그 뒤의 A 리뷰가 지적을 남겼다)
  assert.notEqual(judge([cleanReceipt(B, -40), summary(PREV, 'completed', { at: 0, findings: true })], { findings: [iso(-30)], docsOnly: [B.slice(0, 10)] }).verdict, 'COMPLETE');
});

test('HP-4 AC12: 로컬 HEAD가 그대로여도 push로 원격 브랜치가 바뀌었으면 3분 안이라도 다시 판정한다 (#178)', () => {
  const head = 'abc1234' + '0'.repeat(33);
  const now = 10 * 60_000;
  const MS = 3 * 60_000;
  const prev = { head, prHead: 'def5678', verdict: 'STALE', at: now - 1000, remote: 'r1' };
  assert.ok(skipRecheck(prev, head, now, MS, 'r1'));
  assert.ok(!skipRecheck(prev, head, now, MS, 'r2'));
  // COMPLETE 확정도 원격이 바뀌면 다시 본다
  assert.ok(!skipRecheck({ ...prev, prHead: 'abc1234', verdict: 'COMPLETE', at: 0 }, head, now, MS, 'r2'));
  // 원격을 모르면(브랜치를 아직 push하지 않음) 기존 규칙
  assert.ok(skipRecheck({ ...prev, remote: null }, head, now, MS, null));
});

test('HP-4 AC12: codex-loop는 판정 기록에 원격 브랜치 sha를 남기고 skipRecheck에 넘긴다', () => {
  const src = readFileSync(new URL('../../.claude/hooks/codex-loop.mjs', import.meta.url), 'utf8');
  assert.match(src, /skipRecheck\(prev, head, Date\.now\(\), RECHECK_MS, remote\)/);
  assert.match(src, /remote,/);
});
