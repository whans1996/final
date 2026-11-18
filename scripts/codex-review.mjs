#!/usr/bin/env node
// PR의 Codex 리뷰 상태를 판정하고, 필요하면 재리뷰를 요청해 끝날 때까지 기다린다.
// Codex는 PR마다 요약 댓글 하나를 갱신하며 그 안에 기계용 상태를 넣는다:
//   <!-- codex-review-state:v1 {"code_review":{"commit_sha":"…","has_any_findings":false,"status":"completed"}, …} -->
// 이 상태가 PR head 커밋 기준인지로 판정한다.
// 보조 신호: 수동 요청 뒤 다는 영수증 댓글 "Didn't find any major issues … Reviewed commit: `abc1234`"가
// head 커밋이면 상태 댓글이 늦게 갱신되어도 COMPLETE로 본다. 자동 리뷰 뒤의 👍 반응은 커밋 정보가 없어 쓰지 않는다.
// 문서만 바뀐 커밋: Codex가 마지막으로 지적 없이 끝낸 커밋 이후 docs/ 아래(openapi.json 제외)만 바뀌었으면
// 재리뷰를 요청하지 않고 COMPLETE로 본다 (11-18 사용자 지시). 마지막 리뷰에 지적이 있으면 적용하지 않는다.
//
// 사용법: node scripts/codex-review.mjs <PR번호> [--request] [--wait] [--timeout 20]
//   --request  상태가 이전 커밋 기준(STALE)이면 "@codex review" 댓글을 단다.
//              댓글에 head sha 마커를 넣어, 같은 head에 대한 요청이 이미 있으면 생략한다
//   --wait     COMPLETE·FINDINGS가 될 때까지 30초 간격으로 확인한다 (기본 20분). Claude는 run_in_background로 실행한다
//
// 출력 첫 줄과 종료 코드:
//   COMPLETE 0  head 커밋 리뷰 완료, 지적 없음
//   FINDINGS 2  head 커밋 리뷰 완료, 지적 있음 (지적 요약을 함께 출력)
//   RUNNING  3  head 커밋 리뷰 중
//   STALE    4  리뷰 상태가 없거나 이전 커밋 기준
//   TIMEOUT  5  --wait 시간 초과
//   ERROR    1  gh 실패, 또는 Codex가 마지막 요청 뒤 "create an environment" 안내만 남김
//               (환경을 만든 뒤 --request로 다시 실행하면 요청을 새로 단다)

import { spawnSync } from 'node:child_process';
import { git } from './lib/common.mjs';

const CODE = { COMPLETE: 0, ERROR: 1, FINDINGS: 2, RUNNING: 3, STALE: 4, TIMEOUT: 5 };
const BOT = 'chatgpt-codex-connector';
const STATE_RE = /<!--\s*codex-review-state:v1\s*(\{.*?\})\s*-->/s;
const REQUEST_RE = /<!--\s*codex-review-request:\s*([0-9a-f]{40})\s*-->/;
const ENV_RE = /create an environment for this repo\]\((https?:[^)\s]+)\)/;
const CLEAN_RECEIPT_RE = /codex-review-receipt[\s\S]*Didn't find any major issues[\s\S]*?Reviewed commit:\*{0,2}\s*`([0-9a-f]{7,40})`/;

function fail(msg) {
  console.log(`ERROR ${msg}`);
  process.exit(CODE.ERROR);
}

/** soft면 실패해도 종료하지 않고 null을 돌려준다 */
function gh(args, { soft = false } = {}) {
  // 모든 페이지를 한 번에 받으므로 기본 1MiB 버퍼로는 ENOBUFS가 날 수 있다 (Codex 리뷰)
  const r = spawnSync('gh', args, { encoding: 'utf8', windowsHide: true, maxBuffer: 64 * 1024 * 1024 });
  if (soft && (r.error || r.status !== 0)) return null;
  if (r.error) fail(`gh 실행 실패: ${r.error.message}`);
  if (r.status !== 0) fail(`gh ${args.slice(0, 2).join(' ')}: ${(r.stderr || '').trim().split('\n')[0]}`);
  return r.stdout;
}
const ghJson = (args, opts) => JSON.parse(gh(args, opts) || 'null');

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

