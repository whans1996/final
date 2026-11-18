#!/usr/bin/env node
// PR의 Codex 리뷰 상태를 판정하고, 필요하면 재리뷰를 요청해 끝날 때까지 기다린다.
// Codex는 PR마다 요약 댓글 하나를 갱신하며 그 안에 기계용 상태를 넣는다:
//   <!-- codex-review-state:v1 {"code_review":{"commit_sha":"…","has_any_findings":false,"status":"completed"}, …} -->
// 이 상태가 PR head 커밋 기준인지로 판정한다 (👍 반응·문구 파싱보다 확실하다).
//
// 사용법: node scripts/codex-review.mjs <PR번호> [--request] [--wait] [--timeout 20]
//   --request  상태가 이전 커밋 기준(STALE)이면 "@codex review" 댓글을 단다 (head 커밋 이후 요청이 이미 있으면 생략)
//   --wait     COMPLETE·FINDINGS가 될 때까지 30초 간격으로 확인한다 (기본 20분). Claude는 run_in_background로 실행한다
//
// 출력 첫 줄과 종료 코드:
//   COMPLETE 0  head 커밋 리뷰 완료, 지적 없음
//   FINDINGS 2  head 커밋 리뷰 완료, 지적 있음 (지적 요약을 함께 출력)
//   RUNNING  3  head 커밋 리뷰 중
//   STALE    4  리뷰 상태가 없거나 이전 커밋 기준
//   TIMEOUT  5  --wait 시간 초과
//   ERROR    1

import { spawnSync } from 'node:child_process';
import { git } from './lib/common.mjs';

const CODE = { COMPLETE: 0, ERROR: 1, FINDINGS: 2, RUNNING: 3, STALE: 4, TIMEOUT: 5 };
const BOT = 'chatgpt-codex-connector';
const STATE_RE = /<!--\s*codex-review-state:v1\s*(\{.*?\})\s*-->/s;

function fail(msg) {
  console.log(`ERROR ${msg}`);
  process.exit(CODE.ERROR);
}

function gh(args) {
  const r = spawnSync('gh', args, { encoding: 'utf8', windowsHide: true });
  if (r.error) fail(`gh 실행 실패: ${r.error.message}`);
  if (r.status !== 0) fail(`gh ${args.slice(0, 2).join(' ')}: ${(r.stderr || '').trim().split('\n')[0]}`);
  return r.stdout;
}
const ghJson = (args) => JSON.parse(gh(args) || 'null');

const argv = process.argv.slice(2);
const pr = argv.find((a) => /^\d+$/.test(a));
if (!pr) fail('PR 번호가 필요합니다. 예: node scripts/codex-review.mjs 6 --request --wait');
const request = argv.includes('--request');
const wait = argv.includes('--wait');
const ti = argv.indexOf('--timeout');
const timeoutMin = ti >= 0 ? Number(argv[ti + 1]) || 20 : 20;

const remote = git(['remote', 'get-url', 'origin']).out;
const repo = remote.match(/github\.com[:/](.+?)(\.git)?$/)?.[1];
if (!repo) fail(`origin이 GitHub 저장소가 아닙니다: ${remote || '(없음)'}`);

/** PR head 커밋과 그 커밋 시각 */
function head() {
  const v = ghJson(['pr', 'view', pr, '--json', 'headRefOid,state,commits']);
  const last = v.commits?.[v.commits.length - 1];
  return { sha: v.headRefOid, state: v.state, at: last?.committedDate ?? null };
}

const issueComments = () => ghJson(['api', `repos/${repo}/issues/${pr}/comments?per_page=100`]) || [];

function codexState(comments) {
  for (const c of comments) {
    if (!c.user?.login?.startsWith(BOT)) continue;
    const m = STATE_RE.exec(c.body || '');
    if (m) {
      try {
        return JSON.parse(m[1]).code_review ?? null;
      } catch {
        return null;
      }
    }
  }
  return null;
}

/** head 커밋에 달린 Codex 인라인 지적 요약 */
function findings(sha) {
  const list = ghJson(['api', `repos/${repo}/pulls/${pr}/comments?per_page=100`]) || [];
  return list
    .filter((c) => c.user?.login?.startsWith(BOT) && !c.in_reply_to_id && c.original_commit_id === sha)
    .map((c) => {
      const body = c.body || '';
      const pri = /!\[(P\d) Badge\]/.exec(body)?.[1] ?? '?';
      const title = (body.split('\n')[0] || '')
        .replace(/<\/?sub>/g, '')
        .replace(/!\[[^\]]*\]\([^)]*\)/g, '')
        .replace(/\*\*/g, '')
        .trim();
      const line = c.line ?? c.original_line ?? '?';
      return `- [${pri}] ${c.path}:${line} ${title}\n  ${c.html_url}`;
    });
}

function judge() {
  const h = head();
  const comments = issueComments();
  const st = codexState(comments);
  const short = h.sha.slice(0, 7);
  if (!st || st.commit_sha !== h.sha) {
    const seen = st?.commit_sha ? `${st.commit_sha.slice(0, 7)} 기준` : '상태 댓글 없음';
    return { verdict: 'STALE', h, comments, msg: `head ${short}, Codex ${seen}` };
  }
  if (st.status !== 'completed') return { verdict: 'RUNNING', h, comments, msg: `head ${short} 리뷰 ${st.status}` };
  if (st.has_any_findings) return { verdict: 'FINDINGS', h, comments, msg: `head ${short}` };
  return { verdict: 'COMPLETE', h, comments, msg: `head ${short} 지적 없음` };
}

/** head 커밋 이후 이미 "@codex review"를 요청했는가 */
function alreadyRequested(comments, h) {
  const since = h.at ? Date.parse(h.at) : 0;
  // 봇의 요약·영수증 댓글 안내문에도 "@codex review"가 들어 있으므로 사람 댓글이 이 문구로 시작할 때만 센다
  return comments.some(
    (c) =>
      !c.user?.login?.startsWith(BOT) &&
      /^\s*@codex\s+review\b/i.test(c.body || '') &&
      Date.parse(c.created_at) >= since,
  );
}

function report(r) {
  console.log(`${r.verdict} ${r.msg}`);
  if (r.verdict === 'FINDINGS') {
    const f = findings(r.h.sha);
    console.log(f.length ? f.join('\n') : `(인라인 지적을 찾지 못함: gh api repos/${repo}/pulls/${pr}/reviews 확인)`);
  }
  process.exit(CODE[r.verdict]);
}

let r = judge();
if (r.h.state !== 'OPEN' && (request || wait)) fail(`PR #${pr}이 ${r.h.state} 상태입니다`);

if (request && r.verdict === 'STALE') {
  if (alreadyRequested(r.comments, r.h)) {
    console.error(`(head ${r.h.sha.slice(0, 7)} 이후 @codex review 요청이 이미 있어 생략)`);
  } else {
    gh(['pr', 'comment', pr, '--body', '@codex review']);
    console.error(`(@codex review 요청함: head ${r.h.sha.slice(0, 7)})`);
  }
}

if (!wait || r.verdict === 'COMPLETE' || r.verdict === 'FINDINGS') report(r);

const deadline = Date.now() + timeoutMin * 60_000;
while (Date.now() < deadline) {
  await new Promise((res) => setTimeout(res, 30_000));
  r = judge();
  if (r.verdict === 'COMPLETE' || r.verdict === 'FINDINGS') report(r);
}
console.log(`TIMEOUT ${timeoutMin}분 대기, 마지막 상태 ${r.verdict} (${r.msg})`);
process.exit(CODE.TIMEOUT);
