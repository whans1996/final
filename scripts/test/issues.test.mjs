// follow-up 이슈 분류·배너·PR 본문의 닫는 키워드 (gh 호출 없는 부분)
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { closingRefs, issueBannerLine, issueContext, summarizeIssues } from '../lib/issues.mjs';

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
  issue(17, ['follow-up', 'area:common'], { days: 9 }),
  issue(40, ['follow-up', 'area:B']),
  issue(41, ['follow-up', 'area:B'], { assignees: ['whans1996'] }),
  issue(39, ['follow-up']),
];

test('내 area 라벨이나 담당자는 내 것, area:common은 공통, area 라벨 없음은 분류 필요, 상대 영역은 뺀다', () => {
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

test('PR 본문의 닫는 키워드에서 이슈 번호를 뽑는다', () => {
  assert.deepEqual(closingRefs('## 해결 이슈\nCloses #16\nfixes #23, Resolved: #24'), [16, 23, 24]);
  assert.deepEqual(closingRefs('closes #16\nCLOSES #16'), [16]);
  // 키워드 없는 언급, 다른 단어 안의 키워드, 번호 뒤에 글자가 붙은 것은 무시
  assert.deepEqual(closingRefs('관련 #17, #29 로 이관, enclosed #5, prefixes #6, closes #12a, closes #'), []);
  assert.deepEqual(closingRefs(null), []);
});
