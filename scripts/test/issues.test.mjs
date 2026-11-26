// follow-up 이슈 분류·배너·PR 본문의 닫는 키워드 (gh 호출 없는 부분)
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import { closingRefs, followUpIssues, issueBannerLine, issueContext, issueFailureLine, summarizeIssues } from '../lib/issues.mjs';

// execFile 대역: (file, args, opts, cb) → cb(err, stdout)
const fakeExec = (err, stdout = '') => (file, args, opts, cb) => cb(err, stdout, '');

test('이슈 조회는 성공·실패를 null이 아닌 {ok, issues, reason}으로 알린다 (#67)', async () => {
  assert.deepEqual(await followUpIssues({ exec: fakeExec(null, '[{"number":1}]') }), { ok: true, issues: [{ number: 1 }] });
  assert.deepEqual(await followUpIssues({ exec: fakeExec(Object.assign(new Error('x'), { code: 'ENOENT' })) }), { ok: false, reason: 'gh 없음' });
  assert.deepEqual(await followUpIssues({ exec: fakeExec(Object.assign(new Error('x'), { killed: true }), ''), timeout: 4000 }), {
    ok: false,
    reason: '4초 안에 응답 없음',
  });
  const failed = await followUpIssues({ exec: (f, a, o, cb) => cb(Object.assign(new Error('x'), { code: 1 }), '', 'HTTP 401: Bad credentials\nmore') });
  assert.deepEqual(failed, { ok: false, reason: 'gh 실패: HTTP 401: Bad credentials' });
  // gh가 응답 본문까지 한 줄에 붙여 내는 경우 배너가 넘치지 않게 자른다
  const long = await followUpIssues({ exec: (f, a, o, cb) => cb(Object.assign(new Error('x'), { code: 1 }), '', `401 body: "${'x'.repeat(200)}"`) });
  assert.equal(long.reason.length, 80);
  assert.ok(long.reason.endsWith('…'));
  // 이벤트 루프가 막혀 제한 시간 타이머가 먼저 터지면 종료 코드 0에 빈 출력이 온다 (11-20 재현)
  assert.deepEqual(await followUpIssues({ exec: fakeExec(null, '') }), { ok: false, reason: '빈 응답' });
  assert.deepEqual(await followUpIssues({ exec: fakeExec(null, '{"a":1}') }), { ok: false, reason: '응답 형식 오류' });
  assert.deepEqual(await followUpIssues({ exec: fakeExec(null, 'not json') }), { ok: false, reason: '응답 형식 오류' });
});

test('이슈 조회 실패 줄은 이유와 직접 확인할 명령을 담는다', () => {
  assert.equal(issueFailureLine('빈 응답'), '⚠️ 이슈 조회 실패 (빈 응답) → gh issue list --label follow-up');
});

test('session-start는 동기 git(fetch·autoSync)이 끝난 뒤 gh·docker 조회를 시작한다 (#67)', () => {
  // 동기 git이 이벤트 루프를 막는 동안 제한 시간 타이머가 터져 결과가 사라진다
  const src = readFileSync(new URL('../../.claude/hooks/session-start.mjs', import.meta.url), 'utf8');
  const syncAt = src.indexOf('autoSync({');
  assert.ok(syncAt > 0);
  assert.ok(src.indexOf('followUpIssues(') > syncAt, 'followUpIssues는 autoSync 뒤에 시작');
  assert.ok(src.indexOf('runChecks(') > syncAt, 'runChecks는 autoSync 뒤에 시작');
  // 받기 전에 다른 동기 git(상대 NOW·작업 파일·peer 상태)을 하면 같은 경합이 생긴다 (reviewer)
  // 로드맵 PR 조회(roadmapPrsP)도 같은 자리에서 함께 받는다
  // 시간 예산(H-5) 안에서 기다린다: budget.within(checksP|issuesP|roadmapPrsP, …)
  const awaitAt = src.indexOf('await Promise.all([');
  assert.ok(awaitAt > 0, '환경 점검·이슈·로드맵 조회를 함께 받는다');
  for (const p of ['checksP', 'issuesP', 'roadmapPrsP']) assert.ok(src.indexOf(`budget.within(${p},`, awaitAt) > awaitAt, `${p}를 함께 받는다`);
  for (const sync of ['latestPeerNow(', 'myBranchNows(', 'workFilesNewerThanNow(', 'writePeerState(']) {
    assert.ok(src.indexOf(sync) > awaitAt, `${sync}은 gh·docker 결과를 받은 뒤에 한다`);
  }
});