/** PR head 커밋 */
function head() {
  const v = ghJson(['pr', 'view', pr, '--json', 'headRefOid,state']);
  return { sha: v.headRefOid, state: v.state };
}

/** 모든 페이지를 읽어 하나의 배열로 (댓글이 100개를 넘어도 영수증·요청 마커를 놓치지 않게, Codex 리뷰) */
const ghList = (path) => (ghJson(['api', '--paginate', '--slurp', `${path}?per_page=100`]) || []).flat();
const issueComments = () => ghList(`repos/${repo}/issues/${pr}/comments`);

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
  const list = ghList(`repos/${repo}/pulls/${pr}/comments`);
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

/** "지적 없음" 영수증 댓글 목록 { sha(7~40자), at } (댓글 순서) */
function cleanReceipts(comments) {
  return comments
    .filter((c) => c.user?.login?.startsWith(BOT))
    .map((c) => ({ sha: CLEAN_RECEIPT_RE.exec(c.body || '')?.[1], at: c.created_at }))
    .filter((r) => r.sha);
}

/** Codex 인라인 지적 중 가장 늦은 시각 (없으면 '') */
function lastFindingAt() {
  return ghList(`repos/${repo}/pulls/${pr}/comments`)
    .filter((c) => c.user?.login?.startsWith(BOT) && !c.in_reply_to_id)
    .map((c) => c.created_at)
    .sort()
    .at(-1) ?? '';
}

/** 재리뷰 없이 넘어가도 되는 문서 경로. openapi.json은 API 계약이라 제외 */
const isDocPath = (p) => !!p && p.startsWith('docs/') && p !== 'docs/api/openapi.json';

/** base 이후 head까지 문서만 바뀌었는가. base가 head의 조상이 아니거나(force push) 조회 실패면 false */
function docsOnlySince(base, headSha) {
  const cmp = ghJson(['api', `repos/${repo}/compare/${base}...${headSha}`], { soft: true });
  const files = cmp?.files;
  // compare API는 파일을 300개까지만 준다. 넘으면 확신할 수 없다
  if (cmp?.status !== 'ahead' || !Array.isArray(files) || !files.length || files.length >= 300) return false;
  return files.every((f) => isDocPath(f.filename) && (!f.previous_filename || isDocPath(f.previous_filename)));
}

function judge() {
  const h = head();
  const comments = issueComments();
  const st = codexState(comments);
  const short = h.sha.slice(0, 7);
  const receipts = cleanReceipts(comments);
  const clean = receipts.some((r) => h.sha.startsWith(r.sha));
  const stClean = st?.status === 'completed' && !st.has_any_findings;
  if (st?.commit_sha === h.sha && stClean) {
    return { verdict: 'COMPLETE', h, comments, msg: `head ${short} 지적 없음 (상태 댓글)` };
  }
  if (clean && !(st?.commit_sha === h.sha && st.status === 'completed')) {
    return { verdict: 'COMPLETE', h, comments, msg: `head ${short} 지적 없음 (영수증 댓글)` };
  }
  if (!st || st.commit_sha !== h.sha) {
    // 마지막 리뷰가 지적 없이 끝났을 때만(상태 댓글이 없으면 영수증으로) 기준 커밋을 잡는다.
    // 지적이 있거나 리뷰 중이면 영수증으로 건너뛰어 그 결과를 덮지 않는다.
    // 영수증 뒤에 인라인 지적이 달렸으면 그 영수증은 마지막 리뷰가 아니므로 쓰지 않는다 (Codex 리뷰)
    if (!st || stClean) {
      const findingAt = receipts.length ? lastFindingAt() : '';
      const fresh = receipts.filter((r) => r.at > findingAt).map((r) => r.sha);
      const bases = [...new Set([...fresh.reverse(), ...(stClean ? [st.commit_sha] : [])])];
      const base = bases.find((b) => docsOnlySince(b, h.sha));
      if (base) {
        return { verdict: 'COMPLETE', h, comments, msg: `head ${short} 문서만 변경 (Codex ✅ ${base.slice(0, 7)} 이후)` };
      }
    }
    // 환경 안내가 이 head에 대한 마지막 요청보다 늦으면 기다려도 리뷰가 오지 않는다.
    // --request로 시작하면 환경을 만든 뒤의 재시도로 보고 요청을 새로 단다 (아래, Codex 리뷰)
    const env = envNotice(comments);
    const reqAt = requestAt(comments, h.sha);
    if (env && (!reqAt || env.at > reqAt)) {
      const msg = `Codex 환경 없음 (${env.at}): ${env.url} 에서 만든 뒤 --request로 다시 요청`;
      return { verdict: 'ERROR', envMissing: true, h, comments, msg };
    }
    const seen = st?.commit_sha ? `${st.commit_sha.slice(0, 7)} 기준` : '상태 댓글 없음';
    return { verdict: 'STALE', h, comments, msg: `head ${short}, Codex ${seen}` };
  }
  if (st.status !== 'completed') return { verdict: 'RUNNING', h, comments, msg: `head ${short} 리뷰 ${st.status}` };
  return { verdict: 'FINDINGS', h, comments, msg: `head ${short}` };
}