test('session-start는 gh를 기다린 뒤의 로컬 git에도 남은 예산에서 제한 시간을 준다 (H-5, reviewer)', () => {
  // 고정 상한(호출당 1.5초·3초)을 합치면 gh를 다 기다린 뒤 7~8초가 더 걸려 hook 제한(15초)을 넘을 수 있다
  const src = readFileSync(new URL('../../.claude/hooks/session-start.mjs', import.meta.url), 'utf8');
  assert.match(src, /latestPeerNow\(other, \{ timeoutMs: /);
  assert.match(src, /myBranchNows\(me, \{ budgetMs: myBranchMs \}\)/);
  assert.match(src, /workFilesNewerThanNow\(ROOT, nowFileOf\(me\), \{ timeout: /);
  assert.match(src, /unmergedRemoteBranches\(\{ timeout: budget\.timeout\(/);
});

const NOW = Date.parse('2026-11-19T12:00:00Z');
const issue = (number, labels, { days = 1, assignees = [], title = `이슈 ${number}` } = {}) => ({
  number,
  title,
  labels: labels.map((name) => ({ name })),
  assignees: assignees.map((login) => ({ login })),
  createdAt: new Date(NOW - days * 24 * 60 * 60 * 1000).toISOString(),
});

const ISSUES = [
  issue(26, ['follow-up', 'area:A'], { days: 2 }),
  issue(16, ['follow-up', 'area:A'], { days: 8 }),
  issue(17, ['follow-up', 'area:common'], { days: 9, assignees: ['whans1996'] }),
  issue(40, ['follow-up', 'area:B']),
  issue(41, ['follow-up', 'area:B'], { assignees: ['whans1996'] }),
  issue(39, ['follow-up']),
];

test('area:common은 담당자가 나여도 공통, 내 area 라벨이나 담당자는 내 것, area 라벨 없음은 분류 필요, 상대 영역은 뺀다', () => {
  const s = summarizeIssues(ISSUES, { key: 'dh', github: 'whans1996' }, NOW);
  assert.deepEqual(s.mine.map((i) => i.number), [16, 26, 41]);
  assert.deepEqual(s.common.map((i) => i.number), [17]);
  assert.deepEqual(s.untriaged.map((i) => i.number), [39]);
  // 7일 경계: 8·9일은 넘음, 2일은 아님. 분류 필요는 세지 않는다
  assert.deepEqual(s.stale.map((i) => i.number), [16, 17]);

  const js = summarizeIssues(ISSUES, { key: 'js', github: 'JoonseobKim' }, NOW);
  assert.deepEqual(js.mine.map((i) => i.number), [40, 41]);
});

test('배너는 개수와 첫 이슈를 한 줄로, 볼 것이 없으면 null', () => {
  const s = summarizeIssues(ISSUES, { key: 'dh', github: 'whans1996' }, NOW);
  assert.equal(issueBannerLine(s), '📌 이슈: 내 3 · 공통 1 · 분류 필요 1 · 7일 넘음 2 (#16 이슈 16 외)');

  const one = summarizeIssues([issue(5, ['area:A'], { title: '가'.repeat(50) })], { key: 'dh' }, NOW);
  const line = issueBannerLine(one);
  assert.match(line, /^📌 이슈: 내 1 · 공통 0 \(#5 가{39}…\)$/);

  assert.equal(issueBannerLine(summarizeIssues([], { key: 'dh' }, NOW)), null);
  assert.equal(issueBannerLine(summarizeIssues(null, { key: 'dh' }, NOW)), null);
});

test('모델용 목록은 빈 그룹을 빼고 그룹마다 개수를 자른다', () => {
  const s = summarizeIssues(ISSUES, { key: 'dh', github: 'whans1996' }, NOW);
  const ctx = issueContext(s, 2);
  assert.match(ctx, /^- 내 영역 3개\n {2}- #16 이슈 16\n {2}- #26 이슈 26\n {2}- … 외 1개/);
  assert.match(ctx, /- 공통 1개\n {2}- #17/);
  assert.match(ctx, /- 분류 필요/);
  assert.equal(issueContext(summarizeIssues([], { key: 'dh' }, NOW)), null);
});

test('blocker 라벨은 영역과 무관하게 차단 그룹으로 먼저 모으고 다른 그룹에서 뺀다 (#67)', () => {
  const issues = [...ISSUES, issue(63, ['follow-up', 'area:B', 'blocker'], { days: 8, title: 'B 영역이 내 커밋을 막음' }), issue(70, ['follow-up', 'area:A', 'blocker'])];
  const s = summarizeIssues(issues, { key: 'dh', github: 'whans1996' }, NOW);
  // 상대 영역(area:B)이어도 차단이면 보인다
  assert.deepEqual(s.blockers.map((i) => i.number), [63, 70]);
  assert.deepEqual(s.mine.map((i) => i.number), [16, 26, 41]);
  assert.deepEqual(s.stale.map((i) => i.number), [16, 17, 63]);
  assert.equal(issueBannerLine(s), '🔴 이슈: 차단 2 · 내 3 · 공통 1 · 분류 필요 1 · 7일 넘음 3 (#63 B 영역이 내 커밋을 막음 외)');
  assert.match(issueContext(s), /^- 🔴 차단 \(blocker: [^\n]*\) 2개\n {2}- #63 B 영역이 내 커밋을 막음\n {2}- #70/);
  // 차단 이슈만 있어도 배너를 낸다
  assert.match(issueBannerLine(summarizeIssues([issue(63, ['area:B', 'blocker'])], { key: 'dh' }, NOW)), /^🔴 이슈: 차단 1 · 내 0 · 공통 0 \(#63/);
});

test('PR 본문의 닫는 키워드에서 이슈 번호를 뽑는다', () => {
  assert.deepEqual(closingRefs('## 해결 이슈\nCloses #16\nfixes #23, Resolved: #24'), [16, 23, 24]);
  assert.deepEqual(closingRefs('closes #16\nCLOSES #16'), [16]);
  // 키워드 없는 언급, 다른 단어 안의 키워드, 번호 뒤에 글자가 붙은 것은 무시
  assert.deepEqual(closingRefs('관련 #17, #29 로 이관, enclosed #5, prefixes #6, closes #12a, closes #'), []);
  // 줄 끝 키워드와 다음 줄 번호는 엮지 않는다
  assert.deepEqual(closingRefs('이번 PR은 버그를 fix\n#5 는 다음에'), []);
  assert.deepEqual(closingRefs(null), []);
});