/** 이 head sha에 대한 재리뷰 요청 중 가장 늦은 시각 (없으면 null).
 *  커밋 시각과 비교하면 "먼저 커밋하고 나중에 push"한 경우 옛 요청을 새 head 것으로 오판하므로 sha 마커로 묶는다 (Codex 리뷰).
 *  봇의 안내문에도 "@codex review"가 들어 있으므로 사람 댓글만 본다. */
function requestAt(comments, sha) {
  const at = comments
    .filter((c) => !c.user?.login?.startsWith(BOT) && REQUEST_RE.exec(c.body || '')?.[1] === sha)
    .map((c) => c.created_at);
  return at.length ? at.sort().at(-1) : null;
}

/** 마지막 봇 활동이 "create an environment" 안내이면 { url, at }. 그 뒤 리뷰 댓글이 갱신됐으면 null */
function envNotice(comments) {
  const bot = comments.filter((c) => c.user?.login?.startsWith(BOT));
  const notices = bot.filter((c) => ENV_RE.test(c.body || ''));
  if (!notices.length) return null;
  const last = notices.at(-1);
  const otherAt = bot
    .filter((c) => !ENV_RE.test(c.body || ''))
    .map((c) => c.updated_at || c.created_at)
    .sort()
    .at(-1);
  if (otherAt && otherAt >= last.created_at) return null;
  return { url: ENV_RE.exec(last.body)[1], at: last.created_at };
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

const postRequest = (sha) => gh(['pr', 'comment', pr, '--body', `@codex review\n\n<!-- codex-review-request: ${sha} -->`]);

if (request && r.envMissing) {
  // 안내가 마지막 요청보다 늦으므로 기존 요청이 있어도 다시 단다. 환경이 아직 없으면 대기 중 같은 안내가 와서 ERROR로 끝난다
  postRequest(r.h.sha);
  console.error(`(Codex 환경 안내 뒤 재시도: @codex review 요청함, head ${r.h.sha.slice(0, 7)})`);
  // 방금 단 댓글이 목록에 바로 안 보일 수 있어 다시 판정하지 않고 대기 상태로 둔다
  r = { ...r, verdict: 'STALE', envMissing: false, msg: `head ${r.h.sha.slice(0, 7)}, 환경 안내 뒤 재요청함` };
} else if (request && r.verdict === 'STALE') {
  if (requestAt(r.comments, r.h.sha)) {
    console.error(`(head ${r.h.sha.slice(0, 7)}에 대한 @codex review 요청이 이미 있어 생략)`);
  } else {
    postRequest(r.h.sha);
    console.error(`(@codex review 요청함: head ${r.h.sha.slice(0, 7)})`);
  }
}

const done = (v) => v === 'COMPLETE' || v === 'FINDINGS' || v === 'ERROR';
if (!wait || done(r.verdict)) report(r);

const deadline = Date.now() + timeoutMin * 60_000;
while (Date.now() < deadline) {
  await new Promise((res) => setTimeout(res, 30_000));
  r = judge();
  if (done(r.verdict)) report(r);
}
console.log(`TIMEOUT ${timeoutMin}분 대기, 마지막 상태 ${r.verdict} (${r.msg})`);
process.exit(CODE.TIMEOUT);
